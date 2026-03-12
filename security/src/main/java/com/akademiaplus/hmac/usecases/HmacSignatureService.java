/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.hmac.usecases;

import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Computes and verifies HMAC-SHA256 signatures for API request and response integrity.
 *
 * <p>This service handles the cryptographic operations for both request verification
 * (client-to-server) and response signing (server-to-client). It does NOT manage
 * keys, nonces, or timestamps — those are handled by {@code HmacKeyService},
 * {@code NonceStore}, and the filters respectively.</p>
 *
 * @author ElatusDev
 * @since 1.0
 */
@Service
public class HmacSignatureService {

    /** HMAC algorithm identifier. */
    public static final String HMAC_ALGORITHM = "HmacSHA256";

    /** Hash algorithm for body hashing. */
    public static final String HASH_ALGORITHM = "SHA-256";

    /** Separator between string-to-sign components. */
    public static final String STRING_TO_SIGN_SEPARATOR = "\n";

    /** SHA-256 hash of an empty body. */
    public static final String EMPTY_BODY_HASH =
            "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855";

    /** Error message when SHA-256 algorithm is not available. */
    public static final String ERROR_SHA256_NOT_AVAILABLE = "SHA-256 algorithm not available";

    /** Error message when HMAC-SHA256 computation fails. */
    public static final String ERROR_HMAC_COMPUTATION_FAILED = "HMAC-SHA256 computation failed";

    /**
     * Computes the SHA-256 hash of the given bytes, returning a hex string.
     *
     * @param body the bytes to hash
     * @return the hex-encoded SHA-256 hash
     */
    public String computeBodyHash(byte[] body) {
        try {
            MessageDigest digest = MessageDigest.getInstance(HASH_ALGORITHM);
            byte[] hash = digest.digest(body);
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(ERROR_SHA256_NOT_AVAILABLE, e);
        }
    }

    /**
     * Builds the request string-to-sign from its components.
     *
     * @param method    HTTP method (GET, POST, etc.)
     * @param path      request URI path
     * @param timestamp epoch seconds as string
     * @param bodyHash  SHA-256 hex hash of the request body
     * @param nonce     unique request identifier
     * @return the concatenated string-to-sign
     */
    public String buildRequestStringToSign(String method, String path,
                                           String timestamp, String bodyHash, String nonce) {
        return method + STRING_TO_SIGN_SEPARATOR
                + path + STRING_TO_SIGN_SEPARATOR
                + timestamp + STRING_TO_SIGN_SEPARATOR
                + bodyHash + STRING_TO_SIGN_SEPARATOR
                + nonce;
    }

    /**
     * Builds the response string-to-sign from its components.
     *
     * @param statusCode    HTTP status code as string
     * @param bodyHash      SHA-256 hex hash of the response body
     * @param timestamp     epoch seconds as string
     * @param requestNonce  the nonce from the original request
     * @return the concatenated string-to-sign
     */
    public String buildResponseStringToSign(String statusCode, String bodyHash,
                                            String timestamp, String requestNonce) {
        return statusCode + STRING_TO_SIGN_SEPARATOR
                + bodyHash + STRING_TO_SIGN_SEPARATOR
                + timestamp + STRING_TO_SIGN_SEPARATOR
                + requestNonce;
    }

    /**
     * Computes the HMAC-SHA256 of the given string using the provided key.
     *
     * @param key          the signing key bytes
     * @param stringToSign the data to sign
     * @return the hex-encoded HMAC-SHA256 signature
     */
    public String computeHmac(byte[] key, String stringToSign) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(key, HMAC_ALGORITHM));
            byte[] hmacBytes = mac.doFinal(stringToSign.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hmacBytes);
        } catch (Exception e) {
            throw new IllegalStateException(ERROR_HMAC_COMPUTATION_FAILED, e);
        }
    }

    /**
     * Verifies that the provided signature matches the expected HMAC for the given string-to-sign.
     *
     * @param key               the signing key bytes
     * @param stringToSign      the data that was signed
     * @param providedSignature the signature to verify
     * @return true if the signatures match (constant-time comparison)
     */
    public boolean verifySignature(byte[] key, String stringToSign, String providedSignature) {
        String expectedSignature = computeHmac(key, stringToSign);
        return MessageDigest.isEqual(
                expectedSignature.getBytes(StandardCharsets.UTF_8),
                providedSignature.getBytes(StandardCharsets.UTF_8));
    }
}
