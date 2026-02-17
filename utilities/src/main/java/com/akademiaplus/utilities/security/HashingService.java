/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.utilities.security;

import com.akademiaplus.utilities.exceptions.security.HashingFailureException;
import org.apache.commons.codec.binary.Hex;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Provides cryptographic hashing services for data integrity and verification.
 *
 * <p>This service provides:
 * <ul>
 *   <li>SHA-256 hashing for general-purpose data integrity</li>
 *   <li>Salted hashing for enhanced security</li>
 *   <li>Hexadecimal and Base64 encoding options</li>
 * </ul>
 *
 * <p><b>Security Warning:</b> This service uses SHA-256, which is suitable for
 * general hashing purposes (checksums, data integrity, tokens) but is NOT suitable
 * for password hashing. For passwords, use dedicated password hashing algorithms
 * like Argon2, bcrypt, or PBKDF2 instead.
 *
 * <p><b>Thread Safety:</b> This service is thread-safe. MessageDigest instances
 * are created per invocation to avoid synchronization overhead.
 *
 * @author ElatusDev
 * @version 2.0
 * @since 1.0
 */
@Service
public class HashingService {

    // Error messages as public constants (referenced by tests)
    public static final String ERROR_INPUT_NULL_OR_EMPTY = "Input cannot be null or empty";
    public static final String ERROR_SALT_NULL = "Salt cannot be null";
    public static final String ERROR_ALGORITHM_NOT_AVAILABLE = "Hashing algorithm is not available: %s";

    // Algorithm constants
    private static final String HASH_ALGORITHM = "SHA-256";
    private static final int SALT_LENGTH_BYTES = 16;

    // SecureRandom for salt generation (thread-safe)
    private final SecureRandom secureRandom;

    /**
     * Constructs a new HashingService with a SecureRandom instance for salt generation.
     */
    public HashingService() {
        this.secureRandom = new SecureRandom();
    }

    /**
     * Generates a SHA-256 hash of the input string and returns it as a hexadecimal string.
     *
     * <p>This method is suitable for:
     * <ul>
     *   <li>Data integrity verification</li>
     *   <li>Checksums</li>
     *   <li>Non-password token generation</li>
     * </ul>
     *
     * <p><b>Not suitable for:</b> Password hashing (use Argon2, bcrypt, or PBKDF2 instead)
     *
     * @param input the string to hash
     * @return the hexadecimal representation of the SHA-256 hash
     * @throws HashingFailureException if the input is null or empty, or if hashing fails
     */
    public String generateHash(String input) {
        validateInput(input);

        try {
            MessageDigest digest = createMessageDigest();
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return Hex.encodeHexString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new HashingFailureException(
                    String.format(ERROR_ALGORITHM_NOT_AVAILABLE, HASH_ALGORITHM), e);
        }
    }

    /**
     * Generates an SHA-256 hash with a random salt and returns both hash and salt.
     *
     * <p>The salt adds randomness to prevent rainbow table attacks and ensures
     * identical inputs produce different hashes.
     *
     * <p><b>Usage Pattern:</b>
     * <pre>{@code
     * SaltedHash result = hashingService.generateSaltedHash("myData");
     * // Store both result.getHash() and result.getSalt() in database
     * // Later, verify with: verifyHash("myData", storedSalt, storedHash)
     * }</pre>
     *
     * @param input the string to hash
     * @return a SaltedHash containing the hash and the generated salt
     * @throws HashingFailureException if the input is null or empty, or if hashing fails
     */
    public SaltedHash generateSaltedHash(String input) {
        validateInput(input);

        byte[] salt = generateSalt();
        String hash = generateHashWithSalt(input, salt);

        return new SaltedHash(hash, Base64.getEncoder().encodeToString(salt));
    }

    /**
     * Generates a SHA-256 hash using the provided salt.
     *
     * <p>Use this method to verify a previously hashed value by providing
     * the original salt used during hashing.
     *
     * @param input the string to hash
     * @param saltBase64 the Base64-encoded salt to use
     * @return the hexadecimal representation of the salted hash
     * @throws HashingFailureException if the input or salt is null/empty, or if hashing fails
     */
    public String generateHashWithSalt(String input, String saltBase64) {
        validateInput(input);
        validateSalt(saltBase64);

        byte[] salt = Base64.getDecoder().decode(saltBase64);
        return generateHashWithSalt(input, salt);
    }

    /**
     * Verifies if the input matches the expected hash when combined with the given salt.
     *
     * <p><b>Security Note:</b> Uses constant-time comparison to prevent timing attacks.
     *
     * @param input the input to verify
     * @param saltBase64 the Base64-encoded salt used during original hashing
     * @param expectedHash the expected hash value to compare against
     * @return true if the input hashes to the expected value, false otherwise
     * @throws HashingFailureException if any input is null/empty, or if hashing fails
     */
    public boolean verifyHash(String input, String saltBase64, String expectedHash) {
        validateInput(input);
        validateSalt(saltBase64);
        validateInput(expectedHash);

        String actualHash = generateHashWithSalt(input, saltBase64);

        // Constant-time comparison to prevent timing attacks
        return constantTimeEquals(actualHash, expectedHash);
    }

    /**
     * Validates that the input is not null or empty.
     *
     * @param input the input to validate
     * @throws HashingFailureException if the input is null or empty
     */
    private void validateInput(String input) {
        if (input == null || input.trim().isEmpty()) {
            throw new HashingFailureException(ERROR_INPUT_NULL_OR_EMPTY);
        }
    }

    /**
     * Validates that the salt is not null or empty.
     *
     * @param salt the salt to validate
     * @throws HashingFailureException if the salt is null or empty
     */
    private void validateSalt(String salt) {
        if (salt == null || salt.trim().isEmpty()) {
            throw new HashingFailureException(ERROR_SALT_NULL);
        }
    }

    /**
     * Creates a new MessageDigest instance for the configured algorithm.
     *
     * @return a new MessageDigest instance
     * @throws NoSuchAlgorithmException if the algorithm is not available
     */
    private MessageDigest createMessageDigest() throws NoSuchAlgorithmException {
        return MessageDigest.getInstance(HASH_ALGORITHM);
    }

    /**
     * Generates a cryptographically secure random salt.
     *
     * @return a byte array containing random salt data
     */
    private byte[] generateSalt() {
        byte[] salt = new byte[SALT_LENGTH_BYTES];
        secureRandom.nextBytes(salt);
        return salt;
    }

    /**
     * Generates a SHA-256 hash of the input combined with the salt.
     *
     * @param input the string to hash
     * @param salt the salt bytes to combine with the input
     * @return the hexadecimal representation of the hash
     * @throws HashingFailureException if hashing fails
     */
    private String generateHashWithSalt(String input, byte[] salt) {
        try {
            MessageDigest digest = createMessageDigest();
            digest.update(salt);
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return Hex.encodeHexString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new HashingFailureException(
                    String.format(ERROR_ALGORITHM_NOT_AVAILABLE, HASH_ALGORITHM), e);
        }
    }

    /**
     * Performs a constant-time string comparison to prevent timing attacks.
     *
     * <p>This method ensures that the comparison takes the same amount of time
     * regardless of where the strings differ, preventing attackers from using
     * timing information to guess hash values.
     *
     * @param a the first string to compare
     * @param b the second string to compare
     * @return true if the strings are equal, false otherwise
     */
    private boolean constantTimeEquals(String a, String b) {
        if (a.length() != b.length()) {
            return false;
        }

        int result = 0;
        for (int i = 0; i < a.length(); i++) {
            result |= a.charAt(i) ^ b.charAt(i);
        }

        return result == 0;
    }

    /**
     * Container class for a hash value and its associated salt.
     *
     * <p>Both values should be stored together (typically in a database)
     * to enable verification later.
     *
     * @param hash -- GETTER --
     *             Gets the hexadecimal hash value.
     * @param salt -- GETTER --
     *             Gets the Base64-encoded salt value.
     */
        public record SaltedHash(String hash, String salt) {
    }
}