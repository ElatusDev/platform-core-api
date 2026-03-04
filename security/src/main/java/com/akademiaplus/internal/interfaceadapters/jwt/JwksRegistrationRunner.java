/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.internal.interfaceadapters.jwt;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.interfaces.ECPublicKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Base64;

/**
 * Registers this service's JWT signing public key with the Trust Broker JWKS
 * registry on application startup.
 *
 * <p>Runs after Spring context is fully initialized. Uses plain HTTP to the
 * trust-broker service on the isolated {@code akademia-internal} Docker network.
 * Transport security (TLS) is an infrastructure concern handled by the
 * orchestrator (reverse proxy in Docker, cert-manager/Istio in Kubernetes).
 *
 * <p>Only active on the {@code dev} profile. Local development without Docker
 * skips registration gracefully.
 */
@Slf4j
@Component
@Profile("dev")
@RequiredArgsConstructor
public class JwksRegistrationRunner implements ApplicationRunner {

    private final JwtTokenProvider jwtTokenProvider;

    @Value("${trust-broker.url:http://trust-broker:8082}")
    private String trustBrokerUrl;

    /**
     * Determines the JWS algorithm string from the loaded public key type.
     * EC P-384 → ES384, EC P-256 → ES256, RSA → RS256.
     */
    private String resolveAlgorithm() {
        var publicKey = jwtTokenProvider.getKeyPair().getPublic();
        if (publicKey instanceof ECPublicKey ecKey) {
            int bitLength = ecKey.getParams().getOrder().bitLength();
            return switch (bitLength) {
                case 256 -> "ES256";
                case 384 -> "ES384";
                case 521 -> "ES512";
                default  -> "ES384";
            };
        }
        if (publicKey instanceof RSAPublicKey rsaKey) {
            return rsaKey.getModulus().bitLength() >= 3072 ? "RS384" : "RS256";
        }
        return "ES384";
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            String alg = resolveAlgorithm();
            byte[] derBytes = jwtTokenProvider.getKeyPair().getPublic().getEncoded();
            String publicKeyBase64 = Base64.getEncoder().encodeToString(derBytes);
            String kid = jwtTokenProvider.getServiceId();

            String body = """
                    {"kid":"%s","alg":"%s","publicKeyBase64":"%s"}
                    """.formatted(kid, alg, publicKeyBase64).strip();

            HttpClient client = HttpClient.newHttpClient();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(trustBrokerUrl + "/ca/jwks/register"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 204) {
                log.info("JWKS public key registered with trust broker. kid={} alg={}", kid, alg);
            } else {
                log.warn("JWKS registration returned unexpected status {}: {}", response.statusCode(), response.body());
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("JWKS registration interrupted: {}", e.getMessage());
        } catch (Exception e) {
            // Non-fatal — trust broker may not be available in all environments
            log.warn("JWKS registration failed (non-fatal): {}", e.getMessage());
        }
    }
}
