#!/bin/bash
# docker-entrypoint.sh — Trust propagation entrypoint for platform-core-api
# Fetches the CA root cert, enrolls (or renews) a TLS certificate, builds
# PKCS12 keystore/truststore, and launches the Spring Boot application with mTLS.

set -euo pipefail

CERT_DIR="/app/certs"
CA_ENROLL_URL="${CA_ENROLL_URL:-https://ca-service:8082}"
CA_MTLS_URL="${CA_MTLS_URL:-https://ca-service:8081}"
SERVICE_CN="platform-core-api"
TOKEN_FILE="/run/secrets/platform_core_bootstrap_token"
RENEWAL_THRESHOLD_DAYS=30

mkdir -p "${CERT_DIR}"

# ─── Step 1: Fetch CA root certificate (unauthenticated, public endpoint) ───
echo "[entrypoint] Fetching CA root certificate from ${CA_ENROLL_URL}/ca/ca.crt ..."
curl -sk --retry 5 --retry-delay 3 \
    "${CA_ENROLL_URL}/ca/ca.crt" -o "${CERT_DIR}/ca.crt"

echo "[entrypoint] CA root cert retrieved. Fingerprint:"
openssl x509 -in "${CERT_DIR}/ca.crt" -noout -fingerprint -sha256

# ─── Step 2: Decide enrollment vs renewal vs skip ───
if [ -f "${CERT_DIR}/app.crt" ]; then
    EXPIRY=$(openssl x509 -in "${CERT_DIR}/app.crt" -noout -enddate | cut -d= -f2)
    # macOS-compatible date parsing (supports both GNU and BSD date)
    EXPIRY_EPOCH=$(date -d "${EXPIRY}" +%s 2>/dev/null || date -jf "%b %d %T %Y %Z" "${EXPIRY}" +%s)
    NOW_EPOCH=$(date +%s)
    DAYS_LEFT=$(( (EXPIRY_EPOCH - NOW_EPOCH) / 86400 ))

    if [ "${DAYS_LEFT}" -gt "${RENEWAL_THRESHOLD_DAYS}" ]; then
        echo "[entrypoint] Existing cert valid for ${DAYS_LEFT} days. Skipping enrollment."
    else
        echo "[entrypoint] Cert expires in ${DAYS_LEFT} days. Renewing via mTLS (${CA_MTLS_URL}) ..."
        openssl req -new -key "${CERT_DIR}/app.key" \
            -out "${CERT_DIR}/app.csr" \
            -subj "/CN=${SERVICE_CN}/OU=AkademiaPlus/O=ElatusDev"

        CSR_B64=$(base64 -w0 "${CERT_DIR}/app.csr" 2>/dev/null || base64 -i "${CERT_DIR}/app.csr")
        RESPONSE=$(curl -sk -X POST "${CA_MTLS_URL}/ca/sign-cert" \
            --cert "${CERT_DIR}/app.crt" \
            --key "${CERT_DIR}/app.key" \
            --cacert "${CERT_DIR}/ca.crt" \
            -H "Content-Type: application/json" \
            -d "{
                \"commonName\": \"${SERVICE_CN}\",
                \"organization\": \"ElatusDev\",
                \"certificateSigningRequest\": \"${CSR_B64}\"
            }")

        echo "${RESPONSE}" | jq -r '.signedCertificate' | base64 -d > "${CERT_DIR}/app.crt"
        echo "[entrypoint] Certificate renewed."
    fi
else
    # ─── Step 3: First-time enrollment via bootstrap token ───
    echo "[entrypoint] No existing cert. Enrolling via bootstrap token (${CA_ENROLL_URL}/ca/enroll) ..."

    openssl req -new -newkey rsa:3072 -nodes \
        -keyout "${CERT_DIR}/app.key" \
        -out "${CERT_DIR}/app.csr" \
        -subj "/CN=${SERVICE_CN}/OU=AkademiaPlus/O=ElatusDev/L=GDL/ST=JALISCO/C=MX"

    chmod 600 "${CERT_DIR}/app.key"

    BOOTSTRAP_TOKEN=$(cat "${TOKEN_FILE}")
    CSR_B64=$(base64 -w0 "${CERT_DIR}/app.csr" 2>/dev/null || base64 -i "${CERT_DIR}/app.csr")

    RESPONSE=$(curl -sk -X POST "${CA_ENROLL_URL}/ca/enroll" \
        --cacert "${CERT_DIR}/ca.crt" \
        -H "Content-Type: application/json" \
        -d "{
            \"bootstrapToken\": \"${BOOTSTRAP_TOKEN}\",
            \"commonName\": \"${SERVICE_CN}\",
            \"certificateSigningRequest\": \"${CSR_B64}\"
        }")

    echo "${RESPONSE}" | jq -r '.signedCertificate' | base64 -d > "${CERT_DIR}/app.crt"

    echo "[entrypoint] Enrollment successful. Serial:"
    openssl x509 -in "${CERT_DIR}/app.crt" -noout -serial
fi

# ─── Step 4: Build PKCS12 keystore ───
echo "[entrypoint] Building keystore ..."
openssl pkcs12 -export \
    -in "${CERT_DIR}/app.crt" \
    -inkey "${CERT_DIR}/app.key" \
    -certfile "${CERT_DIR}/ca.crt" \
    -out "${CERT_DIR}/keystore.p12" \
    -name "${KEY_ALIAS}" \
    -passout "pass:${KEYSTORE_PASS}"

# ─── Step 5: Build truststore with CA root ───
echo "[entrypoint] Building truststore ..."
rm -f "${CERT_DIR}/truststore.p12"
keytool -importcert -trustcacerts \
    -keystore "${CERT_DIR}/truststore.p12" \
    -storetype PKCS12 \
    -storepass "${KEYSTORE_PASS}" \
    -noprompt -alias akademiaplus-ca \
    -file "${CERT_DIR}/ca.crt"

echo "[entrypoint] Trust chain established. Starting platform-core-api with mTLS on port 8443 ..."

# ─── Step 6: Launch the application with mTLS ───
exec java \
    -Dserver.port=8443 \
    -Dserver.ssl.enabled=true \
    -Dserver.ssl.key-store="${CERT_DIR}/keystore.p12" \
    -Dserver.ssl.key-store-type=PKCS12 \
    -Dserver.ssl.key-store-password="${KEYSTORE_PASS}" \
    -Dserver.ssl.key-alias="${KEY_ALIAS}" \
    -Dserver.ssl.key-password="${KEYSTORE_PASS}" \
    -Dserver.ssl.client-auth=need \
    -Dserver.ssl.trust-store="${CERT_DIR}/truststore.p12" \
    -Dserver.ssl.trust-store-password="${KEYSTORE_PASS}" \
    -Dserver.ssl.trust-store-type=PKCS12 \
    -Djwt.keystore.path="${KEYSTORE_PATH}" \
    -Djwt.keystore.password="${KEYSTORE_PASS}" \
    -Djwt.keystore.alias="${STORE_ALIAS}" \
    -jar /app.jar
