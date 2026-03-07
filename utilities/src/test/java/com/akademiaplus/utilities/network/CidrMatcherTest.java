/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.utilities.network;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link CidrMatcher}.
 *
 * @author ElatusDev
 * @since 1.0
 */
@DisplayName("CidrMatcher")
class CidrMatcherTest {

    private static final String CIDR_CLASS_C = "192.168.1.0/24";
    private static final String CIDR_CLASS_A = "10.0.0.0/8";
    private static final String CIDR_SINGLE_HOST = "172.16.0.1/32";
    private static final String IP_IN_CLASS_C = "192.168.1.100";
    private static final String IP_OUTSIDE_CLASS_C = "192.168.2.1";
    private static final String IP_IN_CLASS_A = "10.255.0.1";
    private static final String IP_SINGLE_HOST = "172.16.0.1";
    private static final String IP_OUTSIDE_ALL = "203.0.113.50";

    @Nested
    @DisplayName("IPv4 matching")
    class Ipv4Matching {

        @Test
        @DisplayName("Should return true when IP is within /24 CIDR range")
        void shouldReturnTrue_whenIpIsWithinCidr24() {
            // Given
            List<String> cidrs = List.of(CIDR_CLASS_C);

            // When
            boolean result = CidrMatcher.isAllowed(IP_IN_CLASS_C, cidrs);

            // Then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("Should return false when IP is outside /24 CIDR range")
        void shouldReturnFalse_whenIpIsOutsideCidr24() {
            // Given
            List<String> cidrs = List.of(CIDR_CLASS_C);

            // When
            boolean result = CidrMatcher.isAllowed(IP_OUTSIDE_CLASS_C, cidrs);

            // Then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("Should return true when IP matches /32 single host")
        void shouldReturnTrue_whenIpMatchesSingleHost() {
            // Given
            List<String> cidrs = List.of(CIDR_SINGLE_HOST);

            // When
            boolean result = CidrMatcher.isAllowed(IP_SINGLE_HOST, cidrs);

            // Then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("Should return true when IP matches any range in list")
        void shouldReturnTrue_whenIpMatchesAnyRangeInList() {
            // Given
            List<String> cidrs = List.of(CIDR_CLASS_C, CIDR_CLASS_A);

            // When
            boolean result = CidrMatcher.isAllowed(IP_IN_CLASS_A, cidrs);

            // Then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("Should return false when no ranges match")
        void shouldReturnFalse_whenNoRangesMatch() {
            // Given
            List<String> cidrs = List.of(CIDR_CLASS_C, CIDR_SINGLE_HOST);

            // When
            boolean result = CidrMatcher.isAllowed(IP_OUTSIDE_ALL, cidrs);

            // Then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("Should return false when CIDR list is empty")
        void shouldReturnFalse_whenCidrListIsEmpty() {
            // Given
            List<String> cidrs = Collections.emptyList();

            // When
            boolean result = CidrMatcher.isAllowed(IP_IN_CLASS_C, cidrs);

            // Then
            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("IPv6 matching")
    class Ipv6Matching {

        @Test
        @DisplayName("Should return true when IPv6 address is within CIDR range")
        void shouldReturnTrue_whenIpv6IsWithinCidr() {
            // Given
            List<String> cidrs = List.of("2001:db8::/32");

            // When
            boolean result = CidrMatcher.isAllowed("2001:db8::1", cidrs);

            // Then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("Should return false when IPv6 address is outside CIDR range")
        void shouldReturnFalse_whenIpv6IsOutsideCidr() {
            // Given
            List<String> cidrs = List.of("2001:db8::/32");

            // When
            boolean result = CidrMatcher.isAllowed("2001:db9::1", cidrs);

            // Then
            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("Error handling")
    class ErrorHandling {

        @Test
        @DisplayName("Should throw IllegalArgumentException when IP is malformed")
        void shouldThrowIllegalArgumentException_whenIpIsMalformed() {
            // Given
            List<String> cidrs = List.of(CIDR_CLASS_C);

            // When / Then
            assertThatThrownBy(() -> CidrMatcher.isAllowed("not-an-ip", cidrs))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("not-an-ip");
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when CIDR prefix is malformed")
        void shouldThrowIllegalArgumentException_whenCidrPrefixIsMalformed() {
            // Given
            List<String> cidrs = List.of("192.168.1.0/abc");

            // When / Then
            assertThatThrownBy(() -> CidrMatcher.isAllowed(IP_IN_CLASS_C, cidrs))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("192.168.1.0/abc");
        }
    }
}
