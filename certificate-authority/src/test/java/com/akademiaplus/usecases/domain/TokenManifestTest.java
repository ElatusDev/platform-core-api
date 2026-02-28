/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.usecases.domain;

import com.akademiaplus.usecases.exceptions.InvalidBootstrapTokenException;
import com.akademiaplus.usecases.exceptions.TokenAlreadyUsedException;
import com.akademiaplus.usecases.exceptions.TokenCnMismatchException;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("TokenManifest Domain Tests")
class TokenManifestTest {

    private static final String BOUND_CN_PLATFORM = "platform-core-api";
    private static final String TOKEN_PLATFORM = "valid-token-abc";
    private static final String TOKEN_UNKNOWN = "unknown-token-xyz";
    private static final String DIFFERENT_CN = "notification-service";
    private static final String MANIFEST_FILE_NAME = "tokens.json";
    private static final String ISSUED_TIMESTAMP = "2026-02-21T00:00:00Z";

    @TempDir
    Path tempDir;

    private ObjectMapper objectMapper;
    private Path manifestFile;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        manifestFile = tempDir.resolve(MANIFEST_FILE_NAME);
    }

    private TokenManifest buildManifestWithToken(String token, String cn, boolean used) {
        TokenManifest manifest = new TokenManifest(manifestFile, objectMapper);
        manifest.getTokens().add(new BootstrapToken(token, cn, ISSUED_TIMESTAMP, used));
        return manifest;
    }

    @Nested
    @DisplayName("Token Loading Tests")
    class TokenLoadingTests {

        @Test
        @DisplayName("Should load tokens from JSON when manifest file exists")
        void shouldLoadTokensFromJson_whenManifestExists() throws Exception {
            // Given
            String json = """
                    {
                      "tokens": [
                        {"token": "%s", "boundCN": "%s", "issued": "%s", "used": false}
                      ]
                    }
                    """.formatted(TOKEN_PLATFORM, BOUND_CN_PLATFORM, ISSUED_TIMESTAMP);
            Files.writeString(manifestFile, json);

            // When
            TokenManifest loaded = TokenManifest.loadFromFile(manifestFile, objectMapper);

            // Then
            assertThat(loaded.getTokens()).hasSize(1);
            assertThat(loaded.getTokens().get(0).getToken()).isEqualTo(TOKEN_PLATFORM);
            assertThat(loaded.getTokens().get(0).getBoundCN()).isEqualTo(BOUND_CN_PLATFORM);
            assertThat(loaded.getTokens().get(0).isUsed()).isFalse();
        }

        @Test
        @DisplayName("Should return empty token list when manifest file has empty tokens array")
        void shouldReturnEmptyList_whenManifestHasEmptyTokensArray() throws Exception {
            // Given
            Files.writeString(manifestFile, "{\"tokens\": []}");

            // When
            TokenManifest loaded = TokenManifest.loadFromFile(manifestFile, objectMapper);

            // Then
            assertThat(loaded.getTokens()).isEmpty();
        }
    }

    @Nested
    @DisplayName("Token Validation Tests")
    class TokenValidationTests {

        @Test
        @DisplayName("Should return token when given valid unused token and matching CN")
        void shouldReturnToken_whenGivenValidUnusedTokenAndMatchingCn() {
            // Given
            TokenManifest manifest = buildManifestWithToken(TOKEN_PLATFORM, BOUND_CN_PLATFORM, false);

            // When
            BootstrapToken result = manifest.validate(TOKEN_PLATFORM, BOUND_CN_PLATFORM);

            // Then
            assertThat(result.getToken()).isEqualTo(TOKEN_PLATFORM);
            assertThat(result.getBoundCN()).isEqualTo(BOUND_CN_PLATFORM);
        }

        @Test
        @DisplayName("Should throw InvalidBootstrapTokenException when given unknown token")
        void shouldThrowInvalidBootstrapTokenException_whenGivenUnknownToken() {
            // Given
            TokenManifest manifest = buildManifestWithToken(TOKEN_PLATFORM, BOUND_CN_PLATFORM, false);

            // When & Then
            assertThatThrownBy(() -> manifest.validate(TOKEN_UNKNOWN, BOUND_CN_PLATFORM))
                    .isInstanceOf(InvalidBootstrapTokenException.class)
                    .hasMessage(TokenManifest.INVALID_TOKEN_MSG);
        }

        @Test
        @DisplayName("Should throw TokenAlreadyUsedException when given already-used token")
        void shouldThrowTokenAlreadyUsedException_whenGivenAlreadyUsedToken() {
            // Given
            TokenManifest manifest = buildManifestWithToken(TOKEN_PLATFORM, BOUND_CN_PLATFORM, true);

            // When & Then
            assertThatThrownBy(() -> manifest.validate(TOKEN_PLATFORM, BOUND_CN_PLATFORM))
                    .isInstanceOf(TokenAlreadyUsedException.class)
                    .hasMessage(TokenManifest.TOKEN_ALREADY_USED_MSG);
        }

        @Test
        @DisplayName("Should throw TokenCnMismatchException when CN does not match bound CN")
        void shouldThrowTokenCnMismatchException_whenCnDoesNotMatchBoundCn() {
            // Given
            TokenManifest manifest = buildManifestWithToken(TOKEN_PLATFORM, BOUND_CN_PLATFORM, false);

            // When & Then
            assertThatThrownBy(() -> manifest.validate(TOKEN_PLATFORM, DIFFERENT_CN))
                    .isInstanceOf(TokenCnMismatchException.class)
                    .hasMessage(TokenManifest.TOKEN_CN_MISMATCH_MSG);
        }
    }

    @Nested
    @DisplayName("Token Invalidation and Persistence Tests")
    class TokenInvalidationTests {

        @Test
        @DisplayName("Should mark token as used when invalidated")
        void shouldMarkTokenAsUsed_whenInvalidated() {
            // Given
            TokenManifest manifest = buildManifestWithToken(TOKEN_PLATFORM, BOUND_CN_PLATFORM, false);

            // When
            manifest.invalidate(TOKEN_PLATFORM);

            // Then
            assertThat(manifest.getTokens().get(0).isUsed()).isTrue();
        }

        @Test
        @DisplayName("Should persist token state to disk when token is invalidated")
        void shouldPersistTokenState_whenTokenInvalidated() throws Exception {
            // Given
            TokenManifest manifest = buildManifestWithToken(TOKEN_PLATFORM, BOUND_CN_PLATFORM, false);

            // When
            manifest.invalidate(TOKEN_PLATFORM);

            // Then — reload from disk and verify used=true was persisted
            TokenManifest reloaded = TokenManifest.loadFromFile(manifestFile, objectMapper);
            assertThat(reloaded.getTokens().get(0).isUsed()).isTrue();
        }

        @Test
        @DisplayName("Should return true for hasUnusedTokenForCn when unused token exists")
        void shouldReturnTrue_whenUnusedTokenExistsForCn() {
            // Given
            TokenManifest manifest = buildManifestWithToken(TOKEN_PLATFORM, BOUND_CN_PLATFORM, false);

            // When
            boolean result = manifest.hasUnusedTokenForCn(BOUND_CN_PLATFORM);

            // Then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("Should return false for hasUnusedTokenForCn when only used tokens exist")
        void shouldReturnFalse_whenOnlyUsedTokensExistForCn() {
            // Given
            TokenManifest manifest = buildManifestWithToken(TOKEN_PLATFORM, BOUND_CN_PLATFORM, true);

            // When
            boolean result = manifest.hasUnusedTokenForCn(BOUND_CN_PLATFORM);

            // Then
            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("Token Generation Tests")
    class TokenGenerationTests {

        @Test
        @DisplayName("Should add token with correct CN when generateToken is called")
        void shouldAddTokenWithCorrectCn_whenGenerateTokenIsCalled() {
            // Given
            TokenManifest manifest = new TokenManifest(manifestFile, objectMapper);

            // When
            manifest.generateToken(BOUND_CN_PLATFORM);

            // Then
            assertThat(manifest.getTokens()).hasSize(1);
            BootstrapToken generated = manifest.getTokens().get(0);
            assertThat(generated.getBoundCN()).isEqualTo(BOUND_CN_PLATFORM);
            assertThat(generated.isUsed()).isFalse();
            assertThat(generated.getToken()).isNotBlank();
        }

        @Test
        @DisplayName("Should generate cryptographically unique tokens when called multiple times")
        void shouldGenerateUniqueTokens_whenCalledMultipleTimes() {
            // Given
            TokenManifest manifest = new TokenManifest(manifestFile, objectMapper);

            // When
            manifest.generateToken(BOUND_CN_PLATFORM);
            manifest.generateToken(DIFFERENT_CN);

            // Then
            String token1 = manifest.getTokens().get(0).getToken();
            String token2 = manifest.getTokens().get(1).getToken();
            assertThat(token1).isNotEqualTo(token2);
        }
    }
}
