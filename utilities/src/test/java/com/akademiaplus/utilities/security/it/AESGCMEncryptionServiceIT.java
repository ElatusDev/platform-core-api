package com.akademiaplus.utilities.security.it;

import com.akademiaplus.utilities.exceptions.security.DecryptionFailureException;
import com.akademiaplus.utilities.exceptions.security.EncryptionFailureException;
import com.akademiaplus.utilities.security.AESGCMEncryptionService;
import com.akademiaplus.utilities.security.it.conf.TestConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = {TestConfig.class})
@ActiveProfiles("test")
@DisplayName("AESGCMEncryptionService Integration Tests")
class AESGCMEncryptionServiceIT {

        @Autowired
        private AESGCMEncryptionService encryptionService;

        @Test
        @DisplayName("should perform a successful encryption and decryption round trip for a string")
        void encryptAndDecrypt_String_Success() {
            String originalPlaintext = "This is a test message with special characters: ñ, á, é.";

            // Ensure that the original string is correctly converted to bytes
            byte[] originalBytes = originalPlaintext.getBytes(StandardCharsets.UTF_8);

            try {
                // Encrypt
                String encryptedBase64 = encryptionService.encrypt(originalBytes);
                assertNotNull(encryptedBase64);
                assertTrue(encryptedBase64.length() > originalPlaintext.length()); // Ciphertext should be longer than plaintext

                // Decrypt
                String decryptedPlaintext = encryptionService.decrypt(encryptedBase64);
                assertNotNull(decryptedPlaintext);

                // Assert that the decrypted string matches the original
                assertEquals(originalPlaintext, decryptedPlaintext);

            } catch (EncryptionFailureException | DecryptionFailureException e) {
                fail("Encryption/Decryption failed with an unexpected exception: " + e.getMessage());
            }
        }

        @Test
        @DisplayName("should throw DecryptionFailureException on tag mismatch due to tampering")
        void decrypt_TamperedData_ThrowsDecryptionFailureException() throws DecryptionFailureException {
            String originalPlaintext = "Sensitive data that will be tampered with.";
            String encryptedBase64 = encryptionService.encrypt(originalPlaintext.getBytes(StandardCharsets.UTF_8));

            // Tamper with the ciphertext by changing a single character
            String tamperedBase64 = encryptedBase64.substring(0, encryptedBase64.length() - 5) + "A" + encryptedBase64.substring(encryptedBase64.length() - 4);

            assertThrows(DecryptionFailureException.class, () -> {
                encryptionService.decrypt(tamperedBase64);
            });
        }

        @Test
        @DisplayName("should throw DecryptionFailureException if key is invalid")
        void decrypt_InvalidKey_ThrowsDecryptionFailureException() throws DecryptionFailureException {
            String originalPlaintext = "This message will be decrypted with an invalid key.";
            String encryptedBase64 = encryptionService.encrypt(originalPlaintext.getBytes(StandardCharsets.UTF_8));

            String invalidBase64Key = "MzIzNDU2Nzg5MDEyMzQ1Njc4OTAxMjM0NTY3ODkwMTI=";
            AESGCMEncryptionService invalidKeyService = new AESGCMEncryptionService(invalidBase64Key);

            assertThrows(DecryptionFailureException.class, () -> {
                invalidKeyService.decrypt(encryptedBase64);
            });
        }

        @Test
        @DisplayName("should correctly decrypt data encrypted by encrypt(byte[])")
        void encryptAndDecrypt_ByteArrays_Success() {
            byte[] originalData = "Binary data to be encrypted".getBytes(StandardCharsets.UTF_8);

            // Your service has two similar `decrypt` methods.
            // Let's test the `decrypt(byte[])` method

            // Encrypt data
            String encryptedBase64 = encryptionService.encrypt(originalData);
            byte[] encryptedBytes = Base64.getDecoder().decode(encryptedBase64);

            try {
                // Decrypt the byte array using the correct decrypt method
                byte[] decryptedBytes = encryptionService.decrypt(encryptedBytes);
                assertArrayEquals(originalData, decryptedBytes);
            } catch (DecryptionFailureException e) {
                fail("Decryption failed unexpectedly: " + e.getMessage());
            }
        }
}
