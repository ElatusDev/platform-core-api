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
            // Given: IP in 192.168.1.0/24 range
            List<String> cidrs = List.of(CIDR_CLASS_C);

            // When: checking if allowed
            boolean result = CidrMatcher.isAllowed(IP_IN_CLASS_C, cidrs);

            // Then: should be allowed
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("Should return false when IP is outside /24 CIDR range")
        void shouldReturnFalse_whenIpIsOutsideCidr24() {
            // Given: IP outside 192.168.1.0/24 range
            List<String> cidrs = List.of(CIDR_CLASS_C);

            // When: checking if allowed
            boolean result = CidrMatcher.isAllowed(IP_OUTSIDE_CLASS_C, cidrs);

            // Then: should not be allowed
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("Should return true when IP matches /32 single host")
        void shouldReturnTrue_whenIpMatchesSingleHost() {
            // Given: exact host match
            List<String> cidrs = List.of(CIDR_SINGLE_HOST);

            // When: checking if allowed
            boolean result = CidrMatcher.isAllowed(IP_SINGLE_HOST, cidrs);

            // Then: should be allowed
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("Should return true when IP matches any range in list")
        void shouldReturnTrue_whenIpMatchesAnyRangeInList() {
            // Given: multiple CIDR ranges
            List<String> cidrs = List.of(CIDR_CLASS_C, CIDR_CLASS_A);

            // When: checking IP that matches second range
            boolean result = CidrMatcher.isAllowed(IP_IN_CLASS_A, cidrs);

            // Then: should be allowed
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("Should return false when no ranges match")
        void shouldReturnFalse_whenNoRangesMatch() {
            // Given: CIDR ranges that don't contain the IP
            List<String> cidrs = List.of(CIDR_CLASS_C, CIDR_SINGLE_HOST);

            // When: checking unmatched IP
            boolean result = CidrMatcher.isAllowed(IP_OUTSIDE_ALL, cidrs);

            // Then: should not be allowed
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("Should return false when CIDR list is empty")
        void shouldReturnFalse_whenCidrListIsEmpty() {
            // Given: empty CIDR list
            List<String> cidrs = Collections.emptyList();

            // When: checking any IP
            boolean result = CidrMatcher.isAllowed(IP_IN_CLASS_C, cidrs);

            // Then: should not be allowed
            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("IPv6 matching")
    class Ipv6Matching {

        @Test
        @DisplayName("Should return true when IPv6 address is within CIDR range")
        void shouldReturnTrue_whenIpv6IsWithinCidr() {
            // Given: IPv6 CIDR range
            List<String> cidrs = List.of("2001:db8::/32");

            // When: checking IPv6 within range
            boolean result = CidrMatcher.isAllowed("2001:db8::1", cidrs);

            // Then: should be allowed
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("Should return false when IPv6 address is outside CIDR range")
        void shouldReturnFalse_whenIpv6IsOutsideCidr() {
            // Given: IPv6 CIDR range
            List<String> cidrs = List.of("2001:db8::/32");

            // When: checking IPv6 outside range
            boolean result = CidrMatcher.isAllowed("2001:db9::1", cidrs);

            // Then: should not be allowed
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("Should return false when IPv4 client checked against IPv6 CIDR")
        void shouldReturnFalse_whenIpv4ClientCheckedAgainstIpv6Cidr() {
            // Given: IPv6 CIDR range
            List<String> cidrs = List.of("2001:db8::/32");

            // When: checking IPv4 against IPv6 range
            boolean result = CidrMatcher.isAllowed(IP_IN_CLASS_C, cidrs);

            // Then: should not match (address family mismatch)
            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("Input validation")
    class InputValidation {

        @Test
        @DisplayName("Should throw IllegalArgumentException when client IP is malformed")
        void shouldThrowIllegalArgumentException_whenClientIpIsMalformed() {
            // Given: malformed IP address
            String malformedIp = "not-an-ip";
            List<String> cidrs = List.of(CIDR_CLASS_C);

            // When/Then: should throw with message from constant
            assertThatThrownBy(() -> CidrMatcher.isAllowed(malformedIp, cidrs))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage(String.format(CidrMatcher.ERROR_INVALID_IP, malformedIp));
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when CIDR network address is malformed")
        void shouldThrowIllegalArgumentException_whenCidrNetworkAddressIsMalformed() {
            // Given: CIDR with malformed network address
            String malformedCidr = "not-an-ip/24";
            List<String> cidrs = List.of(malformedCidr);

            // When/Then: should throw with message from constant
            assertThatThrownBy(() -> CidrMatcher.isAllowed(IP_IN_CLASS_C, cidrs))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage(String.format(CidrMatcher.ERROR_INVALID_IP, "not-an-ip"));
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when CIDR prefix is non-numeric")
        void shouldThrowIllegalArgumentException_whenCidrPrefixIsNonNumeric() {
            // Given: CIDR with non-numeric prefix
            String badCidr = "192.168.1.0/abc";
            List<String> cidrs = List.of(badCidr);

            // When/Then: should throw with message from constant
            assertThatThrownBy(() -> CidrMatcher.isAllowed(IP_IN_CLASS_C, cidrs))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage(String.format(CidrMatcher.ERROR_INVALID_CIDR, badCidr));
        }

        @Test
        @DisplayName("Should treat CIDR without prefix as single host match")
        void shouldTreatCidrWithoutPrefix_asSingleHostMatch() {
            // Given: CIDR without prefix length (treated as /32)
            List<String> cidrs = List.of("192.168.1.100");

            // When: checking exact IP match
            boolean result = CidrMatcher.isAllowed(IP_IN_CLASS_C, cidrs);

            // Then: should match as single host
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("Should not match CIDR without prefix when IPs differ")
        void shouldNotMatchCidrWithoutPrefix_whenIpsDiffer() {
            // Given: CIDR without prefix length
            List<String> cidrs = List.of("192.168.1.1");

            // When: checking different IP
            boolean result = CidrMatcher.isAllowed(IP_IN_CLASS_C, cidrs);

            // Then: should not match
            assertThat(result).isFalse();
        }
    }
}
