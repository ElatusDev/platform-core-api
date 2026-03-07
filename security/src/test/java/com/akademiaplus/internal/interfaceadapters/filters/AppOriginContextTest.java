/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.internal.interfaceadapters.filters;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("AppOriginContext")
class AppOriginContextTest {

    @Nested
    @DisplayName("Origin Access")
    class OriginAccess {

        @Test
        @DisplayName("Should return akademia when attribute set to akademia")
        void shouldReturnAkademia_whenAttributeSetToAkademia() {
            // Given
            MockHttpServletRequest request = new MockHttpServletRequest();
            AppOriginContext.setAppOrigin(request, AppOriginContext.ORIGIN_AKADEMIA);

            // When
            String result = AppOriginContext.getAppOrigin(request);

            // Then
            assertThat(result).isEqualTo(AppOriginContext.ORIGIN_AKADEMIA);
        }

        @Test
        @DisplayName("Should return elatus when attribute set to elatus")
        void shouldReturnElatus_whenAttributeSetToElatus() {
            // Given
            MockHttpServletRequest request = new MockHttpServletRequest();
            AppOriginContext.setAppOrigin(request, AppOriginContext.ORIGIN_ELATUS);

            // When
            String result = AppOriginContext.getAppOrigin(request);

            // Then
            assertThat(result).isEqualTo(AppOriginContext.ORIGIN_ELATUS);
        }

        @Test
        @DisplayName("Should return default elatus when no attribute set")
        void shouldReturnDefault_whenNoAttributeSet() {
            // Given
            MockHttpServletRequest request = new MockHttpServletRequest();

            // When
            String result = AppOriginContext.getAppOrigin(request);

            // Then
            assertThat(result).isEqualTo(AppOriginContext.DEFAULT_ORIGIN);
        }
    }

    @Nested
    @DisplayName("Convenience Methods")
    class ConvenienceMethods {

        @Test
        @DisplayName("Should return true when isAkademia and origin is akademia")
        void shouldReturnTrue_whenIsAkademiaAndOriginIsAkademia() {
            // Given
            MockHttpServletRequest request = new MockHttpServletRequest();
            AppOriginContext.setAppOrigin(request, AppOriginContext.ORIGIN_AKADEMIA);

            // When
            boolean result = AppOriginContext.isAkademia(request);

            // Then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("Should return false when isAkademia and origin is elatus")
        void shouldReturnFalse_whenIsAkademiaAndOriginIsElatus() {
            // Given
            MockHttpServletRequest request = new MockHttpServletRequest();
            AppOriginContext.setAppOrigin(request, AppOriginContext.ORIGIN_ELATUS);

            // When
            boolean result = AppOriginContext.isAkademia(request);

            // Then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("Should return true when isElatus and origin is elatus")
        void shouldReturnTrue_whenIsElatusAndOriginIsElatus() {
            // Given
            MockHttpServletRequest request = new MockHttpServletRequest();
            AppOriginContext.setAppOrigin(request, AppOriginContext.ORIGIN_ELATUS);

            // When
            boolean result = AppOriginContext.isElatus(request);

            // Then
            assertThat(result).isTrue();
        }
    }
}
