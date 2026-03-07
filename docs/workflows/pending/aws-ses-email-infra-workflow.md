# AWS SES Email Infrastructure Workflow — AkademiaPlus

**Target**: Claude Code CLI
**Repo**: `/Volumes/ElatusDev/ElatusDev/platform/core-api`
**Module**: `infra/aws/ses/` (CloudFormation), `notification-system` (Java — Phase 5 only)
**Prerequisite**: Read `docs/directives/CLAUDE.md` and `docs/directives/AI-CODE-REF.md` before starting. Also read the existing email delivery code (`EmailDeliveryChannelStrategy.java`, `ImmediateSendUseCase.java`, `EmailConfiguration.java`).

---

## 1. Architecture Overview

### 1.1 Transport: AWS SES via SMTP

The email delivery system uses Jakarta Mail (`JavaMailSender`) configured to send through the AWS SES SMTP endpoint (`email-smtp.us-east-1.amazonaws.com:587`). The Java application code is already implemented — this workflow provisions the AWS-side infrastructure: domain identity, DNS records, SMTP credentials, event tracking pipeline, and production alarms.

### 1.2 Infrastructure Topology

```
                    akademiaplus.com
                         |
           DNS Records (manual at registrar)
           +-- 3x CNAME (DKIM)
           +-- 1x MX + 1x TXT (Custom MAIL FROM: mail.akademiaplus.com)
           +-- 1x TXT (_dmarc -- DMARC policy)
                         |
    +--------------------v--------------------+
    |  AWS SES                                 |
    |  +-------------------------------------+ |
    |  | Domain Identity: akademiaplus.com   | |
    |  |  + Easy DKIM (2048-bit RSA)         | |
    |  |  + Custom MAIL FROM                 | |
    |  +------------------+------------------+ |
    |                     |                    |
    |  +------------------v------------------+ |
    |  | Configuration Set                   | |
    |  |  akademiaplus-email-tracking-{env}  | |
    |  |  -> Event Destination -> SNS        | |
    |  +------------------+------------------+ |
    +---------------------+--------------------+
                          |
    +---------------------v--------------------+
    |  SNS Topic -> SQS Queue + DLQ            |
    |  (bounce, complaint, delivery,           |
    |   open, click, reject, send)             |
    +---------------------+--------------------+
                          |
    +---------------------v--------------------+
    |  platform-core-api (Spring Boot)          |
    |  EmailDeliveryChannelStrategy             |
    |   -> JavaMailSender.send(mimeMessage)      |
    |   -> X-SES-CONFIGURATION-SET header        |
    |  (Future) SQS Consumer -> update status    |
    +------------------------------------------+

IAM User: akademiaplus-ses-smtp
  -> Policy: ses:SendRawEmail (scoped to domain)
  -> Access Key -> SMTP password (derived via AWS algorithm)

CloudWatch Alarms:
  +-- Bounce rate > 5%
  +-- Complaint rate > 0.1%
```

### 1.3 Stack Separation

Four independent CloudFormation stacks with isolated blast radius:

| Stack | File | Resources |
|-------|------|-----------|
| Domain Identity | `ses-domain-identity.yaml` | `AWS::SES::EmailIdentity` |
| SMTP Credentials | `ses-smtp-credentials.yaml` | `AWS::IAM::User`, `AWS::IAM::Policy`, `AWS::IAM::AccessKey` |
| Event Tracking | `ses-event-tracking.yaml` | `AWS::SES::ConfigurationSet`, `AWS::SNS::Topic`, `AWS::SQS::Queue`, DLQ |
| Production Alarms | `ses-production-alarms.yaml` | `AWS::CloudWatch::Alarm` x2, `AWS::SNS::Topic` |

### 1.4 Delivery Flow

```
User triggers notification (API call)
  -> EmailDeliveryChannelStrategy.deliver()
     -> JavaMailSender.send(mimeMessage)
        -> mimeMessage header: X-SES-CONFIGURATION-SET = akademiaplus-email-tracking-{env}
        -> SMTP to email-smtp.us-east-1.amazonaws.com:587
           -> SES authenticates IAM SMTP credentials
           -> SES applies DKIM signature
           -> SES sends via Custom MAIL FROM (mail.akademiaplus.com)
           -> SES publishes event to Configuration Set
              -> SNS Topic -> SQS Queue
                 -> (Future) SQS Consumer updates DeliveryStatus
```

---

## 2. File Inventory

### 2.1 New Files

| File | Responsibility | Phase |
|------|---------------|-------|
| `infra/aws/ses/ses-domain-identity.yaml` | SES domain identity with Easy DKIM + Custom MAIL FROM | 1 |
| `infra/aws/ses/parameters/dev.json` | Dev environment parameter values | 1 |
| `infra/aws/ses/parameters/prod.json` | Prod environment parameter values | 1 |
| `infra/aws/ses/ses-smtp-credentials.yaml` | IAM user + policy + access key for SMTP | 2 |
| `infra/aws/ses/ses-event-tracking.yaml` | Configuration Set + SNS + SQS pipeline | 3 |
| `infra/aws/ses/ses-production-alarms.yaml` | CloudWatch bounce/complaint rate alarms | 4 |
| `infra/aws/ses/README.md` | Deployment guide, DNS reference, credential rotation | 6 |

### 2.2 Modified Files

| File | Change | Phase |
|------|--------|-------|
| `application/src/main/resources/application.properties` | Add `akademia.email.ses-configuration-set` property | 5 |
| `notification-system/.../EmailDeliveryChannelStrategy.java` | Add `X-SES-CONFIGURATION-SET` header to MimeMessage | 5 |
| `notification-system/.../ImmediateSendUseCase.java` | Add `X-SES-CONFIGURATION-SET` header to MimeMessage | 5 |

### 2.3 Test Files

No new test files. Phase 5 Java changes are compile-verified only — existing unit tests remain green (header addition is transparent to mock `JavaMailSender`).

---

## 3. Implementation Sequence

### Phase Dependency Graph

```
Phase 1: SES Domain Identity + DNS
    |
Phase 2: SMTP Credentials (IAM)
    |
Phase 3: Event Tracking (Config Set + SNS + SQS)
    |
Phase 4: Production Alarms (CloudWatch)
    |
Phase 5: Application Integration (Java -- X-SES-CONFIGURATION-SET header)
    |
Phase 6: README + Production Readiness
```

Phases 1-4 create CloudFormation templates (no deployment). Phase 5 modifies Java code. Phase 6 writes documentation.

---

### Phase 1: SES Domain Identity + DNS Verification

**Creates**: `infra/aws/ses/ses-domain-identity.yaml`, `infra/aws/ses/parameters/dev.json`, `infra/aws/ses/parameters/prod.json`

#### 1.1 CloudFormation Template

**File**: `infra/aws/ses/ses-domain-identity.yaml`

**Parameters**:

| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| `Environment` | `String` | `dev` | Environment name (dev, staging, prod) |
| `DomainName` | `String` | `akademiaplus.com` | Email sending domain |
| `MailFromSubdomain` | `String` | `mail` | Custom MAIL FROM subdomain |

**Resources**:

```yaml
AWSTemplateFormatVersion: '2010-09-09'
Description: >-
  SES domain identity with Easy DKIM and Custom MAIL FROM for akademiaplus.com.
  Outputs DNS records that must be added manually at the domain registrar.

Parameters:
  Environment:
    Type: String
    Default: dev
    AllowedValues: [dev, staging, prod]
  DomainName:
    Type: String
    Default: akademiaplus.com
  MailFromSubdomain:
    Type: String
    Default: mail

Resources:
  SesDomainIdentity:
    Type: AWS::SES::EmailIdentity
    Properties:
      EmailIdentity: !Ref DomainName
      DkimSigningAttributes:
        NextSigningKeyLength: RSA_2048_BIT
      MailFromAttributes:
        MailFromDomain: !Sub '${MailFromSubdomain}.${DomainName}'
        BehaviorOnMxFailure: USE_DEFAULT_VALUE

Outputs:
  DomainIdentityArn:
    Description: ARN of the SES domain identity
    Value: !Sub 'arn:aws:ses:${AWS::Region}:${AWS::AccountId}:identity/${DomainName}'
    Export:
      Name: !Sub '${Environment}-ses-domain-identity-arn'
  DomainName:
    Description: The verified domain name
    Value: !Ref DomainName
    Export:
      Name: !Sub '${Environment}-ses-domain-name'
  MailFromDomain:
    Description: Custom MAIL FROM domain
    Value: !Sub '${MailFromSubdomain}.${DomainName}'
    Export:
      Name: !Sub '${Environment}-ses-mail-from-domain'
```

**DNS Records to Add** (after stack creation):

| Type | Name | Value |
|------|------|-------|
| CNAME | `{token1}._domainkey.akademiaplus.com` | `{token1}.dkim.amazonses.com` |
| CNAME | `{token2}._domainkey.akademiaplus.com` | `{token2}.dkim.amazonses.com` |
| CNAME | `{token3}._domainkey.akademiaplus.com` | `{token3}.dkim.amazonses.com` |
| MX | `mail.akademiaplus.com` | `10 feedback-smtp.us-east-1.amazonses.com` |
| TXT | `mail.akademiaplus.com` | `"v=spf1 include:amazonses.com ~all"` |
| TXT | `_dmarc.akademiaplus.com` | `"v=DMARC1; p=quarantine; rua=mailto:dmarc-reports@akademiaplus.com"` |

The DKIM CNAME token values are generated by AWS after stack creation. Retrieve them via:

```bash
aws sesv2 get-email-identity --email-identity akademiaplus.com \
  --query 'DkimAttributes.Tokens' --output text
```

#### 1.2 Parameter Files

**File**: `infra/aws/ses/parameters/dev.json`

```json
[
  {
    "ParameterKey": "Environment",
    "ParameterValue": "dev"
  },
  {
    "ParameterKey": "DomainName",
    "ParameterValue": "akademiaplus.com"
  },
  {
    "ParameterKey": "MailFromSubdomain",
    "ParameterValue": "mail"
  }
]
```

**File**: `infra/aws/ses/parameters/prod.json`

```json
[
  {
    "ParameterKey": "Environment",
    "ParameterValue": "prod"
  },
  {
    "ParameterKey": "DomainName",
    "ParameterValue": "akademiaplus.com"
  },
  {
    "ParameterKey": "MailFromSubdomain",
    "ParameterValue": "mail"
  }
]
```

**Verification gate**:

```bash
aws cloudformation validate-template --template-body file://infra/aws/ses/ses-domain-identity.yaml
```

**Deploy command** (reference only — do not auto-deploy):

```bash
aws cloudformation create-stack \
  --stack-name akademiaplus-ses-domain-dev \
  --template-body file://infra/aws/ses/ses-domain-identity.yaml \
  --parameters file://infra/aws/ses/parameters/dev.json
```

**Post-deploy verification**:

```bash
aws sesv2 get-email-identity --email-identity akademiaplus.com
```

---

### Phase 2: SMTP Credentials

**Creates**: `infra/aws/ses/ses-smtp-credentials.yaml`

#### 2.1 CloudFormation Template

**File**: `infra/aws/ses/ses-smtp-credentials.yaml`

**Parameters**:

| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| `Environment` | `String` | `dev` | Environment name |
| `DomainName` | `String` | `akademiaplus.com` | Domain for ARN scoping |

**Resources**:

```yaml
AWSTemplateFormatVersion: '2010-09-09'
Description: >-
  IAM user and policy for SES SMTP authentication.
  The access key secret must be converted to an SMTP password using the
  AWS SMTP credential derivation algorithm (see Outputs).

Parameters:
  Environment:
    Type: String
    Default: dev
    AllowedValues: [dev, staging, prod]
  DomainName:
    Type: String
    Default: akademiaplus.com

Resources:
  SesSmtpUser:
    Type: AWS::IAM::User
    Properties:
      UserName: !Sub 'akademiaplus-ses-smtp-${Environment}'
      Tags:
        - Key: Environment
          Value: !Ref Environment
        - Key: Purpose
          Value: SES SMTP email sending

  SesSmtpPolicy:
    Type: AWS::IAM::Policy
    Properties:
      PolicyName: !Sub 'akademiaplus-ses-send-${Environment}'
      Users:
        - !Ref SesSmtpUser
      PolicyDocument:
        Version: '2012-10-17'
        Statement:
          - Effect: Allow
            Action:
              - ses:SendRawEmail
            Resource:
              - !Sub 'arn:aws:ses:${AWS::Region}:${AWS::AccountId}:identity/${DomainName}'
            Condition:
              StringEquals:
                ses:FromAddress: !Sub 'noreply@${DomainName}'

  SesSmtpAccessKey:
    Type: AWS::IAM::AccessKey
    Properties:
      UserName: !Ref SesSmtpUser

Outputs:
  SmtpUsername:
    Description: SMTP username (same as IAM access key ID)
    Value: !Ref SesSmtpAccessKey
  SmtpSecretAccessKey:
    Description: >-
      IAM secret access key. IMPORTANT: This is NOT the SMTP password.
      You must derive the SMTP password using the AWS algorithm (see README).
    Value: !GetAtt SesSmtpAccessKey.SecretAccessKey
  SmtpEndpoint:
    Description: SES SMTP endpoint
    Value: !Sub 'email-smtp.${AWS::Region}.amazonaws.com'
  SmtpPort:
    Description: SMTP port (STARTTLS)
    Value: '587'
  IamUserArn:
    Description: ARN of the SMTP IAM user
    Value: !GetAtt SesSmtpUser.Arn
```

#### 2.2 SMTP Password Derivation

AWS SES SMTP passwords are **not** the same as IAM secret access keys. The secret key must be converted using an HMAC-SHA256 algorithm specified by AWS.

**Python derivation script** (reference — include in README):

```python
#!/usr/bin/env python3
"""
Derives an AWS SES SMTP password from an IAM secret access key.
Algorithm: https://docs.aws.amazon.com/ses/latest/dg/smtp-credentials.html
"""
import hmac
import hashlib
import base64
import sys

SMTP_REGIONS = [
    'us-east-1', 'us-west-2', 'ap-south-1', 'ap-southeast-1',
    'ap-southeast-2', 'ap-northeast-1', 'ca-central-1',
    'eu-central-1', 'eu-west-1', 'eu-west-2', 'eu-west-3',
    'sa-east-1', 'us-gov-west-1',
]

DATE = "11111111"
SERVICE = "ses"
MESSAGE = "SendRawEmail"
TERMINAL = "aws4_request"
VERSION = 0x04


def sign(key: bytes, msg: str) -> bytes:
    return hmac.new(key, msg.encode('utf-8'), hashlib.sha256).digest()


def calculate_key(secret_access_key: str, region: str) -> str:
    signature = sign(("AWS4" + secret_access_key).encode('utf-8'), DATE)
    signature = sign(signature, region)
    signature = sign(signature, SERVICE)
    signature = sign(signature, TERMINAL)
    signature = sign(signature, MESSAGE)
    signature_and_version = bytes([VERSION]) + signature
    return base64.b64encode(signature_and_version).decode('utf-8')


if __name__ == "__main__":
    if len(sys.argv) != 3:
        print(f"Usage: {sys.argv[0]} <secret_access_key> <region>")
        sys.exit(1)
    secret_key = sys.argv[1]
    region = sys.argv[2]
    if region not in SMTP_REGIONS:
        print(f"Warning: {region} may not support SES SMTP")
    print(calculate_key(secret_key, region))
```

**Usage**:

```bash
python3 derive_smtp_password.py <IAM_SECRET_ACCESS_KEY> us-east-1
```

**Verification gate**:

```bash
aws cloudformation validate-template --template-body file://infra/aws/ses/ses-smtp-credentials.yaml
```

---

### Phase 3: Event Tracking Pipeline

**Creates**: `infra/aws/ses/ses-event-tracking.yaml`

#### 3.1 CloudFormation Template

**File**: `infra/aws/ses/ses-event-tracking.yaml`

**Parameters**:

| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| `Environment` | `String` | `dev` | Environment name |
| `DomainName` | `String` | `akademiaplus.com` | Domain for identity ARN reference |
| `MessageRetentionPeriod` | `Number` | `1209600` | SQS message retention in seconds (14 days) |
| `MaxReceiveCount` | `Number` | `3` | Max delivery attempts before DLQ |

**Resources**:

```yaml
AWSTemplateFormatVersion: '2010-09-09'
Description: >-
  SES Configuration Set with SNS + SQS event tracking pipeline.
  Captures bounce, complaint, delivery, open, click, reject, and send events.

Parameters:
  Environment:
    Type: String
    Default: dev
    AllowedValues: [dev, staging, prod]
  DomainName:
    Type: String
    Default: akademiaplus.com
  MessageRetentionPeriod:
    Type: Number
    Default: 1209600
    Description: SQS message retention in seconds (default 14 days)
  MaxReceiveCount:
    Type: Number
    Default: 3
    Description: Max receive count before message goes to DLQ

Resources:

  # --- Configuration Set ---

  SesConfigurationSet:
    Type: AWS::SES::ConfigurationSet
    Properties:
      Name: !Sub 'akademiaplus-email-tracking-${Environment}'
      ReputationOptions:
        ReputationMetricsEnabled: true
      SendingOptions:
        SendingEnabled: true
      SuppressionOptions:
        SuppressedReasons:
          - BOUNCE
          - COMPLAINT

  # --- SNS Topic ---

  SesEventTopic:
    Type: AWS::SNS::Topic
    Properties:
      TopicName: !Sub 'akademiaplus-ses-events-${Environment}'
      Tags:
        - Key: Environment
          Value: !Ref Environment

  SesEventTopicPolicy:
    Type: AWS::SNS::TopicPolicy
    Properties:
      Topics:
        - !Ref SesEventTopic
      PolicyDocument:
        Version: '2012-10-17'
        Statement:
          - Sid: AllowSESPublish
            Effect: Allow
            Principal:
              Service: ses.amazonaws.com
            Action: sns:Publish
            Resource: !Ref SesEventTopic
            Condition:
              StringEquals:
                AWS:SourceAccount: !Ref AWS::AccountId
              ArnLike:
                AWS:SourceArn: !Sub 'arn:aws:ses:${AWS::Region}:${AWS::AccountId}:configuration-set/akademiaplus-email-tracking-${Environment}'

  # --- SES Event Destination ---

  SesEventDestination:
    Type: AWS::SES::ConfigurationSetEventDestination
    Properties:
      ConfigurationSetName: !Ref SesConfigurationSet
      EventDestination:
        Name: !Sub 'sns-all-events-${Environment}'
        Enabled: true
        MatchingEventTypes:
          - send
          - reject
          - bounce
          - complaint
          - delivery
          - open
          - click
        SnsDestination:
          TopicARN: !Ref SesEventTopic

  # --- SQS Dead Letter Queue ---

  SesEventDeadLetterQueue:
    Type: AWS::SQS::Queue
    Properties:
      QueueName: !Sub 'akademiaplus-ses-events-dlq-${Environment}'
      MessageRetentionPeriod: 1209600
      Tags:
        - Key: Environment
          Value: !Ref Environment

  # --- SQS Main Queue ---

  SesEventQueue:
    Type: AWS::SQS::Queue
    Properties:
      QueueName: !Sub 'akademiaplus-ses-events-${Environment}'
      MessageRetentionPeriod: !Ref MessageRetentionPeriod
      VisibilityTimeout: 300
      RedrivePolicy:
        deadLetterTargetArn: !GetAtt SesEventDeadLetterQueue.Arn
        maxReceiveCount: !Ref MaxReceiveCount
      Tags:
        - Key: Environment
          Value: !Ref Environment

  SesEventQueuePolicy:
    Type: AWS::SQS::QueuePolicy
    Properties:
      Queues:
        - !Ref SesEventQueue
      PolicyDocument:
        Version: '2012-10-17'
        Statement:
          - Sid: AllowSNSPublish
            Effect: Allow
            Principal:
              Service: sns.amazonaws.com
            Action: sqs:SendMessage
            Resource: !GetAtt SesEventQueue.Arn
            Condition:
              ArnEquals:
                aws:SourceArn: !Ref SesEventTopic

  # --- SNS -> SQS Subscription ---

  SesEventSubscription:
    Type: AWS::SNS::Subscription
    Properties:
      Protocol: sqs
      TopicArn: !Ref SesEventTopic
      Endpoint: !GetAtt SesEventQueue.Arn
      RawMessageDelivery: true

Outputs:
  ConfigurationSetName:
    Description: SES Configuration Set name (set as X-SES-CONFIGURATION-SET header)
    Value: !Ref SesConfigurationSet
    Export:
      Name: !Sub '${Environment}-ses-configuration-set-name'
  EventQueueUrl:
    Description: SQS queue URL for consuming SES events
    Value: !Ref SesEventQueue
    Export:
      Name: !Sub '${Environment}-ses-event-queue-url'
  EventQueueArn:
    Description: SQS queue ARN
    Value: !GetAtt SesEventQueue.Arn
    Export:
      Name: !Sub '${Environment}-ses-event-queue-arn'
  DeadLetterQueueUrl:
    Description: DLQ URL for failed event processing
    Value: !Ref SesEventDeadLetterQueue
  EventTopicArn:
    Description: SNS topic ARN for SES events
    Value: !Ref SesEventTopic
    Export:
      Name: !Sub '${Environment}-ses-event-topic-arn'
```

**Verification gate**:

```bash
aws cloudformation validate-template --template-body file://infra/aws/ses/ses-event-tracking.yaml
```

---

### Phase 4: Production Alarms

**Creates**: `infra/aws/ses/ses-production-alarms.yaml`

#### 4.1 CloudFormation Template

**File**: `infra/aws/ses/ses-production-alarms.yaml`

**Parameters**:

| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| `Environment` | `String` | `dev` | Environment name |
| `AlarmEmail` | `String` | — | Email for alarm notifications |
| `BounceRateThreshold` | `Number` | `5` | Bounce rate alarm threshold (%) |
| `ComplaintRateThreshold` | `Number` | `0.1` | Complaint rate alarm threshold (%) |

**Resources**:

```yaml
AWSTemplateFormatVersion: '2010-09-09'
Description: >-
  CloudWatch alarms for SES bounce and complaint rates.
  AWS suspends sending if bounce rate exceeds 10% or complaint rate exceeds 0.5%.
  These alarms trigger at 5% and 0.1% respectively for early warning.

Parameters:
  Environment:
    Type: String
    Default: dev
    AllowedValues: [dev, staging, prod]
  AlarmEmail:
    Type: String
    Description: Email address for alarm notifications
  BounceRateThreshold:
    Type: Number
    Default: 5
    Description: Bounce rate threshold percentage (AWS limit is 10%)
  ComplaintRateThreshold:
    Type: Number
    Default: 0.1
    Description: Complaint rate threshold percentage (AWS limit is 0.5%)

Resources:

  AlarmNotificationTopic:
    Type: AWS::SNS::Topic
    Properties:
      TopicName: !Sub 'akademiaplus-ses-alarms-${Environment}'

  AlarmEmailSubscription:
    Type: AWS::SNS::Subscription
    Properties:
      Protocol: email
      TopicArn: !Ref AlarmNotificationTopic
      Endpoint: !Ref AlarmEmail

  BounceRateAlarm:
    Type: AWS::CloudWatch::Alarm
    Properties:
      AlarmName: !Sub 'akademiaplus-ses-bounce-rate-${Environment}'
      AlarmDescription: !Sub >-
        SES bounce rate exceeds ${BounceRateThreshold}%.
        AWS suspends sending at 10%. Investigate bounced addresses immediately.
      Namespace: AWS/SES
      MetricName: Reputation.BounceRate
      Statistic: Average
      Period: 300
      EvaluationPeriods: 3
      Threshold: !Ref BounceRateThreshold
      ComparisonOperator: GreaterThanThreshold
      TreatMissingData: notBreaching
      AlarmActions:
        - !Ref AlarmNotificationTopic
      OKActions:
        - !Ref AlarmNotificationTopic

  ComplaintRateAlarm:
    Type: AWS::CloudWatch::Alarm
    Properties:
      AlarmName: !Sub 'akademiaplus-ses-complaint-rate-${Environment}'
      AlarmDescription: !Sub >-
        SES complaint rate exceeds ${ComplaintRateThreshold}%.
        AWS suspends sending at 0.5%. Review email content and recipient lists.
      Namespace: AWS/SES
      MetricName: Reputation.ComplaintRate
      Statistic: Average
      Period: 300
      EvaluationPeriods: 3
      Threshold: !Ref ComplaintRateThreshold
      ComparisonOperator: GreaterThanThreshold
      TreatMissingData: notBreaching
      AlarmActions:
        - !Ref AlarmNotificationTopic
      OKActions:
        - !Ref AlarmNotificationTopic

Outputs:
  AlarmTopicArn:
    Description: SNS topic ARN for alarm notifications
    Value: !Ref AlarmNotificationTopic
  BounceRateAlarmArn:
    Description: Bounce rate alarm ARN
    Value: !GetAtt BounceRateAlarm.Arn
  ComplaintRateAlarmArn:
    Description: Complaint rate alarm ARN
    Value: !GetAtt ComplaintRateAlarm.Arn
```

**Verification gate**:

```bash
aws cloudformation validate-template --template-body file://infra/aws/ses/ses-production-alarms.yaml
```

---

### Phase 5: Application Integration (Java)

**Modifies**:
- `application/src/main/resources/application.properties`
- `notification-system/src/main/java/com/akademiaplus/notification/usecases/EmailDeliveryChannelStrategy.java`
- `notification-system/src/main/java/com/akademiaplus/notification/usecases/ImmediateSendUseCase.java`

#### 5.1 Configuration Property

Add to `application.properties`:

```properties
# SES Configuration Set (enables email event tracking)
akademia.email.ses-configuration-set=${SES_CONFIGURATION_SET:}
```

When empty (default), no header is added — emails are sent without event tracking. When set (e.g., `akademiaplus-email-tracking-prod`), all outgoing emails include the `X-SES-CONFIGURATION-SET` header.

#### 5.2 EmailDeliveryChannelStrategy Changes

Add field:

```java
@Value("${akademia.email.ses-configuration-set:}")
private String sesConfigurationSet;
```

Add constant:

```java
public static final String HEADER_SES_CONFIGURATION_SET = "X-SES-CONFIGURATION-SET";
```

In the `deliver()` method, after creating the `MimeMessage` and before calling `javaMailSender.send()`:

```java
if (sesConfigurationSet != null && !sesConfigurationSet.isBlank()) {
    mimeMessage.setHeader(HEADER_SES_CONFIGURATION_SET, sesConfigurationSet);
}
```

#### 5.3 ImmediateSendUseCase Changes

Same pattern — inject the configuration set name and add the header to each `MimeMessage` before sending:

```java
@Value("${akademia.email.ses-configuration-set:}")
private String sesConfigurationSet;
```

Reuse the constant from `EmailDeliveryChannelStrategy`:

```java
if (sesConfigurationSet != null && !sesConfigurationSet.isBlank()) {
    mimeMessage.setHeader(EmailDeliveryChannelStrategy.HEADER_SES_CONFIGURATION_SET, sesConfigurationSet);
}
```

**Verification gate**:

```bash
mvn clean compile -pl notification-system -am -DskipTests -f platform-core-api/pom.xml
```

---

### Phase 6: README + Production Readiness

**Creates**: `infra/aws/ses/README.md`

#### 6.1 README Content

The README covers:

1. **Quick Start** — stack creation order (1→4), parameter file usage
2. **DNS Records Reference** — complete table with all 6 records (3 DKIM CNAMEs, 1 MX, 2 TXTs)
3. **SMTP Credential Setup** — derivation script usage, environment variable mapping
4. **Configuration Set Integration** — `SES_CONFIGURATION_SET` environment variable
5. **Sandbox Exit** — `aws sesv2 put-account-details` command for production access request
6. **Credential Rotation** — step-by-step procedure (create new key, derive password, update env, delete old key)
7. **Environment Variables** — complete mapping table:

| Variable | Source | Description |
|----------|--------|-------------|
| `EMAIL_SMTP_HOST` | Stack 1 output | SES SMTP endpoint |
| `EMAIL_SMTP_PORT` | `587` | SMTP port |
| `EMAIL_SMTP_USERNAME` | Stack 2 output `SmtpUsername` | IAM access key ID |
| `EMAIL_SMTP_PASSWORD` | Derived from Stack 2 `SmtpSecretAccessKey` | SMTP password |
| `EMAIL_FROM_ADDRESS` | `noreply@akademiaplus.com` | Sender address |
| `EMAIL_FROM_NAME` | `AkademiaPlus` | Sender display name |
| `SES_CONFIGURATION_SET` | Stack 3 output `ConfigurationSetName` | Event tracking config set |

8. **Future Enhancements**:
   - SQS consumer for delivery status updates (DELIVERED, BOUNCED, OPENED, CLICKED)
   - AWS Secrets Manager for automatic SMTP credential rotation
   - VPC endpoint for SES (`com.amazonaws.us-east-1.email-smtp`) to avoid public internet
   - Dedicated IP addresses for sender reputation isolation

---

## 4. Key Design Decisions

### 4.1 DKIM: Easy DKIM (AWS-Managed) over BYODKIM

| Factor | Easy DKIM | BYODKIM |
|--------|-----------|---------|
| Key management | AWS rotates automatically | Manual rotation |
| Setup | Zero-config (just add CNAMEs) | Generate RSA key pair, upload public key |
| Key length | 2048-bit RSA | Any length |
| Maintenance | None | Must track expiry and rotate |

**Decision**: Easy DKIM. AWS rotates keys automatically — zero operational burden. BYODKIM only needed when sharing DKIM keys across multiple providers.

### 4.2 MAIL FROM: Custom over Default

| Factor | Custom (`mail.akademiaplus.com`) | Default (`amazonses.com`) |
|--------|----------------------------------|---------------------------|
| DMARC alignment | Full (SPF aligns with From domain) | Fails SPF alignment |
| Deliverability | Higher — passes strict DMARC checks | Lower — some recipients reject |
| DNS setup | 2 additional records (MX + TXT) | None |

**Decision**: Custom MAIL FROM. Full DMARC alignment is critical for deliverability. The 2 extra DNS records are a one-time setup cost.

### 4.3 Event Consumption: SQS Polling over SNS HTTP Push

| Factor | SQS Polling | SNS HTTP Push |
|--------|-------------|---------------|
| Public endpoint | Not required | Required (webhook URL) |
| Retry | Built-in with DLQ | Must handle at HTTP layer |
| Ordering | Best-effort FIFO | No ordering guarantee |
| Decoupling | Full — consumer can be offline | Consumer must be available |

**Decision**: SQS polling. No public endpoint needed — the Spring Boot app polls the queue. Built-in DLQ handles processing failures. Consumer can be deployed independently of the SES infrastructure.

### 4.4 Stack Separation: 4 Stacks over Monolithic

| Factor | 4 Separate Stacks | 1 Monolithic Stack |
|--------|--------------------|--------------------|
| Blast radius | Isolated — alarm change doesn't affect identity | Any change touches everything |
| Deployment | Independent lifecycle per concern | All-or-nothing |
| Cross-stack refs | Uses `Fn::ImportValue` | Internal `!Ref` |
| Complexity | Slightly more files | Single file |

**Decision**: 4 separate stacks. Each stack has an independent lifecycle — updating an alarm threshold doesn't risk the domain identity. Cross-stack references via exports are well-supported.

### 4.5 Credential Management: Manual with Docs over Secrets Manager

| Factor | Manual + Docs | AWS Secrets Manager |
|--------|---------------|---------------------|
| Cost | $0 | $0.40/secret/month + API calls |
| Rotation | Manual procedure (documented) | Automatic via Lambda |
| Complexity | Low — env vars | Medium — Secrets Manager SDK |
| Initial deployment | Faster | Requires Lambda function |

**Decision**: Manual credential management with a documented rotation procedure. Sufficient for initial deployment. Secrets Manager rotation is documented as a future enhancement.

### 4.6 Template Engine: Application-Level over SES Templates

| Factor | App-Level (existing) | SES Templates |
|--------|----------------------|---------------|
| Tenant scoping | Built-in — Hibernate filters | Not tenant-aware |
| Unit testing | Standard Mockito | Requires SES mock |
| Flexibility | Custom rendering service | Limited to SES syntax |
| Provider lock-in | None | AWS-specific |

**Decision**: Application-level templates (already built). Tenant-scoped, unit-testable, and provider-independent. SES Templates offer no advantage for this architecture.

---

## 5. Multi-Tenancy Considerations

### 5.1 Infrastructure Is Tenant-Agnostic

The AWS SES infrastructure (domain identity, SMTP credentials, SNS/SQS pipeline) is shared across all tenants. Multi-tenancy is enforced at the application layer — the Java code already scopes email records by `tenantId` via Hibernate filters.

### 5.2 Configuration Set Is Shared

A single SES Configuration Set (`akademiaplus-email-tracking-{env}`) handles all tenants. The `X-SES-CONFIGURATION-SET` header is the same for every email regardless of tenant. Per-tenant tracking is handled by the application's email delivery records, not by SES.

### 5.3 Future: Per-Tenant Sender Addresses

If tenants need custom sender addresses (e.g., `noreply@tenant.akademiaplus.com`), additional SES identities would be required per subdomain. This is out of scope for the current workflow.

---

## 6. Future Extensibility

### 6.1 SQS Consumer for Delivery Status Updates

Add a Spring `@Scheduled` consumer (or `spring-cloud-aws-sqs`) that:
1. Polls `akademiaplus-ses-events-{env}` queue
2. Parses SES event JSON (bounce, complaint, delivery, open, click)
3. Maps events to `DeliveryStatus` transitions via `EmailDeliveryManagementUseCase.updateDeliveryStatus()`

### 6.2 Secrets Manager Credential Rotation

Replace manual credential management with:
1. `AWS::SecretsManager::Secret` storing SMTP credentials
2. `AWS::Lambda::Function` for rotation (derives new SMTP password)
3. `AWS::SecretsManager::RotationSchedule` — 90-day auto-rotation
4. Application reads credentials from Secrets Manager at startup

### 6.3 VPC Endpoint for SES SMTP

Avoid public internet for SMTP traffic:
1. `AWS::EC2::VPCEndpoint` for `com.amazonaws.us-east-1.email-smtp`
2. Update security groups to allow port 587 to endpoint
3. Change SMTP host to VPC endpoint DNS name

### 6.4 Dedicated IP Addresses

For sender reputation isolation:
1. Request dedicated IPs via SES console
2. Create dedicated IP pool
3. Assign pool to configuration set
4. Warm up IPs gradually (start with low volume)

### 6.5 Suppression List Management

Integrate with SES account-level suppression list:
1. `aws sesv2 get-suppressed-destination` for bounce/complaint checks
2. `EmailDeliveryChannelStrategy` pre-checks suppression list before sending
3. Dashboard for suppression list management

---

## 7. Verification Checklist

Run after all phases complete:

```bash
# 1. Validate all CloudFormation templates
aws cloudformation validate-template --template-body file://infra/aws/ses/ses-domain-identity.yaml
aws cloudformation validate-template --template-body file://infra/aws/ses/ses-smtp-credentials.yaml
aws cloudformation validate-template --template-body file://infra/aws/ses/ses-event-tracking.yaml
aws cloudformation validate-template --template-body file://infra/aws/ses/ses-production-alarms.yaml

# 2. Java compilation (Phase 5 changes)
mvn clean compile -pl notification-system -am -DskipTests -f platform-core-api/pom.xml

# 3. Existing unit tests still pass
mvn test -pl notification-system -am -f platform-core-api/pom.xml

# 4. Full project build (no cross-module breakage)
mvn clean install -DskipTests -f platform-core-api/pom.xml

# 5. Convention compliance
grep -rn "any()" notification-system/src/test/ | grep -v "import" | wc -l  # must be 0
grep -rn "// Arrange" notification-system/src/test/ | wc -l               # must be 0

# 6. Verify README exists
test -f infra/aws/ses/README.md && echo "OK" || echo "MISSING"

# 7. Verify all CloudFormation files exist
ls -la infra/aws/ses/*.yaml
ls -la infra/aws/ses/parameters/*.json
```

---

## 8. Critical Reminders

1. **DNS is manual**: DKIM CNAMEs, MX, and TXT records must be added at the domain registrar after stack deployment — CloudFormation cannot manage external DNS
2. **SMTP password != IAM secret key**: The IAM access key secret must be converted to an SMTP password using the AWS derivation algorithm (HMAC-SHA256) — they are NOT interchangeable
3. **Sandbox limits**: New SES accounts are in sandbox mode — can only send to verified addresses. Submit production access request via `aws sesv2 put-account-details` before go-live
4. **Configuration Set header required**: Without the `X-SES-CONFIGURATION-SET` header, SES events are NOT tracked — the SNS/SQS pipeline receives nothing
5. **Template files only — no auto-deploy**: Phases 1-4 create CloudFormation templates. Deployment is a manual operation with explicit stack creation commands
6. **Existing tests must pass**: Phase 5 Java changes must not break existing unit tests. The header addition is transparent to mock `JavaMailSender`
7. **Commits**: Conventional Commits (`infra(ses): ...`, `feat(notification-system): ...`), NO `Co-Authored-By` or AI attribution
8. **Parameter files**: Both `dev.json` and `prod.json` use the same domain (`akademiaplus.com`) — the `Environment` parameter differentiates resource names
9. **SQS visibility timeout**: Set to 300s (5 minutes) — must be longer than the expected processing time for each event message
10. **Alarm thresholds**: Bounce > 5% and complaint > 0.1% trigger early warnings — AWS hard limits are 10% and 0.5% respectively. Exceeding hard limits causes account suspension
