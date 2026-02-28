> [!NOTE]
> **STATUS: COMPLETED / HISTORICAL** — This document is kept for historical reference.
> The work it describes has been implemented or the architecture has been superseded.
> Moved to docs/completed/ on 2026-02-28.

---

# CA Trust Propagation Workflow — AkademiaPlus

> **Governing ADR**: [ADR-0007: Internal PKI with mTLS and Bootstrap Token Enrollment](adr/0007-internal-pki-mtls-bootstrap-enrollment.md)

## Decisions Locked (ADR-0007)

| Parameter | Value |
|---|---|
| Root CA | ECDSA P-384, 10-year validity |
| Leaf certs | RSA-3072, 365-day validity |
| TLS model | mTLS from Phase 1 |
| Enrollment | Bootstrap token (one-time, CN-bound) |
| JWT vs TLS keys | Separate keystores |
| Crypto provider | Bouncy Castle `bcpkix-jdk18on` |

---

## Target Architecture

```
┌──────────────────────────────────────────────────────────────────────┐
│                 Docker Network: akademia-internal                     │
│                       (internal: true)                                │
│                                                                      │
│  ┌───────────────────┐         ┌──────────────────────────────────┐  │
│  │    CA Service      │  mTLS   │       platform-core-api         │  │
│  │    :8081           │◄───────►│       :8443                     │  │
│  │                    │         │                                  │  │
│  │  Root CA key (P384)│         │  TLS cert (RSA-3072, CA-signed) │  │
│  │  Bootstrap tokens  │         │  JWT keystore (RSA-2048, local) │  │
│  │  Serial tracker    │         │  Truststore (CA root)           │  │
│  │  CN allowlist      │         │                                  │  │
│  │                    │         │  server.ssl.client-auth=need     │  │
│  │  Endpoints:        │         │                                  │  │
│  │  POST /ca/enroll   │         └──────────┬─────────────────────┘  │
│  │  POST /ca/sign-cert│                     │                        │
│  │  GET  /ca/ca.crt   │               ports: "8443:8443"            │
│  └────────┬───────────┘            (host-accessible via external)    │
│           │                                 │                        │
│      NO host port                           │                        │
│                                    ┌────────┴────────┐               │
│  ┌────────────┐  ┌────────────┐   │ akademia-external│               │
│  │  MariaDB   │  │   Redis    │   │    (bridge)      │               │
│  │  :3306     │  │   :6379    │   └─────────────────┘               │
│  └────────────┘  └────────────┘                                      │
└──────────────────────────────────────────────────────────────────────┘

Enrollment Flow (one-time per service):
┌─────────┐  1. POST /ca/enroll     ┌────────────┐
│ Service  │  ── token + CSR ──────► │ CA Service  │
│ (no cert)│  ◄── signed cert ───── │             │
│          │  2. Build keystore      │ invalidates │
│          │  3. Restart with mTLS   │   token     │
└─────────┘                          └────────────┘

Renewal Flow (mTLS-authenticated):
┌─────────┐  POST /ca/sign-cert     ┌────────────┐
│ Service  │  ══ mTLS + CSR ═══════► │ CA Service  │
│ (has cert)│ ◄══ signed cert ══════ │             │
└─────────┘                          └────────────┘
```

---

## Phase 1 — Root CA Bootstrap (ECDSA P-384)

**Goal:** CA service generates a self-signed root certificate using ECDSA P-384 on first
startup. On subsequent startups, loads from the persisted volume.

### Deliverables

| File | Purpose |
|---|---|
| `usecases/domain/CertificateAuthority.java` | Holds EC KeyPair + X509Certificate + serial counter |
| `interfaceadapters/config/CertificateAuthorityConfig.java` | `@Configuration` — init/load CA state |
| `interfaceadapters/config/RootCaInitializer.java` | `ApplicationRunner` — generates root CA if absent |
| `application-ca-service.properties` | CA-specific properties |

### Root CA generation (Bouncy Castle)

```java
// 1. Generate ECDSA P-384 key pair
KeyPairGenerator keyGen = KeyPairGenerator.getInstance("EC", "BC");
keyGen.initialize(new ECGenParameterSpec("secp384r1"), new SecureRandom());
KeyPair caKeyPair = keyGen.generateKeyPair();

// 2. Self-sign the root certificate
X500Name issuer = new X500Name("CN=AkademiaPlus CA,OU=AkademiaPlus,O=ElatusDev,L=GDL,ST=JALISCO,C=MX");
BigInteger serial = BigInteger.ONE;  // Root CA is always serial 1
Instant now = Instant.now();
Date notBefore = Date.from(now);
Date notAfter = Date.from(now.plus(Duration.ofDays(3650)));

X509v3CertificateBuilder builder = new JcaX509v3CertificateBuilder(
    issuer, serial, notBefore, notAfter, issuer, caKeyPair.getPublic()
);

builder.addExtension(Extension.basicConstraints, true, new BasicConstraints(true));  // IS a CA
builder.addExtension(Extension.keyUsage, true,
    new KeyUsage(KeyUsage.keyCertSign | KeyUsage.cRLSign));

ContentSigner signer = new JcaContentSignerBuilder("SHA384withECDSA")
    .setProvider("BC")
    .build(caKeyPair.getPrivate());

X509Certificate caCert = new JcaX509CertificateConverter()
    .setProvider("BC")
    .getCertificate(builder.build(signer));
```

**Key detail:** The root CA cert has `BasicConstraints(true)` and `keyUsage = keyCertSign | cRLSign`. This is what makes it a CA certificate — without these, leaf certs signed by it would be rejected by strict TLS implementations.

### Persistence layout on `ca_certs` volume

```
/certs/
├── ca.key          # ECDSA P-384 private key (PEM, chmod 600)
├── ca.crt          # Root CA certificate (PEM)
├── ca-keystore.p12 # CA's own TLS keystore (for serving HTTPS)
├── truststore.p12  # Contains its own root cert (for mTLS verification)
├── serial.txt      # Monotonic serial counter (next serial to issue)
└── tokens.json     # Bootstrap token manifest
```

### pom.xml addition

```xml
<dependency>
    <groupId>org.bouncycastle</groupId>
    <artifactId>bcpkix-jdk18on</artifactId>
    <version>1.78.1</version>
</dependency>
```

---

## Phase 2 — Bootstrap Token System

**Goal:** CA generates CN-bound one-time enrollment tokens at startup. Services use these
tokens to authenticate their initial certificate request.

### Token manifest (`tokens.json`)

```json
{
  "tokens": [
    {
      "token": "base64-encoded-256-bit-random",
      "boundCN": "platform-core-api",
      "issued": "2025-02-21T00:00:00Z",
      "used": false
    },
    {
      "token": "base64-encoded-256-bit-random",
      "boundCN": "notification-service",
      "issued": "2025-02-21T00:00:00Z",
      "used": false
    }
  ]
}
```

### Token generation

```java
// Generate cryptographically random token
byte[] tokenBytes = new byte[32]; // 256 bits
new SecureRandom().nextBytes(tokenBytes);
String token = Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
```

### Deliverables

| File | Purpose |
|---|---|
| `usecases/domain/BootstrapToken.java` | Token record: token, boundCN, issued, used |
| `usecases/domain/TokenManifest.java` | Load/save/validate token manifest |
| `usecases/EnrollServiceUseCase.java` | Validates token → signs CSR → invalidates token |
| `interfaceadapters/controllers/EnrollmentController.java` | `POST /ca/enroll` endpoint |

### Enrollment endpoint (new — not in current OpenAPI spec)

```yaml
# Addition to certificate-authority-module.yaml
/ca/enroll:
  post:
    summary: One-time bootstrap enrollment for new services
    operationId: enrollService
    security: []  # Token-based, not mTLS
    requestBody:
      required: true
      content:
        application/json:
          schema:
            $ref: '#/components/schemas/EnrollmentRequestDTO'
    responses:
      '200':
        description: Enrollment successful
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/CertificateResponseDTO'
      '401':
        description: Invalid or expired bootstrap token
      '403':
        description: Token CN mismatch or token already used
      '400':
        description: Invalid CSR

schemas:
  EnrollmentRequestDTO:
    type: object
    properties:
      bootstrapToken:
        type: string
        description: One-time enrollment token
      commonName:
        type: string
      organization:
        type: string
      certificateSigningRequest:
        type: string
        description: Base64-encoded PKCS#10 CSR
    required:
      - bootstrapToken
      - commonName
      - certificateSigningRequest
```

### Validation rules in `EnrollServiceUseCase`

1. Token exists in manifest and `used == false`
2. `requestDTO.commonName` matches `token.boundCN` (exact match)
3. CSR is valid PKCS#10 format
4. CSR key algorithm is RSA, key size ≥ 3072 bits
5. CSR subject CN matches `requestDTO.commonName`
6. On success: set `token.used = true`, persist manifest, sign CSR

### Token-vs-mTLS endpoint routing

| Endpoint | Auth method | Spring Security config |
|---|---|---|
| `POST /ca/enroll` | Bearer token (bootstrap) | `server.ssl.client-auth` excluded via filter |
| `POST /ca/sign-cert` | mTLS (client cert) | `server.ssl.client-auth=need` |
| `GET /ca/ca.crt` | None (public) | Unauthenticated — serves root cert for trust bootstrap |

The challenge: Spring Boot's `server.ssl.client-auth=need` applies globally. You cannot
have per-endpoint mTLS requirements out of the box. Two solutions:

**Solution A — Dual-port configuration:**
The CA service listens on two ports:
- `:8081` — mTLS enforced (`client-auth=need`), serves `/ca/sign-cert`
- `:8082` — one-way TLS only (`client-auth=none`), serves `/ca/enroll` and `/ca/ca.crt`

This is the cleanest separation. Spring Boot 4 supports additional connectors via
`WebServerFactoryCustomizer<TomcatServletWebServerFactory>`.

**Solution B — `client-auth=want` with programmatic verification:**
Set `server.ssl.client-auth=want` (request but don't require). The `/ca/sign-cert`
controller programmatically checks that a valid client cert was presented. The `/ca/enroll`
controller accepts connections without a client cert and validates the token instead.

**Recommendation: Solution A (dual-port).** It enforces mTLS at the transport layer for
`/ca/sign-cert`, which is stronger than application-level verification. The enrollment
port can be firewalled or removed entirely once all services have enrolled.

---

## Phase 3 — CA Service Implementation (Clean Architecture)

**Goal:** Implement all three endpoints following the project's interfaceadapters/usecases pattern.

### Package structure

```
com.akademiaplus/
├── Main.java                                     (exists — @Profile("ca-service"))
├── interfaceadapters/
│   ├── controllers/
│   │   ├── CertificateAuthorityController.java   POST /ca/sign-cert, GET /ca/ca.crt
│   │   └── EnrollmentController.java             POST /ca/enroll
│   └── config/
│       ├── CertificateAuthorityConfig.java       CA init, Bouncy Castle provider registration
│       ├── RootCaInitializer.java                ApplicationRunner — generate root if absent
│       ├── DualPortConfiguration.java            TomcatServletWebServerFactory customizer
│       └── CaSecurityConfiguration.java          Per-port security rules
└── usecases/
    ├── SignCertificateUseCase.java                mTLS-authenticated CSR signing
    ├── GetCaCertificateUseCase.java               Return PEM-encoded root cert
    ├── EnrollServiceUseCase.java                  Token-validated enrollment
    └── domain/
        ├── CertificateAuthority.java              EC KeyPair + root cert + serial
        ├── BootstrapToken.java                    Token record
        └── TokenManifest.java                     Manifest persistence + validation
```

### CSR signing logic (Bouncy Castle — ECDSA root signs RSA leaf)

```java
public X509Certificate signCsr(PKCS10CertificationRequest csr, CertificateAuthority ca) {
    // 1. Validate CSR
    SubjectPublicKeyInfo csrPubKey = csr.getSubjectPublicKeyInfo();
    RSAKeyParameters rsaKey = (RSAKeyParameters)
        PublicKeyFactory.createKey(csrPubKey);
    if (rsaKey.getModulus().bitLength() < 3072) {
        throw new CsrValidationException("RSA key must be >= 3072 bits");
    }

    // 2. Build certificate
    BigInteger serial = ca.nextSerial();   // Monotonic, persisted
    Instant now = Instant.now();
    X500Name issuer = X500Name.getInstance(ca.getCertificate()
        .getSubjectX500Principal().getEncoded());
    X500Name subject = csr.getSubject();

    X509v3CertificateBuilder builder = new X509v3CertificateBuilder(
        issuer,
        serial,
        Date.from(now),
        Date.from(now.plus(Duration.ofDays(365))),
        subject,
        csrPubKey
    );

    // 3. X.509v3 extensions — critical for mTLS
    builder.addExtension(Extension.basicConstraints, true,
        new BasicConstraints(false));                          // NOT a CA
    builder.addExtension(Extension.keyUsage, true,
        new KeyUsage(KeyUsage.digitalSignature | KeyUsage.keyEncipherment));
    builder.addExtension(Extension.extendedKeyUsage, false,
        new ExtendedKeyUsage(new KeyPurposeId[]{
            KeyPurposeId.id_kp_serverAuth,
            KeyPurposeId.id_kp_clientAuth                     // mTLS
        }));

    // 4. Subject Alternative Names
    String cn = IETFUtils.valueToString(
        subject.getRDNs(BCStyle.CN)[0].getFirst().getValue());
    GeneralNames san = new GeneralNames(new GeneralName[]{
        new GeneralName(GeneralName.dNSName, cn),
        new GeneralName(GeneralName.dNSName, "localhost"),
        new GeneralName(GeneralName.iPAddress, "127.0.0.1")
    });
    builder.addExtension(Extension.subjectAlternativeName, false, san);

    // 5. Sign with ECDSA P-384 CA key (root is EC, leaf is RSA — valid)
    ContentSigner signer = new JcaContentSignerBuilder("SHA384withECDSA")
        .setProvider("BC")
        .build(ca.getPrivateKey());

    return new JcaX509CertificateConverter()
        .setProvider("BC")
        .getCertificate(builder.build(signer));
}
```

**Note:** The signing algorithm is `SHA384withECDSA` because the *CA's* key is ECDSA P-384.
The *leaf's* key type (RSA-3072) is irrelevant to the signing algorithm — the CA signs with
its own key. Verification works because the verifier uses the CA's public key (from the
root cert in the truststore), which is ECDSA P-384.

### CN Allowlist configuration

```properties
# application-ca-service.properties
ca.allowed-common-names=platform-core-api,notification-service,etl-service,audit-service,pos-system
ca.root.subject=CN=AkademiaPlus CA,OU=AkademiaPlus,O=ElatusDev,L=GDL,ST=JALISCO,C=MX
ca.root.validity-days=3650
ca.leaf.validity-days=365
ca.leaf.min-key-size=3072
ca.cert-path=/certs
ca.serial-file=/certs/serial.txt
ca.token-manifest=/certs/tokens.json
```

---

## Phase 4 — Docker Network Isolation + Dual Port

**Goal:** CA service on internal-only network, dual-port for enrollment vs mTLS.

### docker-compose.dev.yml (full replacement)

```yaml
networks:
  akademia-internal:
    driver: bridge
    internal: true
  akademia-external:
    driver: bridge

services:
  ca-service:
    build:
      context: .
      dockerfile: certificate-authority/Dockerfile
    container_name: ca-service
    # NO ports — internal only
    networks:
      - akademia-internal
    volumes:
      - ca_certs:/certs
    environment:
      SPRING_PROFILES_ACTIVE: ca-service
      CA_KEYSTORE_PASS: ${KEYSTORE_PASS}
    secrets:
      - ca_bootstrap_tokens
    healthcheck:
      test: ["CMD", "curl", "-fk", "https://localhost:8082/ca/ca.crt"]
      interval: 10s
      timeout: 5s
      retries: 5
      start_period: 30s

  platform-core-api:
    image: elatusdevops/akademiaplus-platform-core-api:dev
    build:
      context: .
      dockerfile: Dockerfile
    container_name: platform-core-api
    depends_on:
      ca-service:
        condition: service_healthy
    networks:
      - akademia-internal
      - akademia-external
    ports:
      - "8443:8443"
    env_file:
      - .env
    environment:
      CA_ENROLL_URL: https://ca-service:8082
      CA_MTLS_URL: https://ca-service:8081
    volumes:
      - app_certs:/app/certs
    secrets:
      - platform_core_bootstrap_token
    entrypoint: ["/app/docker-entrypoint.sh"]

  multi_tenant_db:
    image: mariadb:latest
    container_name: multi_tenant_db
    restart: always
    networks:
      - akademia-internal
    environment:
      MYSQL_ROOT_PASSWORD: root
      MYSQL_DATABASE: multi_tenant_db
      MYSQL_USER: dev
      MYSQL_PASSWORD: 12345
    ports:
      - "3307:3306"
    volumes:
      - db_data:/var/lib/mysql
      - ./db_init:/docker-entrypoint-initdb.d
    command: >
      --general-log=1
      --general-log-file=/var/lib/mysql/mariadb-general.log
      --slow-query-log=1
      --slow-query-log-file=/var/lib/mysql/mariadb-slow.log
      --long-query-time=2

  platform-core-redis:
    image: redis:6-alpine
    container_name: platform-core-redis
    restart: always
    networks:
      - akademia-internal
    ports:
      - "6379:6379"
    volumes:
      - redis_data:/data

secrets:
  ca_bootstrap_tokens:
    file: ./secrets/ca-bootstrap-tokens.json
  platform_core_bootstrap_token:
    file: ./secrets/platform-core-api-token.txt

volumes:
  db_data: {}
  redis_data: {}
  ca_certs: {}
  app_certs: {}
```

### CA dual-port Tomcat configuration

```java
@Configuration
public class DualPortConfiguration {

    @Value("${ca.mtls.port:8081}")
    private int mtlsPort;

    @Value("${ca.enrollment.port:8082}")
    private int enrollmentPort;

    @Bean
    public WebServerFactoryCustomizer<TomcatServletWebServerFactory> dualPortCustomizer(
            @Value("${ca.cert-path}") String certPath,
            @Value("${CA_KEYSTORE_PASS}") String keystorePass) {

        return factory -> {
            // Primary connector: enrollment port (one-way TLS)
            factory.setPort(enrollmentPort);

            // Additional connector: mTLS port
            factory.addAdditionalTomcatConnectors(
                createMtlsConnector(certPath, keystorePass));
        };
    }

    private Connector createMtlsConnector(String certPath, String keystorePass) {
        Connector connector = new Connector(TomcatServletWebServerFactory.DEFAULT_PROTOCOL);
        connector.setPort(mtlsPort);
        connector.setSecure(true);
        connector.setScheme("https");

        Http11NioProtocol protocol = (Http11NioProtocol) connector.getProtocolHandler();
        protocol.setSSLEnabled(true);
        protocol.setKeystoreFile(certPath + "/ca-keystore.p12");
        protocol.setKeystorePass(keystorePass);
        protocol.setKeystoreType("PKCS12");
        protocol.setTruststoreFile(certPath + "/truststore.p12");
        protocol.setTruststorePass(keystorePass);
        protocol.setTruststoreType("PKCS12");
        protocol.setClientAuth("required");  // mTLS enforced

        return connector;
    }
}
```

---

## Phase 5 — Trust Propagation Entrypoint

**Goal:** `platform-core-api` auto-enrolls via bootstrap token, then starts with full mTLS.

### docker-entrypoint.sh

```bash
#!/bin/bash
set -euo pipefail

CERT_DIR="/app/certs"
CA_ENROLL_URL="${CA_ENROLL_URL:-https://ca-service:8082}"
CA_MTLS_URL="${CA_MTLS_URL:-https://ca-service:8081}"
SERVICE_CN="platform-core-api"
TOKEN_FILE="/run/secrets/platform_core_bootstrap_token"

# ─── Step 1: Fetch CA root certificate (unauthenticated) ───
echo "[entrypoint] Fetching CA root certificate..."
curl -sk --retry 5 --retry-delay 3 \
    "$CA_ENROLL_URL/ca/ca.crt" -o "$CERT_DIR/ca.crt"

echo "[entrypoint] CA root cert retrieved. Fingerprint:"
openssl x509 -in "$CERT_DIR/ca.crt" -noout -fingerprint -sha256

# ─── Step 2: Check if we already have a valid cert ───
if [ -f "$CERT_DIR/app.crt" ]; then
    EXPIRY=$(openssl x509 -in "$CERT_DIR/app.crt" -noout -enddate | cut -d= -f2)
    EXPIRY_EPOCH=$(date -d "$EXPIRY" +%s 2>/dev/null || date -jf "%b %d %T %Y %Z" "$EXPIRY" +%s)
    NOW_EPOCH=$(date +%s)
    DAYS_LEFT=$(( (EXPIRY_EPOCH - NOW_EPOCH) / 86400 ))

    if [ "$DAYS_LEFT" -gt 30 ]; then
        echo "[entrypoint] Existing cert valid for $DAYS_LEFT days. Skipping enrollment."
    else
        echo "[entrypoint] Cert expires in $DAYS_LEFT days. Requesting renewal via mTLS..."
        # Renewal uses mTLS (existing cert as client cert)
        openssl req -new -key "$CERT_DIR/app.key" \
            -out "$CERT_DIR/app.csr" \
            -subj "/CN=$SERVICE_CN/OU=AkademiaPlus/O=ElatusDev"

        CSR_B64=$(base64 -w0 "$CERT_DIR/app.csr" 2>/dev/null || base64 -i "$CERT_DIR/app.csr")
        RESPONSE=$(curl -sk -X POST "$CA_MTLS_URL/ca/sign-cert" \
            --cert "$CERT_DIR/app.crt" \
            --key "$CERT_DIR/app.key" \
            --cacert "$CERT_DIR/ca.crt" \
            -H "Content-Type: application/json" \
            -d "{
                \"commonName\": \"$SERVICE_CN\",
                \"organization\": \"ElatusDev\",
                \"certificateSigningRequest\": \"$CSR_B64\"
            }")

        echo "$RESPONSE" | jq -r '.signedCertificate' | base64 -d > "$CERT_DIR/app.crt"
        echo "[entrypoint] Certificate renewed."
    fi
else
    # ─── Step 3: First-time enrollment via bootstrap token ───
    echo "[entrypoint] No existing cert. Enrolling via bootstrap token..."

    # Generate RSA-3072 keypair + CSR
    openssl req -new -newkey rsa:3072 -nodes \
        -keyout "$CERT_DIR/app.key" \
        -out "$CERT_DIR/app.csr" \
        -subj "/CN=$SERVICE_CN/OU=AkademiaPlus/O=ElatusDev/L=GDL/ST=JALISCO/C=MX"

    chmod 600 "$CERT_DIR/app.key"

    BOOTSTRAP_TOKEN=$(cat "$TOKEN_FILE")
    CSR_B64=$(base64 -w0 "$CERT_DIR/app.csr" 2>/dev/null || base64 -i "$CERT_DIR/app.csr")

    RESPONSE=$(curl -sk -X POST "$CA_ENROLL_URL/ca/enroll" \
        --cacert "$CERT_DIR/ca.crt" \
        -H "Content-Type: application/json" \
        -d "{
            \"bootstrapToken\": \"$BOOTSTRAP_TOKEN\",
            \"commonName\": \"$SERVICE_CN\",
            \"certificateSigningRequest\": \"$CSR_B64\"
        }")

    echo "$RESPONSE" | jq -r '.signedCertificate' | base64 -d > "$CERT_DIR/app.crt"
    echo "$RESPONSE" | jq -r '.caCertificate' | base64 -d > "$CERT_DIR/ca-from-enrollment.crt"

    echo "[entrypoint] Enrollment successful. Serial:"
    openssl x509 -in "$CERT_DIR/app.crt" -noout -serial
fi

# ─── Step 4: Build PKCS12 keystore ───
echo "[entrypoint] Building keystore and truststore..."
openssl pkcs12 -export \
    -in "$CERT_DIR/app.crt" \
    -inkey "$CERT_DIR/app.key" \
    -certfile "$CERT_DIR/ca.crt" \
    -out "$CERT_DIR/keystore.p12" \
    -name "${KEY_ALIAS}" \
    -passout "pass:${KEYSTORE_PASS}"

# ─── Step 5: Build truststore with CA root ───
rm -f "$CERT_DIR/truststore.p12"
keytool -importcert -trustcacerts \
    -keystore "$CERT_DIR/truststore.p12" \
    -storetype PKCS12 \
    -storepass "${KEYSTORE_PASS}" \
    -noprompt -alias akademiaplus-ca \
    -file "$CERT_DIR/ca.crt"

echo "[entrypoint] Trust chain established. Starting platform-core-api with mTLS..."

# ─── Step 6: Launch with mTLS ───
exec java \
    -Dserver.port=8443 \
    -Dserver.ssl.enabled=true \
    -Dserver.ssl.key-store="$CERT_DIR/keystore.p12" \
    -Dserver.ssl.key-store-type=PKCS12 \
    -Dserver.ssl.key-store-password="${KEYSTORE_PASS}" \
    -Dserver.ssl.key-alias="${KEY_ALIAS}" \
    -Dserver.ssl.key-password="${KEYSTORE_PASS}" \
    -Dserver.ssl.client-auth=need \
    -Dserver.ssl.trust-store="$CERT_DIR/truststore.p12" \
    -Dserver.ssl.trust-store-password="${KEYSTORE_PASS}" \
    -Dserver.ssl.trust-store-type=PKCS12 \
    -Djwt.keystore.path="${KEYSTORE_PATH}" \
    -Djwt.keystore.password="${KEYSTORE_PASS}" \
    -Djwt.keystore.alias="${STORE_ALIAS}" \
    -jar /app.jar
```

**Key behaviors:**
- **First run**: No cert exists → enrolls via bootstrap token on port 8082 (one-way TLS)
- **Subsequent runs**: Cert exists and valid → skips enrollment
- **Cert within 30 days of expiry**: Renews via mTLS on port 8081 (uses existing cert as client cert)
- **JWT keystore**: Separate — loaded from `KEYSTORE_PATH` (existing `makani_keystore.p12`)

---

## Phase 6 — Dockerfile Fixes

### certificate-authority/Dockerfile

```dockerfile
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /workspace

# Dependency caching layer
COPY pom.xml ./
COPY certificate-authority/pom.xml ./certificate-authority/
RUN apk add --no-cache maven && \
    mvn -pl certificate-authority -am dependency:go-offline -q || true

# Build
COPY certificate-authority/src ./certificate-authority/src
RUN mvn -pl certificate-authority -am clean package -DskipTests -q

# Runtime — non-root user
FROM eclipse-temurin:21-jre-alpine
RUN apk update && apk add --no-cache bash curl openssl && \
    addgroup -S cagroup && adduser -S causer -G cagroup

WORKDIR /app
COPY --from=build /workspace/certificate-authority/target/certificate-authority-*.jar \
    /app/ca-service.jar

RUN mkdir -p /certs && chown causer:cagroup /certs
USER causer

EXPOSE 8081 8082
ENTRYPOINT ["java", "-jar", "/app/ca-service.jar"]
```

### platform-core-api Dockerfile addition (entrypoint)

Add to existing `Dockerfile` before the final `CMD`:
```dockerfile
# Trust propagation entrypoint
COPY docker-entrypoint.sh /app/docker-entrypoint.sh
RUN chmod +x /app/docker-entrypoint.sh
RUN apk add --no-cache curl jq openssl
ENTRYPOINT ["/app/docker-entrypoint.sh"]
```

---

## Phase 7 — Testing Strategy

### Unit tests (Given-When-Then, per ADR-0004)

| Test class | Key test methods |
|---|---|
| `CertificateAuthorityTest` | `shouldGenerateEcdsaP384RootCa_whenNoCertExists()` |
| | `shouldLoadExistingCa_whenCertFilePresent()` |
| | `shouldIncrementSerialNumber_whenCertIssued()` |
| `SignCertificateUseCaseTest` | `shouldSignCsr_whenGivenValidRsa3072Request()` |
| | `shouldRejectCsr_whenKeySize2048()` |
| | `shouldRejectCsr_whenCnNotInAllowlist()` |
| | `shouldSetBasicConstraintsFalse_whenSigningLeafCert()` |
| | `shouldIncludeClientAuthEku_whenSigningForMtls()` |
| | `shouldIncludeSan_whenSigningCert()` |
| `EnrollServiceUseCaseTest` | `shouldSignCsr_whenGivenValidBootstrapToken()` |
| | `shouldRejectEnrollment_whenTokenAlreadyUsed()` |
| | `shouldRejectEnrollment_whenCnDoesNotMatchTokenBoundCn()` |
| | `shouldInvalidateToken_whenEnrollmentSucceeds()` |
| `TokenManifestTest` | `shouldLoadTokensFromJson_whenManifestExists()` |
| | `shouldPersistTokenState_whenTokenInvalidated()` |
| `GetCaCertificateUseCaseTest` | `shouldReturnPemEncodedCaCert_whenCaInitialized()` |

### Component tests (Testcontainers)

| Test | What it validates |
|---|---|
| CA round-trip | Start CA → enroll with token → receive signed cert → verify cert chain |
| mTLS enforcement | CA rejects `/ca/sign-cert` without client cert → returns TLS handshake failure |
| Token single-use | Enroll once → attempt second enrollment with same token → 403 |
| Key size enforcement | Submit CSR with RSA-2048 → 400 rejection |
| CN mismatch | Token bound to `service-a`, CSR has CN `service-b` → 403 |
| Cert validation | CA-signed cert validates against CA root cert in truststore |
| SAN verification | Signed cert contains expected DNS and IP SANs |
| Renewal via mTLS | After enrollment, renew using `/ca/sign-cert` with client cert |

### Integration smoke test (docker-compose up)

1. `docker compose -f docker-compose.dev.yml up -d`
2. Verify CA healthcheck passes (port 8082 internal)
3. Verify `platform-core-api` started and enrolled (check logs for `[entrypoint] Enrollment successful`)
4. `curl -k https://localhost:8443/actuator/health` — API reachable from host
5. Verify CA NOT reachable from host: `curl https://localhost:8081` → connection refused
6. Verify mTLS: `curl -k https://localhost:8443/some-endpoint` without client cert → TLS handshake failure

---

## Task Sequencing

```
Phase 1: Root CA Bootstrap (ECDSA P-384)
    │
    ├──► Phase 2: Bootstrap Token System
    │        │
    │        └──► Phase 3: CA Service Implementation
    │                 │
    │                 ├──► Phase 6: Dockerfile Fixes
    │                 │        │
    │                 │        └──► Phase 4: Docker Network + Dual Port
    │                 │                 │
    │                 │                 └──► Phase 5: Trust Propagation Entrypoint
    │                 │
    │                 └──► Phase 7: Tests (parallel with Phase 6)
    │
    └──► Phase 7: Domain unit tests (parallel with Phase 2)
```

---

## Complete File Manifest

### New files

| # | Path | Phase |
|---|---|---|
| 1 | `certificate-authority/src/.../usecases/domain/CertificateAuthority.java` | 1 |
| 2 | `certificate-authority/src/.../usecases/domain/BootstrapToken.java` | 2 |
| 3 | `certificate-authority/src/.../usecases/domain/TokenManifest.java` | 2 |
| 4 | `certificate-authority/src/.../usecases/SignCertificateUseCase.java` | 3 |
| 5 | `certificate-authority/src/.../usecases/GetCaCertificateUseCase.java` | 3 |
| 6 | `certificate-authority/src/.../usecases/EnrollServiceUseCase.java` | 3 |
| 7 | `certificate-authority/src/.../interfaceadapters/controllers/CertificateAuthorityController.java` | 3 |
| 8 | `certificate-authority/src/.../interfaceadapters/controllers/EnrollmentController.java` | 3 |
| 9 | `certificate-authority/src/.../interfaceadapters/config/CertificateAuthorityConfig.java` | 1 |
| 10 | `certificate-authority/src/.../interfaceadapters/config/RootCaInitializer.java` | 1 |
| 11 | `certificate-authority/src/.../interfaceadapters/config/DualPortConfiguration.java` | 4 |
| 12 | `certificate-authority/src/.../interfaceadapters/config/CaSecurityConfiguration.java` | 4 |
| 13 | `certificate-authority/src/main/resources/application-ca-service.properties` | 1 |
| 14 | `docker-entrypoint.sh` | 5 |
| 15 | `secrets/ca-bootstrap-tokens.json` | 2 |
| 16 | `secrets/platform-core-api-token.txt` | 2 |
| 17 | `.gitignore` addition: `secrets/` | 2 |

### New test files

| # | Path | Phase |
|---|---|---|
| 18 | `certificate-authority/src/test/.../usecases/domain/CertificateAuthorityTest.java` | 7 |
| 19 | `certificate-authority/src/test/.../usecases/domain/TokenManifestTest.java` | 7 |
| 20 | `certificate-authority/src/test/.../usecases/SignCertificateUseCaseTest.java` | 7 |
| 21 | `certificate-authority/src/test/.../usecases/GetCaCertificateUseCaseTest.java` | 7 |
| 22 | `certificate-authority/src/test/.../usecases/EnrollServiceUseCaseTest.java` | 7 |

### Modified files

| # | Path | Change | Phase |
|---|---|---|---|
| 23 | `certificate-authority/pom.xml` | Add Bouncy Castle dependency | 1 |
| 24 | `certificate-authority/Dockerfile` | Fix paths, non-root user, dual EXPOSE | 6 |
| 25 | `certificate-authority/src/main/resources/openapi/certificate-authority-module.yaml` | Add `/ca/enroll` endpoint + `EnrollmentRequestDTO` | 2 |
| 26 | `docker-compose.dev.yml` | Networks, secrets, dual-port CA, entrypoint | 4 |
| 27 | `Dockerfile` (platform-core-api) | Add entrypoint + curl/jq/openssl | 6 |
| 28 | `.env` | Add `CA_ENROLL_URL`, `CA_MTLS_URL` | 4 |
| 29 | `.gitignore` | Add `secrets/` directory | 2 |

---

## Secrets Migration (Critical)

| Secret | Current location | Target | Priority |
|---|---|---|---|
| `KEYSTORE_PASS` | `.env` plaintext | Docker secret | Phase 4 |
| `KEY_PASS` | `.env` plaintext | Docker secret | Phase 4 |
| `GITHUB_TOKEN` | `.env` plaintext | **ROTATE NOW** — remove from repo | Immediate |
| `MP_ACCESS_TOKEN` | `.env` plaintext | Docker secret | Phase 4 |
| `ENCRYPTION_KEY` | `.env` plaintext | Docker secret | Phase 4 |
| Bootstrap tokens | `secrets/` directory | Docker secret (already) | Phase 2 |
| CA private key | `ca_certs` volume | Volume + chmod 600 + non-root user | Phase 1 |
