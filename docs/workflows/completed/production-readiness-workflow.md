# Production Readiness Workflow

> **Scope**: Spring production profile, parameterize security properties, Docker Compose fixes
> **Trigger**: No `application-prod.properties` exists — prod profile falls back to base with localhost refs
> **Project**: core-api

---

## 1. Problem Statement

When `SPRING_PROFILES_ACTIVE=prod`, Spring loads `application.properties` (base) which
contains `jdbc:mariadb://localhost:3307/${DATABASE}` and uses env var names (`DB_USER`,
`REDIS_HOST`) that don't match what the ECS task definition provides (`SPRING_DATASOURCE_USERNAME`,
`SPRING_DATA_REDIS_HOST`). The QA profile fixes this, but no prod profile exists.

Additionally, several security properties (passkey RP config, per-app CORS origins,
IP whitelist CIDRs) are hardcoded to localhost values with no env var bindings,
making them impossible to configure per environment.

---

## 2. Changes

### 2.1 Create `application-prod.properties` (CRITICAL)

**File**: `application/src/main/resources/application-prod.properties` — **NEW**

Mirror `application-qa.properties` env var bindings but with production settings:
- `spring.jpa.hibernate.ddl-auto=validate` (NOT `update`)
- `spring.jpa.show-sql=false`
- `logging.level.com.akademiaplus=INFO` (not DEBUG)
- `spring.data.redis.ssl.enabled=${SPRING_DATA_REDIS_SSL_ENABLED:true}`
- App name: `PlatformCore Prod`

### 2.2 Parameterize passkey properties (HIGH)

**File**: `application/src/main/resources/application.properties`

| Current (hardcoded) | Target (parameterized) |
|---|---|
| `security.passkey.rp-id=localhost` | `security.passkey.rp-id=${PASSKEY_RP_ID:localhost}` |
| `security.passkey.rp-name=AkademiaPlus Dev` | `security.passkey.rp-name=${PASSKEY_RP_NAME:AkademiaPlus Dev}` |
| `security.passkey.allowed-origins[0]=http://localhost:3000` | `security.passkey.allowed-origins[0]=${PASSKEY_ALLOWED_ORIGIN:http://localhost:3000}` |

### 2.3 Parameterize per-app allowed-origins (HIGH)

| Current | Target |
|---|---|
| `security.app.akademia.allowed-origins=http://localhost:3000,...` | `...=${AKADEMIA_ALLOWED_ORIGINS:http://localhost:3000,...}` |
| `security.app.elatus.allowed-origins=http://localhost:3001,...` | `...=${ELATUS_ALLOWED_ORIGINS:http://localhost:3001,...}` |

### 2.4 Parameterize IP whitelist CIDRs (HIGH)

| Current | Target |
|---|---|
| `security.akademia.allowed-cidrs=` | `security.akademia.allowed-cidrs=${AKADEMIA_ALLOWED_CIDRS:0.0.0.0/0,::/0}` |

### 2.5 Docker Compose fixes (MEDIUM)

**File**: `docker-compose.dev.yml`

- Expose trust-broker port `8082:8082` to host (enables JWKS debugging)
- Add named volume `mariadb_data` for MariaDB (consistent with Redis named volume)

---

## 3. Acceptance Criteria

- [ ] `application-prod.properties` exists with `ddl-auto=validate`
- [ ] All `${VAR_NAME}` references match ECS task definition env var names
- [ ] Passkey, origins, CIDRs use env vars with localhost defaults
- [ ] `mvn compile -pl application -am` succeeds
- [ ] Existing tests pass
- [ ] trust-broker accessible at `localhost:8082`
- [ ] MariaDB data persists across `docker compose down` (without `-v`)

---

## 4. Risk Assessment

| Change | Risk | Mitigation |
|--------|------|------------|
| application-prod.properties | High — wrong bindings = startup failure | Mirror qa profile exactly, only change ddl-auto/logging |
| Parameterize base properties | Medium — breaks if defaults wrong | Keep current values as defaults |
| Named MariaDB volume | Low — preserves data | Old unnamed volumes unaffected |
