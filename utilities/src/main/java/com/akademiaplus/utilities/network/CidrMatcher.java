/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.utilities.network;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;

/**
 * Validates whether an IP address falls within one or more CIDR ranges.
 *
 * <p>Supports both IPv4 and IPv6 addresses. Each CIDR range is specified
 * in standard notation (e.g., {@code "192.168.1.0/24"}, {@code "10.0.0.0/8"}).
 * A single IP without a prefix length is treated as {@code /32} (IPv4)
 * or {@code /128} (IPv6).
 *
 * @author ElatusDev
 * @since 1.0
 */
public final class CidrMatcher {

    /** Error message when the IP address format is invalid. */
    public static final String ERROR_INVALID_IP = "Invalid IP address: %s";

    /** Error message when the CIDR notation is invalid. */
    public static final String ERROR_INVALID_CIDR = "Invalid CIDR notation: %s";

    private CidrMatcher() {
        // Utility class — no instantiation
    }

    /**
     * Checks whether the given IP address is contained in any of the specified CIDR ranges.
     *
     * @param clientIp    the IP address to check (IPv4 or IPv6)
     * @param cidrRanges  the list of CIDR ranges to match against
     * @return {@code true} if the IP falls within at least one CIDR range
     * @throws IllegalArgumentException if the IP or any CIDR range is malformed
     */
    public static boolean isAllowed(String clientIp, List<String> cidrRanges) {
        InetAddress clientAddress = parseAddress(clientIp);
        byte[] clientBytes = clientAddress.getAddress();

        for (String cidr : cidrRanges) {
            if (matchesCidr(clientBytes, cidr)) {
                return true;
            }
        }
        return false;
    }

    private static boolean matchesCidr(byte[] clientBytes, String cidr) {
        String[] parts = cidr.split("/");
        InetAddress networkAddress = parseAddress(parts[0]);
        byte[] networkBytes = networkAddress.getAddress();

        if (clientBytes.length != networkBytes.length) {
            return false; // IPv4 vs IPv6 mismatch
        }

        int prefixLength = (parts.length == 2)
                ? parsePrefixLength(parts[1], cidr)
                : clientBytes.length * 8;

        return prefixMatches(clientBytes, networkBytes, prefixLength);
    }

    private static boolean prefixMatches(byte[] clientBytes, byte[] networkBytes, int prefixLength) {
        int fullBytes = prefixLength / 8;
        for (int i = 0; i < fullBytes; i++) {
            if (clientBytes[i] != networkBytes[i]) {
                return false;
            }
        }
        int remainingBits = prefixLength % 8;
        if (remainingBits > 0) {
            int mask = (0xFF << (8 - remainingBits)) & 0xFF;
            return (clientBytes[fullBytes] & mask) == (networkBytes[fullBytes] & mask);
        }
        return true;
    }

    private static InetAddress parseAddress(String ip) {
        try {
            return InetAddress.getByName(ip.trim());
        } catch (UnknownHostException e) {
            throw new IllegalArgumentException(String.format(ERROR_INVALID_IP, ip), e);
        }
    }

    private static int parsePrefixLength(String prefixStr, String cidr) {
        try {
            return Integer.parseInt(prefixStr.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(String.format(ERROR_INVALID_CIDR, cidr), e);
        }
    }
}
