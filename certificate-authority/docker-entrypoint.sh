#!/bin/bash
# docker-entrypoint.sh — CA Service bootstrap
#
# WHY THIS EXISTS
# Spring Boot SSL (server.ssl.*) needs the keystore on disk BEFORE Tomcat
# initialises. CertificateAuthorityConfig generates CA material as a Spring
# @Bean, but that runs inside the context — after Tomcat SSL binding.
#
# SOLUTION: generate the CA root key + cert + keystore here with openssl using
# the EXACT same file paths and key alias that CertificateAuthorityConfig expects.
# When Spring starts it detects the files and LOADS instead of regenerating — no
# conflict, identical key material on both boots.
#
# KEY ALIAS: must match CertificateAuthority.CA_KEY_ALIAS = "ca-key"
# FILE PATHS: must match CertificateAuthorityConfig FILE_* constants

set -euo pipefail

CERT_DIR="/certs"
CA_KEY="${CERT_DIR}/akademiaplus-ca.key"
CA_CRT="${CERT_DIR}/akademiaplus-ca.crt"
KEYSTORE="${CERT_DIR}/akademiaplus-ca-keystore.p12"
TRUSTSTORE="${CERT_DIR}/akademiaplus-truststore.p12"
KEY_ALIAS="ca-key"   # Must match CertificateAuthority.CA_KEY_ALIAS

# ─── Generate CA material on first start ──────────────────────────────────────
if [ ! -f "${CA_KEY}" ] || [ ! -f "${CA_CRT}" ]; then
    echo "[ca-entrypoint] Fresh volume — generating ECDSA P-384 root CA..."

    # P-384 key — matches the curve used by CertificateAuthorityConfig
    openssl ecparam -name secp384r1 -genkey -noout -out "${CA_KEY}"
    chmod 600 "${CA_KEY}"

    # Self-signed root cert — subject matches ca.root.subject in properties
    openssl req -new -x509 -days 3650 \
        -key "${CA_KEY}" \
        -out "${CA_CRT}" \
        -subj "/CN=AkademiaPlus CA/OU=AkademiaPlus/O=ElatusDev/L=GDL/ST=JALISCO/C=MX"

    echo "[ca-entrypoint] CA cert generated. Fingerprint:"
    openssl x509 -in "${CA_CRT}" -noout -fingerprint -sha256
fi

# ─── Build PKCS12 keystore if missing ─────────────────────────────────────────
# Alias MUST be "ca-key" — matches CertificateAuthority.CA_KEY_ALIAS.
# CertificateAuthorityConfig will reload (not regenerate) this keystore since
# the source files (akademiaplus-ca.key + .crt) already exist.
if [ ! -f "${KEYSTORE}" ]; then
    echo "[ca-entrypoint] Building keystore (alias: ${KEY_ALIAS})..."
    openssl pkcs12 -export \
        -inkey   "${CA_KEY}" \
        -in      "${CA_CRT}" \
        -name    "${KEY_ALIAS}" \
        -out     "${KEYSTORE}" \
        -passout "pass:${CA_KEYSTORE_PASS}"
fi

# ─── Build truststore if missing ──────────────────────────────────────────────
if [ ! -f "${TRUSTSTORE}" ]; then
    echo "[ca-entrypoint] Building truststore..."
    keytool -importcert -noprompt \
        -alias    akademiaplus-ca \
        -file     "${CA_CRT}" \
        -keystore "${TRUSTSTORE}" \
        -storepass "${CA_KEYSTORE_PASS}" \
        -storetype PKCS12
fi

echo "[ca-entrypoint] CA material ready — launching Spring Boot..."

# ─── Launch Spring Boot ───────────────────────────────────────────────────────
# server.ssl.* at JVM level ensures Tomcat finds the keystore before the Spring
# context initialises — same pattern as all other services in the stack.
# key-alias must match KEY_ALIAS above (= CertificateAuthority.CA_KEY_ALIAS).
exec java \
    -Dspring.profiles.active="${SPRING_PROFILES_ACTIVE:-ca-service}" \
    -Dca.cert-path="${CERT_DIR}" \
    -Dserver.ssl.key-store="${KEYSTORE}" \
    -Dserver.ssl.key-store-type=PKCS12 \
    -Dserver.ssl.key-store-password="${CA_KEYSTORE_PASS}" \
    -Dserver.ssl.key-alias="${KEY_ALIAS}" \
    -Dserver.ssl.client-auth=none \
    -jar /app/ca-service.jar
