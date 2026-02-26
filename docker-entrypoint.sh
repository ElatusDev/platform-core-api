#!/bin/bash
# docker-entrypoint.sh — Trust propagation entrypoint for platform-core-api
# Fetches the CA root cert, enrolls (or renews) a TLS certificate, builds
# PKCS12 keystore/truststore, registers JWT public key with the CA JWKS registry,
# and launches the Spring Boot application with mTLS.

set -euo pipefail

CERT_DIR="/app/certs"
CA_ENROLL_URL="${CA_ENROLL_URL:-https://ca-service:8082}"
CA_MTLS_URL="${CA_MTLS_URL:-https://ca-service:8081}"
SERVICE_CN="platform-core-api"
TOKEN_FILE="/run/secrets/platform_core_bootstrap_token"
RENEWAL_THRESHOLD_DAYS=30

# ─── Cert file names (akademiaplus-* convention) ─────────────────────────────
CA_CRT="${CERT_DIR}/akademiaplus-ca.crt"
APP_CRT="${CERT_DIR}/akademiaplus-app.crt"
APP_KEY="${CERT_DIR}/akademiaplus-app.key"
APP_CSR="${CERT_DIR}/akademiaplus-app.csr"
KEYSTORE="${CERT_DIR}/akademiaplus-keystore.p12"
TRUSTSTORE="${CERT_DIR}/akademiaplus-truststore.p12"

mkdir -p "${CERT_DIR}"

# ─── Step 1: Fetch CA root certificate (unauthenticated, public endpoint) ────
echo "[entrypoint] Fetching CA root certificate from ${CA_ENROLL_URL}/ca/ca.crt ..."
curl -sk --retry 5 --retry-delay 3 \
    "${CA_ENROLL_URL}/ca/ca.crt" -o "${CA_CRT}"

echo "[entrypoint] CA root cert retrieved. Fingerprint:"
openssl x509 -in "${CA_CRT}" -noout -fingerprint -sha256

# ─── Step 2: Decide enrollment vs renewal vs skip ────────────────────────────
if [ -f "${APP_CRT}" ]; then
    EXPIRY=$(openssl x509 -in "${APP_CRT}" -noout -enddate | cut -d= -f2)
    EXPIRY_EPOCH=$(date -d "${EXPIRY}" +%s 2>/dev/null || date -jf "%b %d %T %Y %Z" "${EXPIRY}" +%s)
    NOW_EPOCH=$(date +%s)
    DAYS_LEFT=$(( (EXPIRY_EPOCH - NOW_EPOCH) / 86400 ))

    if [ "${DAYS_LEFT}" -gt "${RENEWAL_THRESHOLD_DAYS}" ]; then
        echo "[entrypoint] Existing cert valid for ${DAYS_LEFT} days. Skipping enrollment."
    else
        echo "[entrypoint] Cert expires in ${DAYS_LEFT} days. Renewing via mTLS (${CA_MTLS_URL}) ..."
        openssl req -new -key "${APP_KEY}" \
            -out "${APP_CSR}" \
            -subj "/CN=${SERVICE_CN}/OU=AkademiaPlus/O=ElatusDev"

        CSR_B64=$(base64 -w0 "${APP_CSR}" 2>/dev/null || base64 -i "${APP_CSR}")
        RESPONSE=$(curl -sk -X POST "${CA_MTLS_URL}/ca/sign-cert" \
            --cert "${APP_CRT}" \
            --key  "${APP_KEY}" \
            --cacert "${CA_CRT}" \
            -H "Content-Type: application/json" \
            -d "{
                \"commonName\": \"${SERVICE_CN}\",
                \"organization\": \"ElatusDev\",
                \"certificateSigningRequest\": \"${CSR_B64}\"
            }")

        echo "${RESPONSE}" | jq -r '.signedCertificate' | base64 -d > "${APP_CRT}"
        echo "[entrypoint] Certificate renewed."
    fi
else
    # ─── Step 3: First-time enrollment via bootstrap token ───────────────────
    echo "[entrypoint] No existing cert. Enrolling via bootstrap token (${CA_ENROLL_URL}/ca/enroll) ..."

    openssl req -new -newkey rsa:3072 -nodes \
        -keyout "${APP_KEY}" \
        -out    "${APP_CSR}" \
        -subj "/CN=${SERVICE_CN}/OU=AkademiaPlus/O=ElatusDev/L=GDL/ST=JALISCO/C=MX"

    chmod 600 "${APP_KEY}"

    BOOTSTRAP_TOKEN=$(cat "${TOKEN_FILE}")
    CSR_B64=$(base64 -w0 "${APP_CSR}" 2>/dev/null || base64 -i "${APP_CSR}")

    RESPONSE=$(curl -sk -X POST "${CA_ENROLL_URL}/ca/enroll" \
        --cacert "${CA_CRT}" \
        -H "Content-Type: application/json" \
        -d "{
            \"bootstrapToken\": \"${BOOTSTRAP_TOKEN}\",
            \"commonName\": \"${SERVICE_CN}\",
            \"certificateSigningRequest\": \"${CSR_B64}\"
        }")

    echo "${RESPONSE}" | jq -r '.signedCertificate' | base64 -d > "${APP_CRT}"

    echo "[entrypoint] Enrollment successful. Serial:"
    openssl x509 -in "${APP_CRT}" -noout -serial
fi

# ─── Step 4: Build PKCS12 keystore (used for both mTLS and JWT signing) ──────
echo "[entrypoint] Building akademiaplus-keystore.p12 ..."
openssl pkcs12 -export \
    -in      "${APP_CRT}" \
    -inkey   "${APP_KEY}" \
    -certfile "${CA_CRT}" \
    -out     "${KEYSTORE}" \
    -name    "${STORE_ALIAS}" \
    -passout "pass:${KEYSTORE_PASS}"

# ─── Step 5: Build truststore with CA root ───────────────────────────────────
echo "[entrypoint] Building akademiaplus-truststore.p12 ..."
rm -f "${TRUSTSTORE}"
keytool -importcert -trustcacerts \
    -keystore "${TRUSTSTORE}" \
    -storetype PKCS12 \
    -storepass "${KEYSTORE_PASS}" \
    -noprompt -alias akademiaplus-ca \
    -file "${CA_CRT}"

echo "[entrypoint] Trust chain established. Starting platform-core-api with mTLS on port 8443 ..."

# ─── Step 6: Launch the application ──────────────────────────────────────────
# JWT uses the same enrolled keystore — no separate JWT_KEYSTORE_* env vars needed.
exec java \
    -Dserver.port=8443 \
    -Dserver.ssl.enabled=true \
    -Dserver.ssl.key-store="file:${KEYSTORE}" \
    -Dserver.ssl.key-store-type=PKCS12 \
    -Dserver.ssl.key-store-password="${KEYSTORE_PASS}" \
    -Dserver.ssl.key-alias="${STORE_ALIAS}" \
    -Dserver.ssl.key-password="${KEYSTORE_PASS}" \
    -Dserver.ssl.client-auth=need \
    -Dserver.ssl.trust-store="file:${TRUSTSTORE}" \
    -Dserver.ssl.trust-store-password="${KEYSTORE_PASS}" \
    -Dserver.ssl.trust-store-type=PKCS12 \
    -Djwt.keystore.path="${KEYSTORE}" \
    -Djwt.keystore.password="${KEYSTORE_PASS}" \
    -Djwt.keystore.alias="${STORE_ALIAS}" \
    -jar /app.jar
