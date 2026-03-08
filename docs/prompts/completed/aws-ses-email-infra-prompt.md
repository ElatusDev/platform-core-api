# AWS SES Email Infrastructure — Claude Code Execution Prompt

**Target**: Claude Code CLI
**Repo**: `/Volumes/ElatusDev/ElatusDev/platform/core-api`
**Spec**: `docs/workflows/pending/aws-ses-email-infra-workflow.md` — read this first.
**Prerequisites**: Read `docs/directives/CLAUDE.md` and `docs/directives/AI-CODE-REF.md` before writing any code.

---

## EXECUTION RULES

1. Execute phases **strictly in order** (1 → 2 → ... → 6). Do NOT skip ahead.
2. Before writing any code or templates, read the existing files listed in each phase's "Read first" section.
3. **Validate gate**: After each phase that produces CloudFormation templates, run `aws cloudformation validate-template`. Fix all errors before proceeding.
4. **Compile gate**: After Phase 5 (Java changes), run the specified `mvn` verification command. Fix all errors before proceeding.
5. All CloudFormation templates MUST include a `Description` field.
6. All new Java code MUST include the ElatusDev copyright header and Javadoc on public classes/methods.
7. Test methods: `shouldDoX_whenGivenY()` with `@DisplayName`, Given-When-Then comments, zero `any()` matchers.
8. All string literals → `public static final` constants, shared between impl and tests.
9. Read existing files BEFORE modifying — field names, import paths, and method signatures may have changed.
10. Commit after each phase using the commit message provided.
11. Do NOT deploy CloudFormation stacks — this prompt creates templates only.

---

## Phase 1: SES Domain Identity + DNS Verification

### Read first

```bash
ls infra/aws/ 2>/dev/null || echo "Directory does not exist yet"
cat application/src/main/resources/application.properties | grep -i "email\|smtp\|ses"
```

### Step 1.1: Create directory structure

```bash
mkdir -p infra/aws/ses/parameters
```

### Step 1.2: Create SES domain identity template

**File**: `infra/aws/ses/ses-domain-identity.yaml`

```yaml
AWSTemplateFormatVersion: '2010-09-09'
Description: >-
  SES domain identity with Easy DKIM and Custom MAIL FROM for akademiaplus.com.
  Outputs DNS records that must be added manually at the domain registrar.

Parameters:
  Environment:
    Type: String
    Default: dev
    AllowedValues:
      - dev
      - staging
      - prod
    Description: Environment name for resource naming
  DomainName:
    Type: String
    Default: akademiaplus.com
    Description: Email sending domain
  MailFromSubdomain:
    Type: String
    Default: mail
    Description: Custom MAIL FROM subdomain (prepended to DomainName)

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

### Step 1.3: Create dev parameter file

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

### Step 1.4: Create prod parameter file

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

### Verify Phase 1

```bash
aws cloudformation validate-template --template-body file://infra/aws/ses/ses-domain-identity.yaml
```

### Commit Phase 1

```bash
git add infra/aws/ses/ses-domain-identity.yaml infra/aws/ses/parameters/
git commit -m "infra(ses): add SES domain identity with Easy DKIM and Custom MAIL FROM

Add CloudFormation template for akademiaplus.com domain identity with
2048-bit RSA Easy DKIM and Custom MAIL FROM (mail.akademiaplus.com).
Add dev and prod parameter files. DNS records must be added manually
at the domain registrar after stack deployment."
```

---

## Phase 2: SMTP Credentials

### Read first

```bash
cat infra/aws/ses/ses-domain-identity.yaml | grep -A5 "Outputs:"
```

### Step 2.1: Create SMTP credentials template

**File**: `infra/aws/ses/ses-smtp-credentials.yaml`

```yaml
AWSTemplateFormatVersion: '2010-09-09'
Description: >-
  IAM user and policy for SES SMTP authentication.
  The access key secret must be converted to an SMTP password using the
  AWS SMTP credential derivation algorithm (see README).

Parameters:
  Environment:
    Type: String
    Default: dev
    AllowedValues:
      - dev
      - staging
      - prod
    Description: Environment name for resource naming
  DomainName:
    Type: String
    Default: akademiaplus.com
    Description: Domain for IAM policy ARN scoping

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
      IAM secret access key. IMPORTANT -- this is NOT the SMTP password.
      Derive the SMTP password using the script in README.md.
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

### Verify Phase 2

```bash
aws cloudformation validate-template --template-body file://infra/aws/ses/ses-smtp-credentials.yaml
```

### Commit Phase 2

```bash
git add infra/aws/ses/ses-smtp-credentials.yaml
git commit -m "infra(ses): add IAM user and policy for SES SMTP credentials

Add CloudFormation template for dedicated SMTP IAM user with
ses:SendRawEmail permission scoped to akademiaplus.com identity.
Access key output requires SMTP password derivation via HMAC-SHA256."
```

---

## Phase 3: Event Tracking Pipeline

### Read first

```bash
cat infra/aws/ses/ses-domain-identity.yaml | grep "Export:" -A2
cat infra/aws/ses/ses-smtp-credentials.yaml | grep "Export:" -A2
```

### Step 3.1: Create event tracking template

**File**: `infra/aws/ses/ses-event-tracking.yaml`

```yaml
AWSTemplateFormatVersion: '2010-09-09'
Description: >-
  SES Configuration Set with SNS + SQS event tracking pipeline.
  Captures bounce, complaint, delivery, open, click, reject, and send events.

Parameters:
  Environment:
    Type: String
    Default: dev
    AllowedValues:
      - dev
      - staging
      - prod
    Description: Environment name for resource naming
  DomainName:
    Type: String
    Default: akademiaplus.com
    Description: Domain for identity ARN reference
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

### Verify Phase 3

```bash
aws cloudformation validate-template --template-body file://infra/aws/ses/ses-event-tracking.yaml
```

### Commit Phase 3

```bash
git add infra/aws/ses/ses-event-tracking.yaml
git commit -m "infra(ses): add event tracking pipeline with SNS topic and SQS queue

Add CloudFormation template for SES Configuration Set with reputation
metrics and suppression. SNS topic receives all SES event types
(bounce, complaint, delivery, open, click, reject, send). SQS queue
with 3-retry DLQ for application consumption."
```

---

## Phase 4: Production Alarms

### Read first

```bash
cat infra/aws/ses/ses-event-tracking.yaml | grep "ConfigurationSetName" | head -3
```

### Step 4.1: Create production alarms template

**File**: `infra/aws/ses/ses-production-alarms.yaml`

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
    AllowedValues:
      - dev
      - staging
      - prod
    Description: Environment name for resource naming
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

### Verify Phase 4

```bash
aws cloudformation validate-template --template-body file://infra/aws/ses/ses-production-alarms.yaml
```

### Commit Phase 4

```bash
git add infra/aws/ses/ses-production-alarms.yaml
git commit -m "infra(ses): add CloudWatch alarms for bounce and complaint rate

Add CloudFormation template with two CloudWatch alarms:
bounce rate > 5% and complaint rate > 0.1% (3 consecutive 5-min periods).
SNS topic with email subscription for alarm notifications.
Thresholds set below AWS hard limits (10% bounce, 0.5% complaint)."
```

---

## Phase 5: Application Integration (Java)

### Read first

```bash
cat notification-system/src/main/java/com/akademiaplus/notification/usecases/EmailDeliveryChannelStrategy.java
cat notification-system/src/main/java/com/akademiaplus/notification/usecases/ImmediateSendUseCase.java
cat application/src/main/resources/application.properties | grep -i "email\|smtp\|ses"
```

### Step 5.1: Add configuration property

**File**: `application/src/main/resources/application.properties`

Append after the existing email SMTP properties block:

```properties
# SES Configuration Set (enables email event tracking)
akademia.email.ses-configuration-set=${SES_CONFIGURATION_SET:}
```

### Step 5.2: Add SES header to EmailDeliveryChannelStrategy

**File**: `notification-system/src/main/java/com/akademiaplus/notification/usecases/EmailDeliveryChannelStrategy.java`

Add constant (alongside existing constants):

```java
/** SES Configuration Set header name for email event tracking. */
public static final String HEADER_SES_CONFIGURATION_SET = "X-SES-CONFIGURATION-SET";
```

Add field (alongside existing `@Value` fields):

```java
@Value("${akademia.email.ses-configuration-set:}")
private String sesConfigurationSet;
```

In the `deliver()` method, locate where the `MimeMessage` is created and configured. **After** all existing message setup (from, to, subject, body) and **before** `javaMailSender.send(mimeMessage)`, add:

```java
if (sesConfigurationSet != null && !sesConfigurationSet.isBlank()) {
    mimeMessage.setHeader(HEADER_SES_CONFIGURATION_SET, sesConfigurationSet);
}
```

### Step 5.3: Add SES header to ImmediateSendUseCase

**File**: `notification-system/src/main/java/com/akademiaplus/notification/usecases/ImmediateSendUseCase.java`

Add field (alongside existing `@Value` fields):

```java
@Value("${akademia.email.ses-configuration-set:}")
private String sesConfigurationSet;
```

In the method that builds and sends `MimeMessage` instances, **after** message setup and **before** `javaMailSender.send(mimeMessage)`, add:

```java
if (sesConfigurationSet != null && !sesConfigurationSet.isBlank()) {
    mimeMessage.setHeader(EmailDeliveryChannelStrategy.HEADER_SES_CONFIGURATION_SET, sesConfigurationSet);
}
```

**IMPORTANT**: Import `EmailDeliveryChannelStrategy` to reference the constant. Do NOT duplicate the constant definition.

### Verify Phase 5

```bash
mvn clean compile -pl notification-system -am -DskipTests -f platform-core-api/pom.xml
```

Also verify existing tests still pass:

```bash
mvn test -pl notification-system -am -f platform-core-api/pom.xml
```

### Commit Phase 5

```bash
git add application/src/main/resources/application.properties notification-system/src/main/java/com/akademiaplus/notification/usecases/EmailDeliveryChannelStrategy.java notification-system/src/main/java/com/akademiaplus/notification/usecases/ImmediateSendUseCase.java
git commit -m "feat(notification-system): add SES Configuration Set header for email tracking

Add X-SES-CONFIGURATION-SET header to MimeMessage in both
EmailDeliveryChannelStrategy and ImmediateSendUseCase. Header is
conditionally set when akademia.email.ses-configuration-set property
is non-empty. Enables SES event tracking via Configuration Set."
```

---

## Phase 6: README + Production Readiness

### Read first

```bash
ls infra/aws/ses/
cat infra/aws/ses/ses-domain-identity.yaml | grep -A3 "Outputs:"
cat infra/aws/ses/ses-smtp-credentials.yaml | grep -A3 "Outputs:"
cat infra/aws/ses/ses-event-tracking.yaml | grep -A3 "Outputs:"
```

### Step 6.1: Create README

**File**: `infra/aws/ses/README.md`

```markdown
# AWS SES Email Infrastructure — AkademiaPlus

CloudFormation templates for provisioning AWS SES email sending infrastructure.

## Quick Start

Deploy stacks in order (each stack is independent but logically sequential):

```bash
# 1. Domain Identity (creates SES identity + Easy DKIM)
aws cloudformation create-stack \
  --stack-name akademiaplus-ses-domain-prod \
  --template-body file://ses-domain-identity.yaml \
  --parameters file://parameters/prod.json

# 2. SMTP Credentials (creates IAM user + access key)
aws cloudformation create-stack \
  --stack-name akademiaplus-ses-smtp-prod \
  --template-body file://ses-smtp-credentials.yaml \
  --parameters file://parameters/prod.json \
  --capabilities CAPABILITY_NAMED_IAM

# 3. Event Tracking (creates Config Set + SNS + SQS)
aws cloudformation create-stack \
  --stack-name akademiaplus-ses-tracking-prod \
  --template-body file://ses-event-tracking.yaml \
  --parameters file://parameters/prod.json

# 4. Production Alarms (creates CloudWatch alarms)
aws cloudformation create-stack \
  --stack-name akademiaplus-ses-alarms-prod \
  --template-body file://ses-production-alarms.yaml \
  --parameters ParameterKey=Environment,ParameterValue=prod \
    ParameterKey=AlarmEmail,ParameterValue=ops@akademiaplus.com
```

## DNS Records

After deploying Stack 1, add these records at your domain registrar:

### DKIM (3 CNAME records)

Retrieve DKIM tokens:

```bash
aws sesv2 get-email-identity --email-identity akademiaplus.com \
  --query 'DkimAttributes.Tokens' --output text
```

For each token, create:

| Type | Name | Value |
|------|------|-------|
| CNAME | `{token}._domainkey.akademiaplus.com` | `{token}.dkim.amazonses.com` |

### Custom MAIL FROM (MX + TXT)

| Type | Name | Value |
|------|------|-------|
| MX | `mail.akademiaplus.com` | `10 feedback-smtp.us-east-1.amazonses.com` |
| TXT | `mail.akademiaplus.com` | `"v=spf1 include:amazonses.com ~all"` |

### DMARC (TXT)

| Type | Name | Value |
|------|------|-------|
| TXT | `_dmarc.akademiaplus.com` | `"v=DMARC1; p=quarantine; rua=mailto:dmarc-reports@akademiaplus.com"` |

### Verify DNS

```bash
# Check DKIM status
aws sesv2 get-email-identity --email-identity akademiaplus.com \
  --query 'DkimAttributes.DkimVerificationStatus'

# Check MAIL FROM status
aws sesv2 get-email-identity --email-identity akademiaplus.com \
  --query 'MailFromAttributes.MailFromDomainStatus'
```

## SMTP Credential Setup

### 1. Get the IAM secret access key

```bash
aws cloudformation describe-stacks \
  --stack-name akademiaplus-ses-smtp-prod \
  --query "Stacks[0].Outputs[?OutputKey=='SmtpSecretAccessKey'].OutputValue" \
  --output text
```

### 2. Derive the SMTP password

The IAM secret access key is NOT the SMTP password. Derive it using this Python script:

```python
#!/usr/bin/env python3
"""Derives an AWS SES SMTP password from an IAM secret access key."""
import hmac, hashlib, base64, sys

DATE = "11111111"
SERVICE = "ses"
MESSAGE = "SendRawEmail"
TERMINAL = "aws4_request"
VERSION = 0x04

def sign(key, msg):
    return hmac.new(key, msg.encode('utf-8'), hashlib.sha256).digest()

def calculate_key(secret_access_key, region):
    signature = sign(("AWS4" + secret_access_key).encode('utf-8'), DATE)
    signature = sign(signature, region)
    signature = sign(signature, SERVICE)
    signature = sign(signature, TERMINAL)
    signature = sign(signature, MESSAGE)
    signature_and_version = bytes([VERSION]) + signature
    return base64.b64encode(signature_and_version).decode('utf-8')

if __name__ == "__main__":
    print(calculate_key(sys.argv[1], sys.argv[2]))
```

Usage:

```bash
python3 derive_smtp_password.py <IAM_SECRET_ACCESS_KEY> us-east-1
```

### 3. Set environment variables

```bash
export EMAIL_SMTP_USERNAME=<SmtpUsername output>
export EMAIL_SMTP_PASSWORD=<derived SMTP password>
export SES_CONFIGURATION_SET=akademiaplus-email-tracking-prod
```

## Environment Variables

| Variable | Source | Description |
|----------|--------|-------------|
| `EMAIL_SMTP_HOST` | Default: `email-smtp.us-east-1.amazonaws.com` | SES SMTP endpoint |
| `EMAIL_SMTP_PORT` | Default: `587` | SMTP port (STARTTLS) |
| `EMAIL_SMTP_USERNAME` | Stack 2 output `SmtpUsername` | IAM access key ID |
| `EMAIL_SMTP_PASSWORD` | Derived from Stack 2 `SmtpSecretAccessKey` | SMTP password (HMAC-SHA256) |
| `EMAIL_FROM_ADDRESS` | Default: `noreply@akademiaplus.com` | Sender email address |
| `EMAIL_FROM_NAME` | Default: `AkademiaPlus` | Sender display name |
| `SES_CONFIGURATION_SET` | Stack 3 output `ConfigurationSetName` | Enables event tracking |

## Credential Rotation

1. Create a new access key for the SMTP IAM user:

```bash
aws iam create-access-key --user-name akademiaplus-ses-smtp-prod
```

2. Derive the new SMTP password from the new secret key.

3. Update `EMAIL_SMTP_USERNAME` and `EMAIL_SMTP_PASSWORD` in your deployment environment.

4. Verify email sending works with the new credentials.

5. Delete the old access key:

```bash
aws iam delete-access-key --user-name akademiaplus-ses-smtp-prod --access-key-id <OLD_KEY_ID>
```

## Sandbox Exit

New SES accounts are in sandbox mode (can only send to verified addresses). Request production access:

```bash
aws sesv2 put-account-details \
  --production-access-enabled \
  --mail-type TRANSACTIONAL \
  --website-url https://akademiaplus.com \
  --use-case-description "Transactional emails for AkademiaPlus learning platform: account verification, password reset, enrollment confirmations, and notification delivery." \
  --additional-contact-email-addresses ops@akademiaplus.com \
  --contact-language EN
```

## Future Enhancements

- **SQS Consumer**: Spring `@Scheduled` or `spring-cloud-aws-sqs` consumer to process SES events and update `DeliveryStatus` (DELIVERED, BOUNCED, OPENED, CLICKED)
- **Secrets Manager**: Automatic SMTP credential rotation via Lambda + `AWS::SecretsManager::RotationSchedule`
- **VPC Endpoint**: `com.amazonaws.us-east-1.email-smtp` VPC endpoint to avoid public internet for SMTP traffic
- **Dedicated IPs**: Request dedicated IP addresses for sender reputation isolation
```

### Verify Phase 6

```bash
test -f infra/aws/ses/README.md && echo "README exists" || echo "MISSING"
ls -la infra/aws/ses/
```

### Commit Phase 6

```bash
git add infra/aws/ses/README.md
git commit -m "docs(ses): add README with DNS records, credential rotation, and sandbox exit guide

Add comprehensive README covering stack deployment order, DNS record
reference table, SMTP password derivation script, environment variable
mapping, credential rotation procedure, and sandbox exit command."
```

---

## VERIFICATION CHECKLIST

Run after all phases complete:

- [ ] `aws cloudformation validate-template --template-body file://infra/aws/ses/ses-domain-identity.yaml` — passes
- [ ] `aws cloudformation validate-template --template-body file://infra/aws/ses/ses-smtp-credentials.yaml` — passes
- [ ] `aws cloudformation validate-template --template-body file://infra/aws/ses/ses-event-tracking.yaml` — passes
- [ ] `aws cloudformation validate-template --template-body file://infra/aws/ses/ses-production-alarms.yaml` — passes
- [ ] `mvn clean compile -pl notification-system -am -DskipTests -f platform-core-api/pom.xml` — compilation passes
- [ ] `mvn test -pl notification-system -am -f platform-core-api/pom.xml` — all existing unit tests green
- [ ] `mvn clean install -DskipTests -f platform-core-api/pom.xml` — full project build passes
- [ ] `infra/aws/ses/README.md` exists with deployment guide, DNS reference, credential rotation
- [ ] `infra/aws/ses/parameters/dev.json` and `prod.json` exist with correct parameter values
- [ ] `EmailDeliveryChannelStrategy.java` contains `HEADER_SES_CONFIGURATION_SET` constant
- [ ] `EmailDeliveryChannelStrategy.java` and `ImmediateSendUseCase.java` both set `X-SES-CONFIGURATION-SET` header
- [ ] `application.properties` contains `akademia.email.ses-configuration-set` property
- [ ] All commit messages use Conventional Commits format (no AI attribution)
