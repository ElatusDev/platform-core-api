/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.utilities.security;

import com.akademiaplus.utilities.exceptions.security.HashingFailureException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Base64;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Comprehensive test suite for {@link HashingService}.
 *
 * <p>Tests cover:
 * <ul>
 *   <li>Basic hashing functionality</li>
 *   <li>Salted hash generation</li>
 *   <li>Hash verification with constant-time comparison</li>
 *   <li>Input validation</li>
 *   <li>Deterministic and non-deterministic behavior</li>
 *   <li>Security properties (different salts, constant-time comparison)</li>
 * </ul>
 *
 * @author ElatusDev
 * @version 1.0
 */
@DisplayName("HashingService")
class HashingServiceTest {

    // Test data constants
    private static final String VALID_INPUT = "test-data-123";
    private static final String ANOTHER_VALID_INPUT = "different-data-456";
    private static final String WHITESPACE_ONLY = "   ";
    private static final int SHA256_HEX_LENGTH = 64; // SHA-256 produces 32 bytes = 64 hex characters
    private static final int EXPECTED_SALT_BASE64_LENGTH = 24; // 16 bytes base64 encoded = 24 characters

    // Regex patterns for validation
    private static final Pattern HEX_PATTERN = Pattern.compile("^[a-f0-9]+$");
    private static final Pattern BASE64_PATTERN = Pattern.compile("^[A-Za-z0-9+/]+=*$");

    private HashingService hashingService;

    @BeforeEach
    void setUp() {
        // Given: A new HashingService instance for each test
        hashingService = new HashingService();
    }

    @Nested
    @DisplayName("generateHash()")
    class GenerateHashTests {

        @Test
        @DisplayName("should generate valid SHA-256 hash in hexadecimal format when input is valid")
        void shouldGenerateValidSha256HashInHexFormat_whenInputIsValid() {
            // Given: Valid input string

            // When: Generating hash
            String hash = hashingService.generateHash(VALID_INPUT);

            // Then: Hash should be valid hexadecimal string of correct length
            assertThat(hash)
                    .isNotNull()
                    .hasSize(SHA256_HEX_LENGTH)
                    .matches(HEX_PATTERN);
        }

        @Test
        @DisplayName("should generate identical hashes when same input is hashed multiple times")
        void shouldGenerateIdenticalHashes_whenSameInputIsHashedMultipleTimes() {
            // Given: Same input string

            // When: Generating hash twice
            String hash1 = hashingService.generateHash(VALID_INPUT);
            String hash2 = hashingService.generateHash(VALID_INPUT);

            // Then: Both hashes should be identical (deterministic behavior)
            assertThat(hash1).isEqualTo(hash2);
        }

        @Test
        @DisplayName("should generate different hashes when different inputs are hashed")
        void shouldGenerateDifferentHashes_whenDifferentInputsAreHashed() {
            // Given: Two different input strings

            // When: Generating hashes for different inputs
            String hash1 = hashingService.generateHash(VALID_INPUT);
            String hash2 = hashingService.generateHash(ANOTHER_VALID_INPUT);

            // Then: Hashes should be different
            assertThat(hash1).isNotEqualTo(hash2);
        }

        @Test
        @DisplayName("should generate expected SHA-256 hash for known input")
        void shouldGenerateExpectedSha256Hash_whenKnownInputIsProvided() {
            // Given: Known input with expected SHA-256 hash
            String input = "hello";
            String expectedHash = "2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824";

            // When: Generating hash
            String actualHash = hashingService.generateHash(input);

            // Then: Generated hash should match expected value
            assertThat(actualHash).isEqualTo(expectedHash);
        }

        @Test
        @DisplayName("should throw HashingFailureException when input is null")
        void shouldThrowHashingFailureException_whenInputIsNull() {
            // Given: Null input

            // When/Then: Generating hash should throw exception with correct message
            assertThatThrownBy(() -> hashingService.generateHash(null))
                    .isInstanceOf(HashingFailureException.class)
                    .hasMessage(HashingService.ERROR_INPUT_NULL_OR_EMPTY);
        }

        @Test
        @DisplayName("should throw HashingFailureException when input is empty string")
        void shouldThrowHashingFailureException_whenInputIsEmptyString() {
            // Given: Empty string input

            // When/Then: Generating hash should throw exception with correct message
            assertThatThrownBy(() -> hashingService.generateHash(""))
                    .isInstanceOf(HashingFailureException.class)
                    .hasMessage(HashingService.ERROR_INPUT_NULL_OR_EMPTY);
        }

        @Test
        @DisplayName("should throw HashingFailureException when input is whitespace only")
        void shouldThrowHashingFailureException_whenInputIsWhitespaceOnly() {
            // Given: Whitespace-only input

            // When/Then: Generating hash should throw exception with correct message
            assertThatThrownBy(() -> hashingService.generateHash(WHITESPACE_ONLY))
                    .isInstanceOf(HashingFailureException.class)
                    .hasMessage(HashingService.ERROR_INPUT_NULL_OR_EMPTY);
        }

        @Test
        @DisplayName("should handle UTF-8 characters correctly when generating hash")
        void shouldHandleUtf8CharactersCorrectly_whenGeneratingHash() {
            // Given: Input with UTF-8 characters
            String utf8Input = "Hello 世界 🌍";

            // When: Generating hash
            String hash = hashingService.generateHash(utf8Input);

            // Then: Should generate valid hash
            assertThat(hash)
                    .isNotNull()
                    .hasSize(SHA256_HEX_LENGTH)
                    .matches(HEX_PATTERN);
        }
    }

    @Nested
    @DisplayName("generateSaltedHash()")
    class GenerateSaltedHashTests {

        @Test
        @DisplayName("should generate valid salted hash with hash and salt when input is valid")
        void shouldGenerateValidSaltedHashWithHashAndSalt_whenInputIsValid() {
            // Given: Valid input string

            // When: Generating salted hash
            HashingService.SaltedHash result = hashingService.generateSaltedHash(VALID_INPUT);

            // Then: Result should contain valid hash and salt
            assertThat(result).isNotNull();
            assertThat(result.hash())
                    .isNotNull()
                    .hasSize(SHA256_HEX_LENGTH)
                    .matches(HEX_PATTERN);
            assertThat(result.salt())
                    .isNotNull()
                    .hasSize(EXPECTED_SALT_BASE64_LENGTH)
                    .matches(BASE64_PATTERN);
        }

        @Test
        @DisplayName("should generate different salts when same input is hashed multiple times")
        void shouldGenerateDifferentSalts_whenSameInputIsHashedMultipleTimes() {
            // Given: Same input string

            // When: Generating salted hash twice
            HashingService.SaltedHash result1 = hashingService.generateSaltedHash(VALID_INPUT);
            HashingService.SaltedHash result2 = hashingService.generateSaltedHash(VALID_INPUT);

            // Then: Salts should be different (randomness)
            assertThat(result1.salt()).isNotEqualTo(result2.salt());
        }

        @Test
        @DisplayName("should generate different hashes when same input is hashed multiple times due to different salts")
        void shouldGenerateDifferentHashes_whenSameInputIsHashedMultipleTimesDueToDifferentSalts() {
            // Given: Same input string

            // When: Generating salted hash twice
            HashingService.SaltedHash result1 = hashingService.generateSaltedHash(VALID_INPUT);
            HashingService.SaltedHash result2 = hashingService.generateSaltedHash(VALID_INPUT);

            // Then: Hashes should be different due to different salts
            assertThat(result1.hash()).isNotEqualTo(result2.hash());
        }

        @Test
        @DisplayName("should generate valid Base64-encoded salt")
        void shouldGenerateValidBase64EncodedSalt_whenGeneratingSaltedHash() {
            // Given: Valid input string

            // When: Generating salted hash
            HashingService.SaltedHash result = hashingService.generateSaltedHash(VALID_INPUT);

            // Then: Salt should be decodable as Base64
            assertThat(result.salt()).isNotNull();
            byte[] decodedSalt = Base64.getDecoder().decode(result.salt());
            assertThat(decodedSalt).hasSize(16); // Salt should be 16 bytes
        }

        @Test
        @DisplayName("should throw HashingFailureException when input is null")
        void shouldThrowHashingFailureException_whenInputIsNull() {
            // Given: Null input

            // When/Then: Generating salted hash should throw exception
            assertThatThrownBy(() -> hashingService.generateSaltedHash(null))
                    .isInstanceOf(HashingFailureException.class)
                    .hasMessage(HashingService.ERROR_INPUT_NULL_OR_EMPTY);
        }

        @Test
        @DisplayName("should throw HashingFailureException when input is empty string")
        void shouldThrowHashingFailureException_whenInputIsEmptyString() {
            // Given: Empty string input

            // When/Then: Generating salted hash should throw exception
            assertThatThrownBy(() -> hashingService.generateSaltedHash(""))
                    .isInstanceOf(HashingFailureException.class)
                    .hasMessage(HashingService.ERROR_INPUT_NULL_OR_EMPTY);
        }

        @Test
        @DisplayName("should throw HashingFailureException when input is whitespace only")
        void shouldThrowHashingFailureException_whenInputIsWhitespaceOnly() {
            // Given: Whitespace-only input

            // When/Then: Generating salted hash should throw exception
            assertThatThrownBy(() -> hashingService.generateSaltedHash(WHITESPACE_ONLY))
                    .isInstanceOf(HashingFailureException.class)
                    .hasMessage(HashingService.ERROR_INPUT_NULL_OR_EMPTY);
        }
    }

    @Nested
    @DisplayName("generateHashWithSalt()")
    class GenerateHashWithSaltTests {

        private static final String VALID_SALT_BASE64 = "abcdefghijklmnopqrstuv==";

        @Test
        @DisplayName("should generate valid hash when input and salt are valid")
        void shouldGenerateValidHash_whenInputAndSaltAreValid() {
            // Given: Valid input and salt

            // When: Generating hash with salt
            String hash = hashingService.generateHashWithSalt(VALID_INPUT, VALID_SALT_BASE64);

            // Then: Hash should be valid hexadecimal string
            assertThat(hash)
                    .isNotNull()
                    .hasSize(SHA256_HEX_LENGTH)
                    .matches(HEX_PATTERN);
        }

        @Test
        @DisplayName("should generate identical hashes when same input and salt are used")
        void shouldGenerateIdenticalHashes_whenSameInputAndSaltAreUsed() {
            // Given: Same input and salt

            // When: Generating hash twice with same input and salt
            String hash1 = hashingService.generateHashWithSalt(VALID_INPUT, VALID_SALT_BASE64);
            String hash2 = hashingService.generateHashWithSalt(VALID_INPUT, VALID_SALT_BASE64);

            // Then: Hashes should be identical (deterministic with same salt)
            assertThat(hash1).isEqualTo(hash2);
        }

        @Test
        @DisplayName("should generate different hashes when different salts are used")
        void shouldGenerateDifferentHashes_whenDifferentSaltsAreUsed() {
            // Given: Same input but different salts

            // When: Generating hashes with different salts
            String hash1 = hashingService.generateHashWithSalt(VALID_INPUT, "abcdefghijklmnopqrstuv==");
            String hash2 = hashingService.generateHashWithSalt(VALID_INPUT, "zyxwvutsrqponmlkjihgfe==");

            // Then: Hashes should be different
            assertThat(hash1).isNotEqualTo(hash2);
        }

        @Test
        @DisplayName("should throw HashingFailureException when input is null")
        void shouldThrowHashingFailureException_whenInputIsNull() {
            // Given: Null input with valid salt

            // When/Then: Generating hash should throw exception
            assertThatThrownBy(() -> hashingService.generateHashWithSalt(null, VALID_SALT_BASE64))
                    .isInstanceOf(HashingFailureException.class)
                    .hasMessage(HashingService.ERROR_INPUT_NULL_OR_EMPTY);
        }

        @Test
        @DisplayName("should throw HashingFailureException when input is empty string")
        void shouldThrowHashingFailureException_whenInputIsEmptyString() {
            // Given: Empty string input with valid salt

            // When/Then: Generating hash should throw exception
            assertThatThrownBy(() -> hashingService.generateHashWithSalt("", VALID_SALT_BASE64))
                    .isInstanceOf(HashingFailureException.class)
                    .hasMessage(HashingService.ERROR_INPUT_NULL_OR_EMPTY);
        }

        @Test
        @DisplayName("should throw HashingFailureException when input is whitespace only")
        void shouldThrowHashingFailureException_whenInputIsWhitespaceOnly() {
            // Given: Whitespace-only input with valid salt

            // When/Then: Generating hash should throw exception
            assertThatThrownBy(() -> hashingService.generateHashWithSalt(WHITESPACE_ONLY, VALID_SALT_BASE64))
                    .isInstanceOf(HashingFailureException.class)
                    .hasMessage(HashingService.ERROR_INPUT_NULL_OR_EMPTY);
        }

        @Test
        @DisplayName("should throw HashingFailureException when salt is null")
        void shouldThrowHashingFailureException_whenSaltIsNull() {
            // Given: Valid input with null salt

            // When/Then: Generating hash should throw exception
            assertThatThrownBy(() -> hashingService.generateHashWithSalt(VALID_INPUT, null))
                    .isInstanceOf(HashingFailureException.class)
                    .hasMessage(HashingService.ERROR_SALT_NULL);
        }

        @Test
        @DisplayName("should throw HashingFailureException when salt is empty string")
        void shouldThrowHashingFailureException_whenSaltIsEmptyString() {
            // Given: Valid input with empty salt

            // When/Then: Generating hash should throw exception
            assertThatThrownBy(() -> hashingService.generateHashWithSalt(VALID_INPUT, ""))
                    .isInstanceOf(HashingFailureException.class)
                    .hasMessage(HashingService.ERROR_SALT_NULL);
        }

        @Test
        @DisplayName("should throw HashingFailureException when salt is whitespace only")
        void shouldThrowHashingFailureException_whenSaltIsWhitespaceOnly() {
            // Given: Valid input with whitespace-only salt

            // When/Then: Generating hash should throw exception
            assertThatThrownBy(() -> hashingService.generateHashWithSalt(VALID_INPUT, WHITESPACE_ONLY))
                    .isInstanceOf(HashingFailureException.class)
                    .hasMessage(HashingService.ERROR_SALT_NULL);
        }

        @Test
        @DisplayName("should integrate correctly with generateSaltedHash output")
        void shouldIntegrateCorrectlyWithGenerateSaltedHashOutput_whenUsingSameInputAndSalt() {
            // Given: Input that was used to generate salted hash
            HashingService.SaltedHash saltedHash = hashingService.generateSaltedHash(VALID_INPUT);

            // When: Regenerating hash with the same salt
            String regeneratedHash = hashingService.generateHashWithSalt(VALID_INPUT, saltedHash.salt());

            // Then: Regenerated hash should match original
            assertThat(regeneratedHash).isEqualTo(saltedHash.hash());
        }
    }

    @Nested
    @DisplayName("verifyHash()")
    class VerifyHashTests {

        private static final String VALID_SALT_BASE64 = "abcdefghijklmnopqrstuv==";

        @Test
        @DisplayName("should return true when input matches expected hash with correct salt")
        void shouldReturnTrue_whenInputMatchesExpectedHashWithCorrectSalt() {
            // Given: Input, salt, and expected hash that match
            String expectedHash = hashingService.generateHashWithSalt(VALID_INPUT, VALID_SALT_BASE64);

            // When: Verifying hash
            boolean result = hashingService.verifyHash(VALID_INPUT, VALID_SALT_BASE64, expectedHash);

            // Then: Verification should succeed
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("should return false when input does not match expected hash")
        void shouldReturnFalse_whenInputDoesNotMatchExpectedHash() {
            // Given: Input that doesn't match the expected hash
            String expectedHash = hashingService.generateHashWithSalt(ANOTHER_VALID_INPUT, VALID_SALT_BASE64);

            // When: Verifying hash with wrong input
            boolean result = hashingService.verifyHash(VALID_INPUT, VALID_SALT_BASE64, expectedHash);

            // Then: Verification should fail
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("should return false when salt is different from original")
        void shouldReturnFalse_whenSaltIsDifferentFromOriginal() {
            // Given: Input and hash generated with one salt
            String expectedHash = hashingService.generateHashWithSalt(VALID_INPUT, "abcdefghijklmnopqrstuv==");

            // When: Verifying with different salt
            boolean result = hashingService.verifyHash(VALID_INPUT, "zyxwvutsrqponmlkjihgfe==", expectedHash);

            // Then: Verification should fail
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("should work correctly with generateSaltedHash workflow")
        void shouldWorkCorrectlyWithGenerateSaltedHashWorkflow_whenVerifyingOriginalInput() {
            // Given: Original input and generated salted hash
            HashingService.SaltedHash saltedHash = hashingService.generateSaltedHash(VALID_INPUT);

            // When: Verifying with original input
            boolean result = hashingService.verifyHash(
                    VALID_INPUT,
                    saltedHash.salt(),
                    saltedHash.hash()
            );

            // Then: Verification should succeed
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("should return false when expected hash is modified")
        void shouldReturnFalse_whenExpectedHashIsModified() {
            // Given: Input and salt with modified expected hash
            String validHash = hashingService.generateHashWithSalt(VALID_INPUT, VALID_SALT_BASE64);
            String modifiedHash = validHash.substring(0, validHash.length() - 1) + "0";

            // When: Verifying with modified hash
            boolean result = hashingService.verifyHash(VALID_INPUT, VALID_SALT_BASE64, modifiedHash);

            // Then: Verification should fail
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("should use constant-time comparison to prevent timing attacks")
        void shouldUseConstantTimeComparison_whenComparingHashes() {
            // Given: Two different inputs with same salt
            String hash2 = hashingService.generateHashWithSalt(ANOTHER_VALID_INPUT, VALID_SALT_BASE64);

            // When: Verifying wrong input (should fail)
            boolean result = hashingService.verifyHash(VALID_INPUT, VALID_SALT_BASE64, hash2);

            // Then: Should return false (timing should be constant regardless of where difference occurs)
            assertThat(result).isFalse();
            // Note: Actual timing attack resistance requires specialized testing tools,
            // but the implementation uses XOR-based constant-time comparison
        }

        @Test
        @DisplayName("should throw HashingFailureException when input is null")
        void shouldThrowHashingFailureException_whenInputIsNull() {
            // Given: Null input with valid salt and hash

            // When/Then: Verifying should throw exception
            assertThatThrownBy(() -> hashingService.verifyHash(null, VALID_SALT_BASE64, "dummy-hash"))
                    .isInstanceOf(HashingFailureException.class)
                    .hasMessage(HashingService.ERROR_INPUT_NULL_OR_EMPTY);
        }

        @Test
        @DisplayName("should throw HashingFailureException when input is empty string")
        void shouldThrowHashingFailureException_whenInputIsEmptyString() {
            // Given: Empty string input with valid salt and hash

            // When/Then: Verifying should throw exception
            assertThatThrownBy(() -> hashingService.verifyHash("", VALID_SALT_BASE64, "dummy-hash"))
                    .isInstanceOf(HashingFailureException.class)
                    .hasMessage(HashingService.ERROR_INPUT_NULL_OR_EMPTY);
        }

        @Test
        @DisplayName("should throw HashingFailureException when input is whitespace only")
        void shouldThrowHashingFailureException_whenInputIsWhitespaceOnly() {
            // Given: Whitespace-only input with valid salt and hash

            // When/Then: Verifying should throw exception
            assertThatThrownBy(() -> hashingService.verifyHash(WHITESPACE_ONLY, VALID_SALT_BASE64, "dummy-hash"))
                    .isInstanceOf(HashingFailureException.class)
                    .hasMessage(HashingService.ERROR_INPUT_NULL_OR_EMPTY);
        }

        @Test
        @DisplayName("should throw HashingFailureException when salt is null")
        void shouldThrowHashingFailureException_whenSaltIsNull() {
            // Given: Valid input with null salt

            // When/Then: Verifying should throw exception
            assertThatThrownBy(() -> hashingService.verifyHash(VALID_INPUT, null, "dummy-hash"))
                    .isInstanceOf(HashingFailureException.class)
                    .hasMessage(HashingService.ERROR_SALT_NULL);
        }

        @Test
        @DisplayName("should throw HashingFailureException when salt is empty string")
        void shouldThrowHashingFailureException_whenSaltIsEmptyString() {
            // Given: Valid input with empty salt

            // When/Then: Verifying should throw exception
            assertThatThrownBy(() -> hashingService.verifyHash(VALID_INPUT, "", "dummy-hash"))
                    .isInstanceOf(HashingFailureException.class)
                    .hasMessage(HashingService.ERROR_SALT_NULL);
        }

        @Test
        @DisplayName("should throw HashingFailureException when salt is whitespace only")
        void shouldThrowHashingFailureException_whenSaltIsWhitespaceOnly() {
            // Given: Valid input with whitespace-only salt

            // When/Then: Verifying should throw exception
            assertThatThrownBy(() -> hashingService.verifyHash(VALID_INPUT, WHITESPACE_ONLY, "dummy-hash"))
                    .isInstanceOf(HashingFailureException.class)
                    .hasMessage(HashingService.ERROR_SALT_NULL);
        }

        @Test
        @DisplayName("should throw HashingFailureException when expected hash is null")
        void shouldThrowHashingFailureException_whenExpectedHashIsNull() {
            // Given: Valid input and salt with null expected hash

            // When/Then: Verifying should throw exception
            assertThatThrownBy(() -> hashingService.verifyHash(VALID_INPUT, VALID_SALT_BASE64, null))
                    .isInstanceOf(HashingFailureException.class)
                    .hasMessage(HashingService.ERROR_INPUT_NULL_OR_EMPTY);
        }

        @Test
        @DisplayName("should throw HashingFailureException when expected hash is empty string")
        void shouldThrowHashingFailureException_whenExpectedHashIsEmptyString() {
            // Given: Valid input and salt with empty expected hash

            // When/Then: Verifying should throw exception
            assertThatThrownBy(() -> hashingService.verifyHash(VALID_INPUT, VALID_SALT_BASE64, ""))
                    .isInstanceOf(HashingFailureException.class)
                    .hasMessage(HashingService.ERROR_INPUT_NULL_OR_EMPTY);
        }

        @Test
        @DisplayName("should throw HashingFailureException when expected hash is whitespace only")
        void shouldThrowHashingFailureException_whenExpectedHashIsWhitespaceOnly() {
            // Given: Valid input and salt with whitespace-only expected hash

            // When/Then: Verifying should throw exception
            assertThatThrownBy(() -> hashingService.verifyHash(VALID_INPUT, VALID_SALT_BASE64, WHITESPACE_ONLY))
                    .isInstanceOf(HashingFailureException.class)
                    .hasMessage(HashingService.ERROR_INPUT_NULL_OR_EMPTY);
        }
    }

    @Nested
    @DisplayName("SaltedHash record")
    class SaltedHashRecordTests {

        @Test
        @DisplayName("should store hash and salt correctly")
        void shouldStoreHashAndSaltCorrectly_whenRecordIsCreated() {
            // Given: Hash and salt values

            // When: Creating SaltedHash record
            HashingService.SaltedHash saltedHash = new HashingService.SaltedHash("abc123def456", "salt123");

            // Then: Values should be accessible
            assertThat(saltedHash.hash()).isEqualTo("abc123def456");
            assertThat(saltedHash.salt()).isEqualTo("salt123");
        }

        @Test
        @DisplayName("should implement equals correctly for records")
        void shouldImplementEqualsCorrectly_whenComparingRecords() {
            // Given: Two identical records
            HashingService.SaltedHash record1 = new HashingService.SaltedHash("abc123", "salt456");
            HashingService.SaltedHash record2 = new HashingService.SaltedHash("abc123", "salt456");

            // When/Then: Records should be equal
            assertThat(record1).isEqualTo(record2);
        }

        @Test
        @DisplayName("should implement hashCode correctly for records")
        void shouldImplementHashCodeCorrectly_whenComparingRecords() {
            // Given: Two identical records
            HashingService.SaltedHash record1 = new HashingService.SaltedHash("abc123", "salt456");
            HashingService.SaltedHash record2 = new HashingService.SaltedHash("abc123", "salt456");

            // When/Then: Hash codes should be equal
            assertThat(record1.hashCode()).isEqualTo(record2.hashCode());
        }
    }
}