#!/bin/bash
# docker-entrypoint.sh — etl-service
#
# The etl-service connects to MongoDB (staging) and MariaDB (production loads).
# A JWT keystore is generated because JwtTokenProvider (@Component) loads
# one via @PostConstruct regardless of profile. TLS is an infrastructure concern.

set -euo pipefail

JWT_KEYSTORE="${JWT_KEYSTORE_PATH:-/app/jwt-keystore.p12}"
JWT_ALIAS="${JWT_KEY_ALIAS:-etl-service}"
JWT_PASS="${JWT_KEYSTORE_PASS:-${KEYSTORE_PASS:-changeit}}"

# ─── Generate JWT signing keystore if it doesn't exist ────────────────────────
if [ ! -f "${JWT_KEYSTORE}" ]; then
    echo "[entrypoint] Generating ECDSA P-384 JWT signing keystore..."
    keytool -genkeypair -alias "${JWT_ALIAS}" \
        -keyalg EC -groupname secp384r1 \
        -keystore "${JWT_KEYSTORE}" \
        -storetype PKCS12 \
        -storepass "${JWT_PASS}" \
        -keypass "${JWT_PASS}" \
        -dname "CN=${JWT_ALIAS},OU=AkademiaPlus,O=ElatusDev" \
        -validity 3650
    echo "[entrypoint] JWT keystore created at ${JWT_KEYSTORE}"
fi

MONGO_URI="${MONGODB_URI:-mongodb://localhost:27017/etl_staging}"
echo "[entrypoint] Starting etl-service on port 8280 (plain HTTP)..."
echo "[entrypoint] MongoDB URI: ${MONGO_URI}"

exec java \
    -Dserver.port=8280 \
    -Djwt.keystore.path="${JWT_KEYSTORE}" \
    -Djwt.keystore.password="${JWT_PASS}" \
    -Djwt.keystore.alias="${JWT_ALIAS}" \
    "-Dspring.mongodb.uri=${MONGO_URI}" \
    -jar /app.jar
