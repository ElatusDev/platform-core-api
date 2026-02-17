# Security Policy

## Supported Versions

| Version | Supported |
|---------|-----------|
| 1.0.x   | ✅ Active |

## Reporting a Vulnerability

**Do NOT open a public GitHub issue for security vulnerabilities.**

If you discover a security vulnerability in AkademiaPlus, please report it responsibly:

1. **Email**: Send details to **security@elatusdev.com**
2. **Subject line**: `[SECURITY] AkademiaPlus — Brief description`
3. **Include**:
   - Description of the vulnerability
   - Steps to reproduce
   - Potential impact assessment
   - Suggested fix (if any)

## Response Timeline

| Action | Target |
|--------|--------|
| Acknowledgment | Within 48 hours |
| Initial assessment | Within 5 business days |
| Fix or mitigation | Based on severity (Critical: 7 days, High: 14 days, Medium: 30 days) |

## Scope

This policy covers the `platform-core-api` repository including:

- Authentication and authorization (JWT, filters)
- Field-level encryption (AES-256-GCM)
- PII normalization and hashing
- Multi-tenant data isolation
- API endpoint security
- Dependency vulnerabilities

## Security Architecture

For details on the security layers implemented in this project, see the Security Layers
section in [DESIGN.md](DESIGN.md).

## Recognition

We appreciate responsible disclosure. Contributors who report valid vulnerabilities
will be acknowledged (with permission) in release notes.
