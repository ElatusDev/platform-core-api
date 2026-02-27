#!/bin/bash
# docker-entrypoint.sh — platform-core-api
#
# Generates a JWT signing keystore on first run, then launches Spring Boot.
# TLS is NOT configured here — transport security is an infrastructure concern
# handled by the orchestrator (reverse proxy in Docker, cert-manager/Istio in K8s).

set -euo pipefail

JWT_KEYSTORE="${JWT_KEYSTORE_PATH:-/app/jwt-keystore.p12}"
JWT_ALIAS="${JWT_KEY_ALIAS:-platform-core-api}"
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
else
    echo "[entrypoint] JWT keystore exists. Skipping generation."
fi

echo "[entrypoint] Starting platform-core-api on port 8080 (plain HTTP)..."

exec java \
    -Djwt.keystore.path="${JWT_KEYSTORE}" \
    -Djwt.keystore.password="${JWT_PASS}" \
    -Djwt.keystore.alias="${JWT_ALIAS}" \
    -jar /app.jar
