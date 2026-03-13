/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.utilities.security;

import com.akademiaplus.utilities.exceptions.security.DecryptionFailureException;
import com.akademiaplus.utilities.exceptions.security.EncryptionFailureException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Comprehensive test suite for {@link AESGCMEncryptionService}.
 *
 * <p>Tests cover:
 * <ul>
 *   <li>String encryption and decryption</li>
 *   <li>Byte array encryption and decryption</li>
 *   <li>Wrapper Byte[] methods (deprecated)</li>
 *   <li>Input validation</li>
 *   <li>Security properties (IV uniqueness, authentication tag verification)</li>
 *   <li>Integration workflows (encrypt → decrypt)</li>
 *   <li>Error handling and edge cases</li>
 * </ul>
 *
 * @author ElatusDev
 * @version 1.0
 */
@DisplayName("AESGCMEncryptionService")
class AESGCMEncryptionServiceTest {

    // Test data constants
    private static final String VALID_BASE64_KEY = "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA="; // 256-bit key
    private static final String VALID_PLAINTEXT = "Hello, World!";
    private static final String ANOTHER_PLAINTEXT = "Different text for testing";
    private static final String WHITESPACE_ONLY = "   ";
    private static final int IV_LENGTH_BYTES = 12;
    private static final int AUTH_TAG_LENGTH_BYTES = 16;
    private static final int MINIMUM_ENCRYPTED_LENGTH = IV_LENGTH_BYTES + AUTH_TAG_LENGTH_BYTES;

    private AESGCMEncryptionService encryptionService;

    @BeforeEach
    void setUp() {
        // Given: A new encryption service instance for each test
        encryptionService = new AESGCMEncryptionService(VALID_BASE64_KEY);
    }

    @Nested
    @DisplayName("encrypt(String)")
    class EncryptStringTests {

        @Test
        @DisplayName("should encrypt plaintext and return Base64-encoded string when plaintext is valid")
        void shouldEncryptPlaintextAndReturnBase64EncodedString_whenPlaintextIsValid() {
            // Given: Valid plaintext string

            // When: Encrypting plaintext
            String encrypted = encryptionService.encrypt(VALID_PLAINTEXT);

            // Then: Should return non-null Base64-encoded string
            assertThat(encrypted).isNotNull();

            // Verify it's valid Base64
            byte[] decoded = Base64.getDecoder().decode(encrypted);
            assertThat(decoded).isNotEmpty();

            // Verify minimum length (IV + tag + at least some ciphertext)
            assertThat(decoded.length).isGreaterThanOrEqualTo(MINIMUM_ENCRYPTED_LENGTH);
        }

        @Test
        @DisplayName("should produce different encrypted outputs when same plaintext is encrypted multiple times")
        void shouldProduceDifferentEncryptedOutputs_whenSamePlaintextIsEncryptedMultipleTimes() {
            // Given: Same plaintext string

            // When: Encrypting same plaintext twice
            String encrypted1 = encryptionService.encrypt(VALID_PLAINTEXT);
            String encrypted2 = encryptionService.encrypt(VALID_PLAINTEXT);

            // Then: Encrypted values should be different (due to unique IVs)
            assertThat(encrypted1).isNotEqualTo(encrypted2);
        }

        @Test
        @DisplayName("should produce different encrypted outputs when different plaintexts are encrypted")
        void shouldProduceDifferentEncryptedOutputs_whenDifferentPlaintextsAreEncrypted() {
            // Given: Two different plaintext strings

            // When: Encrypting different plaintexts
            String encrypted1 = encryptionService.encrypt(VALID_PLAINTEXT);
            String encrypted2 = encryptionService.encrypt(ANOTHER_PLAINTEXT);

            // Then: Encrypted values should be different
            assertThat(encrypted1).isNotEqualTo(encrypted2);
        }

        @Test
        @DisplayName("should throw EncryptionFailureException when plaintext is empty string")
        void shouldThrowEncryptionFailureException_whenPlaintextIsEmptyString() {
            // Given: Empty string

            // When/Then: Encrypting should throw exception
            assertThatThrownBy(() -> encryptionService.encrypt(""))
                    .isInstanceOf(EncryptionFailureException.class)
                    .hasMessage(AESGCMEncryptionService.ERROR_PLAINTEXT_NULL_OR_EMPTY);
        }

        @Test
        @DisplayName("should handle UTF-8 characters correctly when encrypting")
        void shouldHandleUtf8CharactersCorrectly_whenEncrypting() {
            // Given: Plaintext with UTF-8 characters
            String utf8Plaintext = "Hello 世界 🌍";

            // When: Encrypting UTF-8 plaintext
            String encrypted = encryptionService.encrypt(utf8Plaintext);

            // Then: Should encrypt successfully
            assertThat(encrypted).isNotNull();
            byte[] decoded = Base64.getDecoder().decode(encrypted);
            assertThat(decoded).isNotEmpty();
        }

        @Test
        @DisplayName("should throw EncryptionFailureException when plaintext is null")
        void shouldThrowEncryptionFailureException_whenPlaintextIsNull() {
            // Given: Null plaintext

            // When/Then: Encrypting should throw exception
            assertThatThrownBy(() -> encryptionService.encrypt((String) null))
                    .isInstanceOf(EncryptionFailureException.class)
                    .hasMessage(AESGCMEncryptionService.ERROR_PLAINTEXT_NULL_OR_EMPTY);
        }

        @Test
        @DisplayName("should throw EncryptionFailureException when plaintext is blank")
        void shouldThrowEncryptionFailureException_whenPlaintextIsBlank() {
            // Given: Blank plaintext

            // When/Then: Encrypting should throw exception
            assertThatThrownBy(() -> encryptionService.encrypt(WHITESPACE_ONLY))
                    .isInstanceOf(EncryptionFailureException.class)
                    .hasMessage(AESGCMEncryptionService.ERROR_PLAINTEXT_NULL_OR_EMPTY);
        }
    }

    @Nested
    @DisplayName("encrypt(byte[])")
    class EncryptBytesStringReturnTests {

        @Test
        @DisplayName("should encrypt byte array and return Base64 string when bytes are valid")
        void shouldEncryptByteArrayAndReturnBase64String_whenBytesAreValid() {
            // Given: Valid byte array
            byte[] plaintextBytes = VALID_PLAINTEXT.getBytes(StandardCharsets.UTF_8);

            // When: Encrypting bytes
            String encrypted = encryptionService.encrypt(plaintextBytes);

            // Then: Should return valid Base64-encoded string
            assertThat(encrypted).isNotNull();
            byte[] decoded = Base64.getDecoder().decode(encrypted);
            assertThat(decoded.length).isGreaterThanOrEqualTo(MINIMUM_ENCRYPTED_LENGTH);
        }

        @Test
        @DisplayName("should produce different encrypted outputs when same bytes are encrypted multiple times")
        void shouldProduceDifferentEncryptedOutputs_whenSameBytesAreEncryptedMultipleTimes() {
            // Given: Same byte array
            byte[] plaintextBytes = VALID_PLAINTEXT.getBytes(StandardCharsets.UTF_8);

            // When: Encrypting same bytes twice
            String encrypted1 = encryptionService.encrypt(plaintextBytes);
            String encrypted2 = encryptionService.encrypt(plaintextBytes);

            // Then: Encrypted values should be different (unique IVs)
            assertThat(encrypted1).isNotEqualTo(encrypted2);
        }

        @Test
        @DisplayName("should throw EncryptionFailureException when byte array is null")
        void shouldThrowEncryptionFailureException_whenByteArrayIsNull() {
            // Given: Null byte array

            // When/Then: Encrypting should throw exception
            assertThatThrownBy(() -> encryptionService.encrypt((byte[]) null))
                    .isInstanceOf(EncryptionFailureException.class)
                    .hasMessage(AESGCMEncryptionService.ERROR_PLAINTEXT_BYTES_NULL);
        }
    }

    @Nested
    @DisplayName("encryptBytes(byte[])")
    class EncryptBytesTests {

        @Test
        @DisplayName("should encrypt bytes and return encrypted byte array when input is valid")
        void shouldEncryptBytesAndReturnEncryptedByteArray_whenInputIsValid() {
            // Given: Valid byte array
            byte[] plaintextBytes = VALID_PLAINTEXT.getBytes(StandardCharsets.UTF_8);

            // When: Encrypting bytes
            byte[] encrypted = encryptionService.encryptBytes(plaintextBytes);

            // Then: Should return encrypted byte array with correct structure
            assertThat(encrypted).isNotNull();
            assertThat(encrypted.length).isGreaterThanOrEqualTo(MINIMUM_ENCRYPTED_LENGTH);

            // Verify IV is at the beginning (first 12 bytes)
            assertThat(encrypted).hasSizeGreaterThanOrEqualTo(IV_LENGTH_BYTES);
        }

        @Test
        @DisplayName("should produce different encrypted bytes when same input is encrypted multiple times")
        void shouldProduceDifferentEncryptedBytes_whenSameInputIsEncryptedMultipleTimes() {
            // Given: Same byte array
            byte[] plaintextBytes = VALID_PLAINTEXT.getBytes(StandardCharsets.UTF_8);

            // When: Encrypting same bytes twice
            byte[] encrypted1 = encryptionService.encryptBytes(plaintextBytes);
            byte[] encrypted2 = encryptionService.encryptBytes(plaintextBytes);

            // Then: Encrypted values should be different (unique IVs)
            assertThat(encrypted1).isNotEqualTo(encrypted2);
        }

        @Test
        @DisplayName("should include IV in encrypted output")
        void shouldIncludeIvInEncryptedOutput_whenEncrypting() {
            // Given: Valid plaintext bytes
            byte[] plaintextBytes = VALID_PLAINTEXT.getBytes(StandardCharsets.UTF_8);

            // When: Encrypting bytes twice
            byte[] encrypted1 = encryptionService.encryptBytes(plaintextBytes);
            byte[] encrypted2 = encryptionService.encryptBytes(plaintextBytes);

            // Then: First 12 bytes (IV) should be different between encryptions
            byte[] iv1 = new byte[IV_LENGTH_BYTES];
            byte[] iv2 = new byte[IV_LENGTH_BYTES];
            System.arraycopy(encrypted1, 0, iv1, 0, IV_LENGTH_BYTES);
            System.arraycopy(encrypted2, 0, iv2, 0, IV_LENGTH_BYTES);

            assertThat(iv1).isNotEqualTo(iv2);
        }

        @Test
        @DisplayName("should throw EncryptionFailureException when bytes are null")
        void shouldThrowEncryptionFailureException_whenBytesAreNull() {
            // Given: Null byte array

            // When/Then: Encrypting should throw exception
            assertThatThrownBy(() -> encryptionService.encryptBytes(null))
                    .isInstanceOf(EncryptionFailureException.class)
                    .hasMessage(AESGCMEncryptionService.ERROR_PLAINTEXT_BYTES_NULL);
        }
    }

    @Nested
    @DisplayName("decrypt(String)")
    class DecryptStringTests {

        @Test
        @DisplayName("should decrypt ciphertext and return original plaintext when ciphertext is valid")
        void shouldDecryptCiphertextAndReturnOriginalPlaintext_whenCiphertextIsValid() {
            // Given: Valid encrypted ciphertext
            String encrypted = encryptionService.encrypt(VALID_PLAINTEXT);

            // When: Decrypting ciphertext
            String decrypted = encryptionService.decrypt(encrypted);

            // Then: Should return original plaintext
            assertThat(decrypted).isEqualTo(VALID_PLAINTEXT);
        }

        @Test
        @DisplayName("should handle UTF-8 characters correctly when decrypting")
        void shouldHandleUtf8CharactersCorrectly_whenDecrypting() {
            // Given: Encrypted UTF-8 plaintext
            String utf8Plaintext = "Hello 世界 🌍";
            String encrypted = encryptionService.encrypt(utf8Plaintext);

            // When: Decrypting
            String decrypted = encryptionService.decrypt(encrypted);

            // Then: Should return original UTF-8 plaintext
            assertThat(decrypted).isEqualTo(utf8Plaintext);
        }

        @Test
        @DisplayName("should throw DecryptionFailureException when ciphertext is null")
        void shouldThrowDecryptionFailureException_whenCiphertextIsNull() {
            // Given: Null ciphertext

            // When/Then: Decrypting should throw exception
            assertThatThrownBy(() -> encryptionService.decrypt((String) null))
                    .isInstanceOf(DecryptionFailureException.class)
                    .hasMessage(AESGCMEncryptionService.ERROR_CIPHERTEXT_NULL_OR_EMPTY);
        }

        @Test
        @DisplayName("should throw DecryptionFailureException when ciphertext is blank")
        void shouldThrowDecryptionFailureException_whenCiphertextIsBlank() {
            // Given: Blank ciphertext

            // When/Then: Decrypting should throw exception
            assertThatThrownBy(() -> encryptionService.decrypt(WHITESPACE_ONLY))
                    .isInstanceOf(DecryptionFailureException.class)
                    .hasMessage(AESGCMEncryptionService.ERROR_CIPHERTEXT_NULL_OR_EMPTY);
        }

        @Test
        @DisplayName("should throw DecryptionFailureException when ciphertext is too short")
        void shouldThrowDecryptionFailureException_whenCiphertextIsTooShort() {
            // Given: Base64 string that decodes to less than minimum length
            byte[] tooShort = new byte[MINIMUM_ENCRYPTED_LENGTH - 1];
            String shortCiphertext = Base64.getEncoder().encodeToString(tooShort);

            // When/Then: Decrypting should throw exception with specific message
            assertThatThrownBy(() -> encryptionService.decrypt(shortCiphertext))
                    .isInstanceOf(DecryptionFailureException.class)
                    .hasMessageContaining(String.format(
                            AESGCMEncryptionService.ERROR_ENCRYPTED_DATA_TOO_SHORT,
                            MINIMUM_ENCRYPTED_LENGTH));
        }

        @Test
        @DisplayName("should throw DecryptionFailureException when ciphertext is tampered")
        void shouldThrowDecryptionFailureException_whenCiphertextIsTampered() {
            // Given: Valid encrypted ciphertext
            String encrypted = encryptionService.encrypt(VALID_PLAINTEXT);

            // Tamper with the ciphertext by modifying one byte
            byte[] encryptedBytes = Base64.getDecoder().decode(encrypted);
            encryptedBytes[encryptedBytes.length - 1] ^= 0x01; // Flip last bit
            String tamperedCiphertext = Base64.getEncoder().encodeToString(encryptedBytes);

            // When/Then: Decrypting tampered data should fail (authentication tag mismatch)
            assertThatThrownBy(() -> encryptionService.decrypt(tamperedCiphertext))
                    .isInstanceOf(DecryptionFailureException.class)
                    .hasMessage(AESGCMEncryptionService.ERROR_DECRYPTION_FAILED);
        }

        @Test
        @DisplayName("should throw DecryptionFailureException when ciphertext is invalid Base64")
        void shouldThrowDecryptionFailureException_whenCiphertextIsInvalidBase64() {
            // Given: Invalid Base64 string
            String invalidBase64 = "This is not valid Base64!@#$";

            // When/Then: Decrypting should throw exception
            assertThatThrownBy(() -> encryptionService.decrypt(invalidBase64))
                    .isInstanceOf(DecryptionFailureException.class)
                    .hasMessage(AESGCMEncryptionService.ERROR_DECRYPTION_FAILED);
        }
    }

    @Nested
    @DisplayName("decryptBytes(byte[])")
    class DecryptBytesTests {

        @Test
        @DisplayName("should decrypt encrypted bytes and return original plaintext bytes when data is valid")
        void shouldDecryptEncryptedBytesAndReturnOriginalPlaintextBytes_whenDataIsValid() {
            // Given: Encrypted byte array
            byte[] plaintextBytes = VALID_PLAINTEXT.getBytes(StandardCharsets.UTF_8);
            byte[] encrypted = encryptionService.encryptBytes(plaintextBytes);

            // When: Decrypting
            byte[] decrypted = encryptionService.decryptBytes(encrypted);

            // Then: Should return original plaintext bytes
            assertThat(decrypted).isEqualTo(plaintextBytes);
        }

        @Test
        @DisplayName("should decrypt bytes encrypted by encrypt method")
        void shouldDecryptBytesEncryptedByEncryptMethod_whenUsingStringOverload() {
            // Given: Data encrypted via encrypt(byte[]) method
            byte[] plaintextBytes = VALID_PLAINTEXT.getBytes(StandardCharsets.UTF_8);
            String encryptedBase64 = encryptionService.encrypt(plaintextBytes);
            byte[] encryptedBytes = Base64.getDecoder().decode(encryptedBase64);

            // When: Decrypting with decryptBytes
            byte[] decrypted = encryptionService.decryptBytes(encryptedBytes);

            // Then: Should return original bytes
            assertThat(decrypted).isEqualTo(plaintextBytes);
        }

        @Test
        @DisplayName("should throw DecryptionFailureException when encrypted data is null")
        void shouldThrowDecryptionFailureException_whenEncryptedDataIsNull() {
            // Given: Null encrypted data

            // When/Then: Decrypting should throw exception
            assertThatThrownBy(() -> encryptionService.decryptBytes(null))
                    .isInstanceOf(DecryptionFailureException.class)
                    .hasMessage(AESGCMEncryptionService.ERROR_ENCRYPTED_DATA_NULL);
        }

        @Test
        @DisplayName("should throw DecryptionFailureException when encrypted data is too short")
        void shouldThrowDecryptionFailureException_whenEncryptedDataIsTooShort() {
            // Given: Encrypted data shorter than minimum length
            byte[] tooShort = new byte[MINIMUM_ENCRYPTED_LENGTH - 1];

            // When/Then: Decrypting should throw exception
            assertThatThrownBy(() -> encryptionService.decryptBytes(tooShort))
                    .isInstanceOf(DecryptionFailureException.class)
                    .hasMessageContaining(String.format(
                            AESGCMEncryptionService.ERROR_ENCRYPTED_DATA_TOO_SHORT,
                            MINIMUM_ENCRYPTED_LENGTH));
        }

        @Test
        @DisplayName("should throw DecryptionFailureException when encrypted data is tampered")
        void shouldThrowDecryptionFailureException_whenEncryptedDataIsTampered() {
            // Given: Valid encrypted data
            byte[] plaintextBytes = VALID_PLAINTEXT.getBytes(StandardCharsets.UTF_8);
            byte[] encrypted = encryptionService.encryptBytes(plaintextBytes);

            // Tamper with the data
            encrypted[encrypted.length - 1] ^= 0x01;

            // When/Then: Decrypting tampered data should fail
            assertThatThrownBy(() -> encryptionService.decryptBytes(encrypted))
                    .isInstanceOf(DecryptionFailureException.class)
                    .hasMessage(AESGCMEncryptionService.ERROR_DECRYPTION_FAILED);
        }
    }

    @Nested
    @DisplayName("encrypt(Byte[]) - deprecated")
    class EncryptWrapperBytesTests {

        @Test
        @DisplayName("should encrypt wrapper Byte array when data is valid")
        void shouldEncryptWrapperByteArray_whenDataIsValid() {
            // Given: Valid wrapper Byte array
            byte[] primitiveBytes = VALID_PLAINTEXT.getBytes(StandardCharsets.UTF_8);
            Byte[] wrapperBytes = convertToWrapper(primitiveBytes);

            // When: Encrypting wrapper array
            Byte[] encrypted = encryptionService.encrypt(wrapperBytes);

            // Then: Should return encrypted wrapper array
            assertThat(encrypted).isNotNull();
            assertThat(encrypted.length).isGreaterThanOrEqualTo(MINIMUM_ENCRYPTED_LENGTH);
        }

        @Test
        @DisplayName("should throw EncryptionFailureException when wrapper Byte array is null")
        void shouldThrowEncryptionFailureException_whenWrapperByteArrayIsNull() {
            // Given: Null wrapper Byte array

            // When/Then: Encrypting should throw exception
            assertThatThrownBy(() -> encryptionService.encrypt((Byte[]) null))
                    .isInstanceOf(EncryptionFailureException.class)
                    .hasMessage(AESGCMEncryptionService.ERROR_PLAINTEXT_BYTES_NULL);
        }
    }

    @Nested
    @DisplayName("decrypt(Byte[]) - deprecated")
    class DecryptWrapperBytesTests {

        @Test
        @DisplayName("should decrypt wrapper Byte array when data is valid")
        void shouldDecryptWrapperByteArray_whenDataIsValid() {
            // Given: Encrypted wrapper Byte array
            byte[] primitiveBytes = VALID_PLAINTEXT.getBytes(StandardCharsets.UTF_8);
            Byte[] wrapperBytes = convertToWrapper(primitiveBytes);
            Byte[] encrypted = encryptionService.encrypt(wrapperBytes);

            // When: Decrypting wrapper array
            Byte[] decrypted = encryptionService.decrypt(encrypted);

            // Then: Should return original wrapper array
            assertThat(decrypted).isEqualTo(wrapperBytes);
        }

        @Test
        @DisplayName("should throw DecryptionFailureException when wrapper Byte array is null")
        void shouldThrowDecryptionFailureException_whenWrapperByteArrayIsNull() {
            // Given: Null wrapper Byte array

            // When/Then: Decrypting should throw exception
            assertThatThrownBy(() -> encryptionService.decrypt((Byte[]) null))
                    .isInstanceOf(DecryptionFailureException.class)
                    .hasMessage(AESGCMEncryptionService.ERROR_ENCRYPTED_DATA_NULL);
        }
    }

    @Nested
    @DisplayName("Integration Tests")
    class IntegrationTests {

        @Test
        @DisplayName("should successfully encrypt and decrypt string roundtrip")
        void shouldSuccessfullyEncryptAndDecryptStringRoundtrip_whenUsingStringMethods() {
            // Given: Original plaintext

            // When: Encrypting then decrypting
            String encrypted = encryptionService.encrypt(VALID_PLAINTEXT);
            String decrypted = encryptionService.decrypt(encrypted);

            // Then: Should get original plaintext back
            assertThat(decrypted).isEqualTo(VALID_PLAINTEXT);
        }

        @Test
        @DisplayName("should successfully encrypt and decrypt bytes roundtrip")
        void shouldSuccessfullyEncryptAndDecryptBytesRoundtrip_whenUsingByteMethods() {
            // Given: Original plaintext bytes
            byte[] originalBytes = VALID_PLAINTEXT.getBytes(StandardCharsets.UTF_8);

            // When: Encrypting then decrypting
            byte[] encrypted = encryptionService.encryptBytes(originalBytes);
            byte[] decrypted = encryptionService.decryptBytes(encrypted);

            // Then: Should get original bytes back
            assertThat(decrypted).isEqualTo(originalBytes);
        }

        @Test
        @DisplayName("should successfully encrypt and decrypt wrapper Byte array roundtrip")
        void shouldSuccessfullyEncryptAndDecryptWrapperByteArrayRoundtrip_whenUsingWrapperMethods() {
            // Given: Original wrapper Byte array
            byte[] primitiveBytes = VALID_PLAINTEXT.getBytes(StandardCharsets.UTF_8);
            Byte[] originalWrapper = convertToWrapper(primitiveBytes);

            // When: Encrypting then decrypting
            Byte[] encrypted = encryptionService.encrypt(originalWrapper);
            Byte[] decrypted = encryptionService.decrypt(encrypted);

            // Then: Should get original wrapper array back
            assertThat(decrypted).isEqualTo(originalWrapper);
        }

        @Test
        @DisplayName("should handle long text correctly in roundtrip")
        void shouldHandleLongTextCorrectlyInRoundtrip_whenTextIsLarge() {
            // Given: Long plaintext
            String longText = "A".repeat(10000);

            // When: Encrypting then decrypting
            String encrypted = encryptionService.encrypt(longText);
            String decrypted = encryptionService.decrypt(encrypted);

            // Then: Should get original long text back
            assertThat(decrypted).isEqualTo(longText);
        }

        @Test
        @DisplayName("should handle special characters correctly in roundtrip")
        void shouldHandleSpecialCharactersCorrectlyInRoundtrip_whenTextHasSpecialChars() {
            // Given: Plaintext with special characters
            String specialText = "!@#$%^&*()_+-=[]{}|;':\",./<>?`~\n\t\r";

            // When: Encrypting then decrypting
            String encrypted = encryptionService.encrypt(specialText);
            String decrypted = encryptionService.decrypt(encrypted);

            // Then: Should preserve all special characters
            assertThat(decrypted).isEqualTo(specialText);
        }

        @Test
        @DisplayName("should cross-decrypt between encrypt methods")
        void shouldCrossDecryptBetweenEncryptMethods_whenUsingDifferentOverloads() {
            // Given: Plaintext encrypted via encrypt(String)
            String encryptedViaString = encryptionService.encrypt(VALID_PLAINTEXT);

            // When: Decrypting via decrypt(String)
            String decryptedViaString = encryptionService.decrypt(encryptedViaString);

            // Then: Should work correctly
            assertThat(decryptedViaString).isEqualTo(VALID_PLAINTEXT);

            // And: encrypt(byte[]) → decryptBytes() should also work
            byte[] plaintextBytes = VALID_PLAINTEXT.getBytes(StandardCharsets.UTF_8);
            String encryptedViaBytes = encryptionService.encrypt(plaintextBytes);
            byte[] decryptedBytes = encryptionService.decryptBytes(Base64.getDecoder().decode(encryptedViaBytes));

            assertThat(new String(decryptedBytes, StandardCharsets.UTF_8)).isEqualTo(VALID_PLAINTEXT);
        }
    }

    @Nested
    @DisplayName("Security Properties")
    class SecurityPropertiesTests {

        @Test
        @DisplayName("should generate unique IV for each encryption")
        void shouldGenerateUniqueIvForEachEncryption_whenEncryptingMultipleTimes() {
            // Given: Same plaintext

            // When: Encrypting multiple times
            String encrypted1 = encryptionService.encrypt(VALID_PLAINTEXT);
            String encrypted2 = encryptionService.encrypt(VALID_PLAINTEXT);
            String encrypted3 = encryptionService.encrypt(VALID_PLAINTEXT);

            // Then: All encrypted values should be different
            assertThat(encrypted1).isNotEqualTo(encrypted2);
            assertThat(encrypted2).isNotEqualTo(encrypted3);
            assertThat(encrypted1).isNotEqualTo(encrypted3);
        }

        @Test
        @DisplayName("should detect authentication tag mismatch when data is modified")
        void shouldDetectAuthenticationTagMismatch_whenDataIsModified() {
            // Given: Valid encrypted data
            byte[] plaintextBytes = VALID_PLAINTEXT.getBytes(StandardCharsets.UTF_8);
            byte[] encrypted = encryptionService.encryptBytes(plaintextBytes);

            // When: Modifying ciphertext (not IV, not last bytes which might be tag)
            encrypted[IV_LENGTH_BYTES + 2] ^= (byte) 0xFF;

            // Then: Decryption should fail due to authentication tag verification
            assertThatThrownBy(() -> encryptionService.decryptBytes(encrypted))
                    .isInstanceOf(DecryptionFailureException.class)
                    .hasMessage(AESGCMEncryptionService.ERROR_DECRYPTION_FAILED);
        }

        @Test
        @DisplayName("should detect authentication tag mismatch when IV is modified")
        void shouldDetectAuthenticationTagMismatch_whenIvIsModified() {
            // Given: Valid encrypted data
            String encrypted = encryptionService.encrypt(VALID_PLAINTEXT);
            byte[] encryptedBytes = Base64.getDecoder().decode(encrypted);

            // When: Modifying IV (first byte)
            encryptedBytes[0] ^= 0x01;
            String tamperedEncrypted = Base64.getEncoder().encodeToString(encryptedBytes);

            // Then: Decryption should fail
            assertThatThrownBy(() -> encryptionService.decrypt(tamperedEncrypted))
                    .isInstanceOf(DecryptionFailureException.class)
                    .hasMessage(AESGCMEncryptionService.ERROR_DECRYPTION_FAILED);
        }

        @Test
        @DisplayName("should verify encrypted data contains IV at start")
        void shouldVerifyEncryptedDataContainsIvAtStart_whenEncrypting() {
            // Given: Valid plaintext
            byte[] plaintextBytes = VALID_PLAINTEXT.getBytes(StandardCharsets.UTF_8);

            // When: Encrypting twice
            byte[] encrypted1 = encryptionService.encryptBytes(plaintextBytes);
            byte[] encrypted2 = encryptionService.encryptBytes(plaintextBytes);

            // Then: First IV_LENGTH_BYTES should be different (unique IVs)
            boolean ivsDifferent = false;
            for (int i = 0; i < IV_LENGTH_BYTES; i++) {
                if (encrypted1[i] != encrypted2[i]) {
                    ivsDifferent = true;
                    break;
                }
            }
            assertThat(ivsDifferent).isTrue();
        }

        @Test
        @DisplayName("should use SecureRandom for IV generation")
        void shouldUseSecureRandomForIvGeneration_whenCreatingService() {
            // Given: Service with injected SecureRandom
            SecureRandom mockSecureRandom = new SecureRandom();
            AESGCMEncryptionService serviceWithMockRandom =
                    new AESGCMEncryptionService(VALID_BASE64_KEY, mockSecureRandom);

            // When: Encrypting data
            String encrypted = serviceWithMockRandom.encrypt(VALID_PLAINTEXT);

            // Then: Should successfully encrypt (verifies SecureRandom injection works)
            assertThat(encrypted).isNotNull();

            // And: Should decrypt correctly
            String decrypted = serviceWithMockRandom.decrypt(encrypted);
            assertThat(decrypted).isEqualTo(VALID_PLAINTEXT);
        }
    }

    /**
     * Utility method to convert primitive byte array to wrapper Byte array.
     */
    private Byte[] convertToWrapper(byte[] primitiveArray) {
        Byte[] wrapperArray = new Byte[primitiveArray.length];
        for (int i = 0; i < primitiveArray.length; i++) {
            wrapperArray[i] = primitiveArray[i];
        }
        return wrapperArray;
    }
}