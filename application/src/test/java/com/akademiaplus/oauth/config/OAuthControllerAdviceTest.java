/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.oauth.config;

import com.akademiaplus.oauth.exceptions.OAuthProviderException;
import com.akademiaplus.oauth.exceptions.UnsupportedProviderException;
import com.akademiaplus.utilities.MessageService;
import openapi.akademiaplus.domain.utilities.dto.ErrorResponseDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * Unit tests for {@link OAuthControllerAdvice}.
 *
 * @author ElatusDev
 * @since 1.0
 */
@DisplayName("OAuthControllerAdvice")
@ExtendWith(MockitoExtension.class)
class OAuthControllerAdviceTest {

    @Mock private MessageService messageService;

    private OAuthControllerAdvice advice;

    @BeforeEach
    void setUp() {
        advice = new OAuthControllerAdvice(messageService);
    }

    @Nested
    @DisplayName("Unsupported Provider")
    class UnsupportedProvider {

        @Test
        @DisplayName("Should return 400 when provider is unsupported")
        void shouldReturn400_whenProviderIsUnsupported() {
            // Given
            UnsupportedProviderException ex = new UnsupportedProviderException("twitter");

            // When
            ResponseEntity<ErrorResponseDTO> response = advice.handleUnsupportedProvider(ex);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getCode()).isEqualTo(OAuthControllerAdvice.CODE_UNSUPPORTED_PROVIDER);
            assertThat(response.getBody().getMessage()).contains("twitter");
            verifyNoInteractions(messageService);
        }
    }

    @Nested
    @DisplayName("OAuth Provider Error")
    class ProviderError {

        @Test
        @DisplayName("Should return 401 when provider exchange fails")
        void shouldReturn401_whenProviderExchangeFails() {
            // Given
            OAuthProviderException ex = new OAuthProviderException("google");

            // When
            ResponseEntity<ErrorResponseDTO> response = advice.handleOAuthProviderError(ex);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getCode()).isEqualTo(OAuthControllerAdvice.CODE_OAUTH_PROVIDER_ERROR);
            assertThat(response.getBody().getMessage()).contains("google");
            verifyNoInteractions(messageService);
        }
    }
}
