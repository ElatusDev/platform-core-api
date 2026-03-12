# Production Readiness — Claude Code Execution Prompt

> **Workflow**: [`production-readiness-workflow.md`](../../workflows/pending/production-readiness-workflow.md)
> **Project**: core-api

---

## EXECUTION RULES

1. **Read every file before modifying it**
2. Verify after each step: `mvn compile -pl application -am -DskipTests -q`
3. Do NOT add AI attribution comments in code

---

## Step 1 — Create `application-prod.properties`

### Read first

```bash
cat application/src/main/resources/application.properties
cat application/src/main/resources/application-qa.properties
```

### Create file

**File**: `application/src/main/resources/application-prod.properties`

```properties
spring.datasource.url=${SPRING_DATASOURCE_URL}
spring.datasource.driver-class-name=org.mariadb.jdbc.Driver
spring.jpa.database-platform=org.hibernate.dialect.MariaDBDialect
spring.datasource.username=${SPRING_DATASOURCE_USERNAME}
spring.datasource.password=${SPRING_DATASOURCE_PASSWORD}

spring.datasource.hikari.maximum-pool-size=10
spring.datasource.hikari.minimum-idle=5
spring.datasource.hikari.connection-timeout=30000
spring.datasource.hikari.idle-timeout=600000
spring.datasource.hikari.max-lifetime=1800000

spring.jpa.hibernate.ddl-auto=validate
spring.jpa.show-sql=false
spring.jpa.properties.hibernate.format_sql=false

spring.http.encoding.charset=UTF-8
spring.http.encoding.enabled=true
spring.http.encoding.force=true

# Redis (ElastiCache)
spring.data.redis.host=${SPRING_DATA_REDIS_HOST}
spring.data.redis.port=${SPRING_DATA_REDIS_PORT:6379}
spring.data.redis.ssl.enabled=${SPRING_DATA_REDIS_SSL_ENABLED:true}

jwt.keystore.path=${JWT_KEYSTORE_PATH}
jwt.keystore.password=${JWT_KEYSTORE_PASSWORD}
jwt.keystore.alias=${JWT_STORE_ALIAS}
jwt.token.validity-ms=900000

logging.level.root=INFO
logging.level.com.akademiaplus=INFO

server.port=8080
server.servlet.context-path=/api
spring.application.name=PlatformCore Prod
management.endpoints.web.exposure.include=health,info,prometheus

# Health probes for ECS health checks
management.endpoint.health.probes.enabled=true
management.health.livenessstate.enabled=true
management.health.readinessstate.enabled=true

# Structured JSON logging for production (ECS format)
logging.structured.format.console=ecs
```

---

## Step 2 — Parameterize security properties in base `application.properties`

### Read first

```bash
cat application/src/main/resources/application.properties
```

### Passkey properties

```properties
# BEFORE
security.passkey.rp-id=localhost
security.passkey.rp-name=AkademiaPlus Dev
security.passkey.allowed-origins[0]=http://localhost:3000

# AFTER
security.passkey.rp-id=${PASSKEY_RP_ID:localhost}
security.passkey.rp-name=${PASSKEY_RP_NAME:AkademiaPlus Dev}
security.passkey.allowed-origins[0]=${PASSKEY_ALLOWED_ORIGIN:http://localhost:3000}
```

### Per-app allowed-origins

```properties
# BEFORE
security.app.akademia.allowed-origins=http://localhost:3000,https://localhost:3000
security.app.elatus.allowed-origins=http://localhost:3001,https://localhost:3001

# AFTER
security.app.akademia.allowed-origins=${AKADEMIA_ALLOWED_ORIGINS:http://localhost:3000,https://localhost:3000}
security.app.elatus.allowed-origins=${ELATUS_ALLOWED_ORIGINS:http://localhost:3001,https://localhost:3001}
```

### IP whitelist CIDRs

```properties
# BEFORE
security.akademia.allowed-cidrs=

# AFTER
security.akademia.allowed-cidrs=${AKADEMIA_ALLOWED_CIDRS:0.0.0.0/0,::/0}
```

### Verify

```bash
mvn compile -pl application -am -DskipTests -q
mvn test -pl application -am
```

---

## Step 3 — Docker Compose fixes

### Read first

```bash
cat docker-compose.dev.yml
```

### Expose trust-broker port

Add to trust-broker service:
```yaml
ports:
  - "8082:8082"
```

### Named MariaDB volume

Add to volumes section:
```yaml
volumes:
  redis_data: {}
  trust_broker_data: {}
  mariadb_data: {}
```

Update multi_tenant_db service:
```yaml
volumes:
  - mariadb_data:/var/lib/mysql
  - ./db_init:/docker-entrypoint-initdb.d
```

### Verify

```bash
docker compose -f docker-compose.dev.yml config --quiet
```

---

## Commit

```
feat(config): add production Spring profile and parameterize security properties

Create application-prod.properties with validate DDL mode, INFO logging,
and Redis TLS. Parameterize passkey RP config, per-app CORS origins, and
IP whitelist CIDRs with env var bindings and localhost defaults. Expose
trust-broker port and add named MariaDB volume in Docker Compose.
```
