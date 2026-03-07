/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.tokenbinding.interfaceadapters.config;

import com.akademiaplus.tokenbinding.usecases.domain.TokenBindingMode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link TokenBindingProperties}.
 *
 * @author ElatusDev
 * @since 1.0
 */
@DisplayName("TokenBindingProperties")
class TokenBindingPropertiesTest {

    @Test
    @DisplayName("should default to STRICT mode")
    void shouldDefaultToStrictMode() {
        // Given
        TokenBindingProperties properties = new TokenBindingProperties();

        // When / Then
        assertThat(properties.getMode()).isEqualTo(TokenBindingMode.STRICT);
    }

    @Test
    @DisplayName("should allow setting mode")
    void shouldAllowSettingMode() {
        // Given
        TokenBindingProperties properties = new TokenBindingProperties();

        // When
        properties.setMode(TokenBindingMode.RELAXED);

        // Then
        assertThat(properties.getMode()).isEqualTo(TokenBindingMode.RELAXED);
    }
}
