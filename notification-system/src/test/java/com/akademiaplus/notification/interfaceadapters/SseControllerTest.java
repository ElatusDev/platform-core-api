/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.notification.interfaceadapters;

import com.akademiaplus.infra.persistence.config.TenantContextHolder;
import com.akademiaplus.notification.usecases.SseEmitterRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Collections;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@DisplayName("SseController")
@ExtendWith(MockitoExtension.class)
class SseControllerTest {

    private static final Long TENANT_ID = 1L;
    private static final Long USER_ID = 100L;
    private static final String USER_ID_STRING = "100";

    @Mock
    private SseEmitterRegistry sseEmitterRegistry;

    @Mock
    private TenantContextHolder tenantContextHolder;

    private SseController sseController;

    @BeforeEach
    void setUp() {
        sseController = new SseController(sseEmitterRegistry, tenantContextHolder);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void setUpAuthenticatedUser(Long userId) {
        Map<String, Object> claims = Map.of(SseController.CLAIM_USER_ID, userId);
        UsernamePasswordAuthenticationToken token =
                new UsernamePasswordAuthenticationToken("user", "password", Collections.emptyList());
        token.setDetails(claims);
        SecurityContextHolder.getContext().setAuthentication(token);
    }

    @Nested
    @DisplayName("Subscribe")
    class Subscribe {

        @Test
        @DisplayName("Should return registered emitter when user is authenticated")
        void shouldReturnRegisteredEmitter_whenUserIsAuthenticated() {
            // Given
            setUpAuthenticatedUser(USER_ID);
            SseEmitter expectedEmitter = new SseEmitter();
            when(tenantContextHolder.requireTenantId()).thenReturn(TENANT_ID);
            when(sseEmitterRegistry.register(TENANT_ID, USER_ID)).thenReturn(expectedEmitter);

            // When
            SseEmitter result = sseController.subscribe();

            // Then
            assertThat(result).isSameAs(expectedEmitter);
            verify(tenantContextHolder, times(1)).requireTenantId();
            verify(sseEmitterRegistry, times(1)).register(TENANT_ID, USER_ID);
            verifyNoMoreInteractions(tenantContextHolder, sseEmitterRegistry);
        }
    }

    @Nested
    @DisplayName("Extract Authenticated User ID")
    class ExtractAuthenticatedUserId {

        @Test
        @DisplayName("Should return user ID when valid authentication with user_id claim")
        void shouldReturnUserId_whenValidAuthenticationWithClaim() {
            // Given
            setUpAuthenticatedUser(USER_ID);

            // When
            Long result = sseController.extractAuthenticatedUserId();

            // Then
            assertThat(result).isEqualTo(USER_ID);
            verifyNoInteractions(sseEmitterRegistry, tenantContextHolder);
        }

        @Test
        @DisplayName("Should throw IllegalStateException when authentication is null")
        void shouldThrowIllegalStateException_whenAuthenticationIsNull() {
            // Given — no authentication set in SecurityContextHolder

            // When & Then
            assertThatThrownBy(() -> sseController.extractAuthenticatedUserId())
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage(SseController.ERROR_USER_ID_NOT_FOUND);
            verifyNoInteractions(sseEmitterRegistry, tenantContextHolder);
        }

        @Test
        @DisplayName("Should throw IllegalStateException when token details is not a Map")
        void shouldThrowIllegalStateException_whenDetailsIsNotMap() {
            // Given
            UsernamePasswordAuthenticationToken token =
                    new UsernamePasswordAuthenticationToken("user", "password", Collections.emptyList());
            token.setDetails("not-a-map");
            SecurityContextHolder.getContext().setAuthentication(token);

            // When & Then
            assertThatThrownBy(() -> sseController.extractAuthenticatedUserId())
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage(SseController.ERROR_USER_ID_NOT_FOUND);
            verifyNoInteractions(sseEmitterRegistry, tenantContextHolder);
        }

        @Test
        @DisplayName("Should throw IllegalStateException when user_id claim is missing from map")
        void shouldThrowIllegalStateException_whenUserIdClaimMissing() {
            // Given
            Map<String, Object> claimsWithoutUserId = Map.of("other_claim", "value");
            UsernamePasswordAuthenticationToken token =
                    new UsernamePasswordAuthenticationToken("user", "password", Collections.emptyList());
            token.setDetails(claimsWithoutUserId);
            SecurityContextHolder.getContext().setAuthentication(token);

            // When & Then
            assertThatThrownBy(() -> sseController.extractAuthenticatedUserId())
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage(SseController.ERROR_USER_ID_NOT_FOUND);
            verifyNoInteractions(sseEmitterRegistry, tenantContextHolder);
        }

        @Test
        @DisplayName("Should return user ID when claim value is a String")
        void shouldReturnUserId_whenClaimValueIsString() {
            // Given
            Map<String, Object> claims = Map.of(SseController.CLAIM_USER_ID, USER_ID_STRING);
            UsernamePasswordAuthenticationToken token =
                    new UsernamePasswordAuthenticationToken("user", "password", Collections.emptyList());
            token.setDetails(claims);
            SecurityContextHolder.getContext().setAuthentication(token);

            // When
            Long result = sseController.extractAuthenticatedUserId();

            // Then
            assertThat(result).isEqualTo(USER_ID);
            verifyNoInteractions(sseEmitterRegistry, tenantContextHolder);
        }
    }
}
