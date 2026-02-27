#!/bin/bash
# docker-entrypoint.sh — Trust Broker (formerly CA Service)
#
# CertificateAuthorityConfig generates all CA material (key, cert, keystore,
# truststore) inside the Spring context on first boot. No pre-generation needed
# since there is no Tomcat SSL binding to race against.
#
# TLS is NOT configured here — transport security is an infrastructure concern.

set -euo pipefail

echo "[trust-broker] Starting on port 8082 (plain HTTP)..."

exec java \
    -Dspring.profiles.active="${SPRING_PROFILES_ACTIVE:-ca-service}" \
    -Dca.cert-path="${CA_CERT_PATH:-/certs}" \
    -jar /app/ca-service.jar
