#!/bin/bash
# docker-entrypoint.sh — Trust propagation entrypoint for webapp-service
# Enrolls with the internal CA, builds PKCS12 keystore/truststore, and
# launches the Node.js / Next.js webapp with HTTPS on port 3443.

set -euo pipefail

CERT_DIR="/app/certs"
CA_ENROLL_URL="${CA_ENROLL_URL:-https://ca-service:8082}"
CA_MTLS_URL="${CA_MTLS_URL:-https://ca-service:8081}"
SERVICE_CN="webapp-service"
TOKEN_FILE="/run/secrets/webapp_service_bootstrap_token"
RENEWAL_THRESHOLD_DAYS=30

# ─── Cert file names (akademiaplus-* convention) ─────────────────────────────
CA_CRT="${CERT_DIR}/akademiaplus-ca.crt"
APP_CRT="${CERT_DIR}/akademiaplus-app.crt"
APP_KEY="${CERT_DIR}/akademiaplus-app.key"
APP_CSR="${CERT_DIR}/akademiaplus-app.csr"

mkdir -p "${CERT_DIR}"

# ─── Step 1: Fetch CA root certificate ───────────────────────────────────────
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
        echo "[entrypoint] Cert expires in ${DAYS_LEFT} days. Renewing via mTLS ..."
        openssl req -new -key "${APP_KEY}" \
            -out "${APP_CSR}" \
            -subj "/CN=${SERVICE_CN}/OU=AkademiaPlus/O=ElatusDev"

        CSR_B64=$(base64 -w0 "${APP_CSR}" 2>/dev/null || base64 -i "${APP_CSR}")
        RESPONSE=$(curl -sk -X POST "${CA_MTLS_URL}/ca/sign-cert" \
            --cert   "${APP_CRT}" \
            --key    "${APP_KEY}" \
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
    echo "[entrypoint] No existing cert. Enrolling via bootstrap token ..."

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

# ─── Step 4: Cache JWKS from CA for client-side JWT verification ─────────────
echo "[entrypoint] Fetching JWKS from CA trust broker ..."
curl -sk --retry 5 --retry-delay 3 \
    "${CA_ENROLL_URL}/ca/.well-known/jwks.json" \
    -o "${CERT_DIR}/akademiaplus-jwks.json"
echo "[entrypoint] JWKS cached at ${CERT_DIR}/akademiaplus-jwks.json"

echo "[entrypoint] Trust chain established. Starting webapp-service on port 3443 ..."

# ─── Step 5: Export env vars for the webapp framework ────────────────────────
# Node.js TLS: pass PEM paths (Node reads PEM natively, no PKCS12 needed)
export NODE_TLS_CA_CERT="${CA_CRT}"
export NODE_TLS_CERT="${APP_CRT}"
export NODE_TLS_KEY="${APP_KEY}"
export JWKS_CACHE_PATH="${CERT_DIR}/akademiaplus-jwks.json"
export API_BASE_URL="${API_BASE_URL:-https://platform-core-api:8443/api}"

# ─── Step 6: Launch the webapp ───────────────────────────────────────────────
# exec "$@" delegates to the CMD in the Dockerfile (e.g. node server.js / next start)
exec "$@"
