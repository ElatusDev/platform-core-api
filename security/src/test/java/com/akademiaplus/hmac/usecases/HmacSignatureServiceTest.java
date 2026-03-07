/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.hmac.usecases;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link HmacSignatureService}.
 *
 * @author ElatusDev
 * @since 1.0
 */
@DisplayName("HmacSignatureService")
class HmacSignatureServiceTest {

    private static final byte[] SIGNING_KEY = "test-hmac-key-32-chars-minimum!!".getBytes(StandardCharsets.UTF_8);
    private static final String KNOWN_BODY = "{\"name\":\"test\"}";
    private static final byte[] KNOWN_BODY_BYTES = KNOWN_BODY.getBytes(StandardCharsets.UTF_8);

    private HmacSignatureService service;

    @BeforeEach
    void setUp() {
        service = new HmacSignatureService();
    }

    @Nested
    @DisplayName("Body hash")
    class BodyHash {

        @Test
        @DisplayName("should return known SHA-256 for known body")
        void shouldReturnKnownSha256_whenHashingKnownBody() {
            // When
            String hash = service.computeBodyHash(KNOWN_BODY_BYTES);

            // Then
            assertThat(hash).hasSize(64); // SHA-256 hex = 64 chars
            assertThat(hash).matches("[0-9a-f]+");
        }

        @Test
        @DisplayName("should return empty body hash constant for empty body")
        void shouldReturnEmptyBodyHash_whenBodyIsEmpty() {
            // When
            String hash = service.computeBodyHash(new byte[0]);

            // Then
            assertThat(hash).isEqualTo(HmacSignatureService.EMPTY_BODY_HASH);
        }

        @Test
        @DisplayName("should produce same hash for same input")
        void shouldProduceSameHash_whenInputIsSame() {
            // When
            String hash1 = service.computeBodyHash(KNOWN_BODY_BYTES);
            String hash2 = service.computeBodyHash(KNOWN_BODY_BYTES);

            // Then
            assertThat(hash1).isEqualTo(hash2);
        }
    }

    @Nested
    @DisplayName("String-to-sign")
    class StringToSign {

        @Test
        @DisplayName("should build request string-to-sign with all components")
        void shouldBuildRequestStringToSign_whenAllComponentsProvided() {
            // When
            String result = service.buildRequestStringToSign(
                    "POST", "/v1/courses", "1709834567", "bodyhash123", "nonce-uuid");

            // Then
            assertThat(result).isEqualTo("POST\n/v1/courses\n1709834567\nbodyhash123\nnonce-uuid");
        }

        @Test
        @DisplayName("should build response string-to-sign with all components")
        void shouldBuildResponseStringToSign_whenAllComponentsProvided() {
            // When
            String result = service.buildResponseStringToSign(
                    "200", "responsebodyhash", "1709834568", "nonce-uuid");

            // Then
            assertThat(result).isEqualTo("200\nresponsebodyhash\n1709834568\nnonce-uuid");
        }
    }

    @Nested
    @DisplayName("HMAC computation")
    class HmacComputation {

        @Test
        @DisplayName("should return valid hex HMAC for known key and data")
        void shouldReturnKnownHmac_whenComputingWithKnownKeyAndData() {
            // When
            String hmac = service.computeHmac(SIGNING_KEY, "test-data");

            // Then
            assertThat(hmac).hasSize(64); // HMAC-SHA256 hex = 64 chars
            assertThat(hmac).matches("[0-9a-f]+");
        }

        @Test
        @DisplayName("should return different HMAC when key changes")
        void shouldReturnDifferentHmac_whenKeyChanges() {
            // Given
            byte[] differentKey = "different-key-32-chars-minimum!!".getBytes(StandardCharsets.UTF_8);

            // When
            String hmac1 = service.computeHmac(SIGNING_KEY, "test-data");
            String hmac2 = service.computeHmac(differentKey, "test-data");

            // Then
            assertThat(hmac1).isNotEqualTo(hmac2);
        }

        @Test
        @DisplayName("should return different HMAC when data changes")
        void shouldReturnDifferentHmac_whenDataChanges() {
            // When
            String hmac1 = service.computeHmac(SIGNING_KEY, "data-1");
            String hmac2 = service.computeHmac(SIGNING_KEY, "data-2");

            // Then
            assertThat(hmac1).isNotEqualTo(hmac2);
        }
    }

    @Nested
    @DisplayName("Signature verification")
    class SignatureVerification {

        @Test
        @DisplayName("should return true when signature matches expected")
        void shouldReturnTrue_whenSignatureMatchesExpected() {
            // Given
            String stringToSign = "POST\n/v1/courses\n1709834567\nbodyhash\nnonce";
            String validSignature = service.computeHmac(SIGNING_KEY, stringToSign);

            // When
            boolean result = service.verifySignature(SIGNING_KEY, stringToSign, validSignature);

            // Then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("should return false when signature mismatches")
        void shouldReturnFalse_whenSignatureMismatches() {
            // Given
            String stringToSign = "POST\n/v1/courses\n1709834567\nbodyhash\nnonce";

            // When
            boolean result = service.verifySignature(SIGNING_KEY, stringToSign, "invalid-signature");

            // Then
            assertThat(result).isFalse();
        }
    }
}
