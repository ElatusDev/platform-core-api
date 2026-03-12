/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.magiclink.config;

import com.akademiaplus.magiclink.exceptions.MagicLinkTokenAlreadyUsedException;
import com.akademiaplus.magiclink.exceptions.MagicLinkTokenExpiredException;
import com.akademiaplus.magiclink.exceptions.MagicLinkTokenNotFoundException;
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
 * Unit tests for {@link MagicLinkControllerAdvice}.
 *
 * @author ElatusDev
 * @since 1.0
 */
@DisplayName("MagicLinkControllerAdvice")
@ExtendWith(MockitoExtension.class)
class MagicLinkControllerAdviceTest {

    @Mock private MessageService messageService;

    private MagicLinkControllerAdvice advice;

    @BeforeEach
    void setUp() {
        advice = new MagicLinkControllerAdvice(messageService);
    }

    @Nested
    @DisplayName("Token Not Found")
    class TokenNotFound {

        @Test
        @DisplayName("Should return 401 when token not found")
        void shouldReturn401_whenTokenNotFound() {
            // Given
            MagicLinkTokenNotFoundException ex = new MagicLinkTokenNotFoundException();

            // When
            ResponseEntity<ErrorResponseDTO> response = advice.handleTokenNotFound(ex);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getCode()).isEqualTo(MagicLinkControllerAdvice.CODE_TOKEN_NOT_FOUND);
            verifyNoInteractions(messageService);
        }
    }

    @Nested
    @DisplayName("Token Expired")
    class TokenExpired {

        @Test
        @DisplayName("Should return 401 when token expired")
        void shouldReturn401_whenTokenExpired() {
            // Given
            MagicLinkTokenExpiredException ex = new MagicLinkTokenExpiredException();

            // When
            ResponseEntity<ErrorResponseDTO> response = advice.handleTokenExpired(ex);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getCode()).isEqualTo(MagicLinkControllerAdvice.CODE_TOKEN_EXPIRED);
            verifyNoInteractions(messageService);
        }
    }

    @Nested
    @DisplayName("Token Already Used")
    class TokenAlreadyUsed {

        @Test
        @DisplayName("Should return 401 when token already used")
        void shouldReturn401_whenTokenAlreadyUsed() {
            // Given
            MagicLinkTokenAlreadyUsedException ex = new MagicLinkTokenAlreadyUsedException();

            // When
            ResponseEntity<ErrorResponseDTO> response = advice.handleTokenAlreadyUsed(ex);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getCode()).isEqualTo(MagicLinkControllerAdvice.CODE_TOKEN_ALREADY_USED);
            verifyNoInteractions(messageService);
        }
    }
}
