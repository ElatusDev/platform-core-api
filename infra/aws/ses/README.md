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
