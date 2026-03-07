# AWS SES Email Infrastructure

CloudFormation templates for provisioning AWS SES email sending infrastructure for AkademiaPlus.

## Stack Overview

| Stack | Template | Purpose |
|-------|----------|---------|
| 1. Domain Identity | `ses-domain-identity.yaml` | SES domain identity with Easy DKIM + Custom MAIL FROM |
| 2. SMTP Credentials | `ses-smtp-credentials.yaml` | IAM user + policy + access key for SMTP auth |
| 3. Event Tracking | `ses-event-tracking.yaml` | Configuration Set + SNS + SQS event pipeline |
| 4. Production Alarms | `ses-production-alarms.yaml` | CloudWatch bounce/complaint rate alarms |

## Quick Start

Deploy stacks in order (1 → 4):

```bash
# 1. Domain Identity
aws cloudformation create-stack \
  --stack-name akademiaplus-ses-domain-dev \
  --template-body file://infra/aws/ses/ses-domain-identity.yaml \
  --parameters file://infra/aws/ses/parameters/dev.json

# 2. SMTP Credentials (requires IAM capabilities)
aws cloudformation create-stack \
  --stack-name akademiaplus-ses-smtp-dev \
  --template-body file://infra/aws/ses/ses-smtp-credentials.yaml \
  --parameters file://infra/aws/ses/parameters/dev.json \
  --capabilities CAPABILITY_NAMED_IAM

# 3. Event Tracking
aws cloudformation create-stack \
  --stack-name akademiaplus-ses-tracking-dev \
  --template-body file://infra/aws/ses/ses-event-tracking.yaml \
  --parameters file://infra/aws/ses/parameters/dev.json

# 4. Production Alarms
aws cloudformation create-stack \
  --stack-name akademiaplus-ses-alarms-dev \
  --template-body file://infra/aws/ses/ses-production-alarms.yaml \
  --parameters ParameterKey=Environment,ParameterValue=dev \
    ParameterKey=AlarmEmail,ParameterValue=alerts@akademiaplus.com
```

## DNS Records

After deploying Stack 1, add these records at your domain registrar:

| Type | Name | Value |
|------|------|-------|
| CNAME | `{token1}._domainkey.akademiaplus.com` | `{token1}.dkim.amazonses.com` |
| CNAME | `{token2}._domainkey.akademiaplus.com` | `{token2}.dkim.amazonses.com` |
| CNAME | `{token3}._domainkey.akademiaplus.com` | `{token3}.dkim.amazonses.com` |
| MX | `mail.akademiaplus.com` | `10 feedback-smtp.us-east-1.amazonses.com` |
| TXT | `mail.akademiaplus.com` | `"v=spf1 include:amazonses.com ~all"` |
| TXT | `_dmarc.akademiaplus.com` | `"v=DMARC1; p=quarantine; rua=mailto:dmarc-reports@akademiaplus.com"` |

Retrieve DKIM tokens after stack creation:

```bash
aws sesv2 get-email-identity --email-identity akademiaplus.com \
  --query 'DkimAttributes.Tokens' --output text
```

## SMTP Credential Setup

AWS SES SMTP passwords are **not** the same as IAM secret access keys. Derive the SMTP password:

```python
#!/usr/bin/env python3
"""Derives an AWS SES SMTP password from an IAM secret access key."""
import hmac, hashlib, base64, sys

def calculate_key(secret_access_key, region):
    date, service, terminal = "11111111", "ses", "aws4_request"
    sig = hmac.new(("AWS4" + secret_access_key).encode(), date.encode(), hashlib.sha256).digest()
    sig = hmac.new(sig, region.encode(), hashlib.sha256).digest()
    sig = hmac.new(sig, service.encode(), hashlib.sha256).digest()
    sig = hmac.new(sig, terminal.encode(), hashlib.sha256).digest()
    sig = hmac.new(sig, "SendRawEmail".encode(), hashlib.sha256).digest()
    return base64.b64encode(bytes([0x04]) + sig).decode()

if __name__ == "__main__":
    print(calculate_key(sys.argv[1], sys.argv[2]))
```

Usage: `python3 derive_smtp_password.py <IAM_SECRET_ACCESS_KEY> us-east-1`

## Environment Variables

| Variable | Source | Description |
|----------|--------|-------------|
| `EMAIL_SMTP_HOST` | Stack 2 output `SmtpEndpoint` | SES SMTP endpoint |
| `EMAIL_SMTP_PORT` | `587` | SMTP port (STARTTLS) |
| `EMAIL_SMTP_USERNAME` | Stack 2 output `SmtpUsername` | IAM access key ID |
| `EMAIL_SMTP_PASSWORD` | Derived from Stack 2 `SmtpSecretAccessKey` | SMTP password |
| `EMAIL_FROM_ADDRESS` | `noreply@akademiaplus.com` | Sender address |
| `EMAIL_FROM_NAME` | `AkademiaPlus` | Sender display name |
| `SES_CONFIGURATION_SET` | Stack 3 output `ConfigurationSetName` | Event tracking config set |

## Sandbox Exit

New SES accounts are in sandbox mode (can only send to verified addresses). Request production access:

```bash
aws sesv2 put-account-details \
  --mail-type TRANSACTIONAL \
  --website-url https://akademiaplus.com \
  --use-case-description "Transactional emails for school management platform" \
  --contact-language EN
```

## Credential Rotation

1. Create a new IAM access key for the SMTP user
2. Derive the new SMTP password using the script above
3. Update `EMAIL_SMTP_USERNAME` and `EMAIL_SMTP_PASSWORD` environment variables
4. Verify email sending works with the new credentials
5. Delete the old IAM access key

## Template Validation

```bash
aws cloudformation validate-template --template-body file://infra/aws/ses/ses-domain-identity.yaml
aws cloudformation validate-template --template-body file://infra/aws/ses/ses-smtp-credentials.yaml
aws cloudformation validate-template --template-body file://infra/aws/ses/ses-event-tracking.yaml
aws cloudformation validate-template --template-body file://infra/aws/ses/ses-production-alarms.yaml
```
