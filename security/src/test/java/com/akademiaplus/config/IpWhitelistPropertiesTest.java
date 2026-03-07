/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link IpWhitelistProperties}.
 *
 * @author ElatusDev
 * @since 1.0
 */
@DisplayName("IpWhitelistProperties")
class IpWhitelistPropertiesTest {

    @Nested
    @DisplayName("Default values")
    class DefaultValues {

        @Test
        @DisplayName("Should return empty list when no CIDRs are configured")
        void shouldReturnEmptyList_whenNoCidrsConfigured() {
            // Given
            IpWhitelistProperties properties = new IpWhitelistProperties();

            // When
            List<String> result = properties.getAllowedCidrs();

            // Then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("Configured values")
    class ConfiguredValues {

        @Test
        @DisplayName("Should return configured CIDR list")
        void shouldReturnConfiguredCidrList_whenCidrsAreSet() {
            // Given
            IpWhitelistProperties properties = new IpWhitelistProperties();
            List<String> cidrs = List.of("192.168.1.0/24", "10.0.0.0/8");
            properties.setAllowedCidrs(cidrs);

            // When
            List<String> result = properties.getAllowedCidrs();

            // Then
            assertThat(result).containsExactly("192.168.1.0/24", "10.0.0.0/8");
        }
    }
}
