# OAuth2 Social Login — Claude Code Slash Command

Execute the OAuth2 Social Login feature implementation.

## Instructions

1. Read the workflow document first:
   ```
   docs/workflows/pending/oauth-social-login-workflow.md
   ```

2. Read the execution prompt:
   ```
   docs/prompts/pending/oauth-social-login-prompt.md
   ```

3. Read the project directives before writing any code:
   ```
   docs/directives/CLAUDE.md
   docs/directives/AI-CODE-REF.md
   ```

4. Execute all 14 phases in the prompt document **strictly in order**.

5. After all 14 phases pass, switch to the `platform-api-e2e` repo and execute:
   ```
   platform-api-e2e/docs/prompts/pending/oauth-e2e-prompt.md
   ```

## Quick Reference

- **Modules touched**: `multi-tenant-data`, `security`, `user-management`, `application`
- **New endpoint**: `POST /v1/security/login/oauth`
- **Providers**: Google, Facebook (covers Instagram)
- **Pattern**: Token exchange — frontend sends auth code, API exchanges for access token
- **Tests**: Unit (security + application), Component (application), E2E (platform-api-e2e)
