/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.utilities.security;

import com.akademiaplus.utilities.exceptions.security.DecryptionFailureException;
import com.akademiaplus.utilities.exceptions.security.EncryptionFailureException;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Provides AES-GCM encryption and decryption services with authenticated encryption.
 *
 * <p>This service implements AES-GCM (Galois/Counter Mode) which provides both
 * confidentiality and authenticity/integrity of data. GCM mode automatically includes
 * an authentication tag that prevents tampering.
 *
 * <p><b>Security Features:</b>
 * <ul>
 *   <li>AES-256-GCM encryption (configurable key size)</li>
 *   <li>128-bit authentication tag for integrity verification</li>
 *   <li>Unique 96-bit (12-byte) IV for each encryption operation</li>
 *   <li>Cryptographically secure random IV generation</li>
 *   <li>Base64 encoding for safe text storage/transmission</li>
 * </ul>
 *
 * <p><b>Data Format:</b> Encrypted data is structured as: [IV (12 bytes)][Ciphertext][Auth Tag (16 bytes)]
 * and then Base64-encoded for string operations.
 *
 * <p><b>Thread Safety:</b> This service is thread-safe. Cipher instances are created
 * per invocation and SecureRandom is thread-safe.
 *
 * <p><b>Key Management:</b> The encryption key must be provided via Spring configuration
 * property {@code security.encryption-key} as a Base64-encoded string.
 *
 * @author ElatusDev
 * @version 2.0
 * @since 1.0
 */
@Service
public class AESGCMEncryptionService {

    // Error messages as public constants (referenced by tests)
    public static final String ERROR_PLAINTEXT_NULL_OR_EMPTY = "Plaintext cannot be null or empty";
    public static final String ERROR_PLAINTEXT_BYTES_NULL = "Plaintext bytes cannot be null";
    public static final String ERROR_CIPHERTEXT_NULL_OR_EMPTY = "Ciphertext cannot be null or empty";
    public static final String ERROR_ENCRYPTED_DATA_NULL = "Encrypted data cannot be null";
    public static final String ERROR_ENCRYPTED_DATA_TOO_SHORT = "Encrypted data is too short - minimum length is %d bytes (IV + tag)";
    public static final String ERROR_ENCRYPTION_FAILED = "Encryption failed";
    public static final String ERROR_DECRYPTION_FAILED = "Decryption failed";

    // Algorithm and cipher constants
    private static final String ALGORITHM_AES = "AES";
    private static final String CIPHER_TRANSFORMATION = "AES/GCM/NoPadding";

    // GCM parameters
    private static final int IV_LENGTH_BYTES = 12;  // 96 bits - recommended for GCM
    private static final int AUTH_TAG_LENGTH_BITS = 128;  // 128 bits - standard GCM tag
    private static final int AUTH_TAG_LENGTH_BYTES = AUTH_TAG_LENGTH_BITS / 8;  // 16 bytes
    private static final int MINIMUM_ENCRYPTED_DATA_LENGTH = IV_LENGTH_BYTES + AUTH_TAG_LENGTH_BYTES;

    private final SecretKeySpec secretKey;
    private final SecureRandom secureRandom;

    /**
     * Constructs a new AESGCMEncryptionService with the provided encryption key.
     *
     * @param base64Key the Base64-encoded encryption key from configuration
     */
    public AESGCMEncryptionService(@Value("${security.encryption-key}") String base64Key) {
        this(base64Key, new SecureRandom());
    }

    /**
     * Constructs a new AESGCMEncryptionService with the provided encryption key and SecureRandom.
     * This constructor is primarily for testing purposes.
     *
     * @param base64Key the Base64-encoded encryption key
     * @param secureRandom the SecureRandom instance for IV generation
     */
    AESGCMEncryptionService(String base64Key, SecureRandom secureRandom) {
        byte[] keyBytes = Base64.getDecoder().decode(base64Key);
        this.secretKey = new SecretKeySpec(keyBytes, ALGORITHM_AES);
        this.secureRandom = secureRandom;
    }

    /**
     * Encrypts a plaintext string using AES-GCM.
     *
     * <p>The encryption process:
     * <ol>
     *   <li>Validates input is not null or blank</li>
     *   <li>Converts string to UTF-8 bytes</li>
     *   <li>Generates a unique random IV</li>
     *   <li>Encrypts data with AES-GCM (produces ciphertext + auth tag)</li>
     *   <li>Prepends IV to the encrypted data</li>
     *   <li>Base64-encodes the result for safe storage/transmission</li>
     * </ol>
     *
     * @param plainText the plaintext string to encrypt
     * @return Base64-encoded string containing [IV + ciphertext + auth tag]
     * @throws EncryptionFailureException if plainText is null/blank or encryption fails
     */
    public String encrypt(String plainText) {
        validatePlaintext(plainText);
        return encrypt(plainText.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Encrypts plaintext bytes using AES-GCM.
     *
     * <p>The IV is prepended to the ciphertext (which includes the GCM authentication tag).
     * The final result is Base64-encoded.
     *
     * <p><b>Format:</b> Base64([IV (12 bytes)][Ciphertext][Auth Tag (16 bytes)])
     *
     * @param plaintextBytes the plaintext bytes to encrypt
     * @return Base64-encoded string containing [IV + ciphertext + auth tag]
     * @throws EncryptionFailureException if plaintextBytes is null or encryption fails
     */
    public String encrypt(byte[] plaintextBytes) {
        validatePlaintextBytes(plaintextBytes);

        byte[] iv = generateIV();

        try {
            Cipher cipher = Cipher.getInstance(CIPHER_TRANSFORMATION);
            GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(AUTH_TAG_LENGTH_BITS, iv);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmParameterSpec);

            byte[] ciphertextWithTag = cipher.doFinal(plaintextBytes);

            ByteBuffer byteBuffer = ByteBuffer.allocate(IV_LENGTH_BYTES + ciphertextWithTag.length);
            byteBuffer.put(iv);
            byteBuffer.put(ciphertextWithTag);

            return Base64.getEncoder().encodeToString(byteBuffer.array());
        } catch (GeneralSecurityException e) {
            throw new EncryptionFailureException(ERROR_ENCRYPTION_FAILED, e);
        }
    }

    /**
     * Encrypts byte array data using AES-GCM and returns encrypted bytes.
     *
     * <p>This method is useful when working with binary data that doesn't need Base64 encoding.
     * The IV is prepended to the encrypted data.
     *
     * <p><b>Format:</b> [IV (12 bytes)][Ciphertext][Auth Tag (16 bytes)]
     *
     * @param plaintextBytes the data to encrypt
     * @return byte array containing [IV + ciphertext + auth tag]
     * @throws EncryptionFailureException if plaintextBytes is null or encryption fails
     */
    public byte[] encryptBytes(byte[] plaintextBytes) {
        validatePlaintextBytes(plaintextBytes);

        byte[] iv = generateIV();

        try {
            Cipher cipher = Cipher.getInstance(CIPHER_TRANSFORMATION);
            GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(AUTH_TAG_LENGTH_BITS, iv);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmParameterSpec);

            byte[] ciphertextWithTag = cipher.doFinal(plaintextBytes);

            ByteBuffer byteBuffer = ByteBuffer.allocate(IV_LENGTH_BYTES + ciphertextWithTag.length);
            byteBuffer.put(iv);
            byteBuffer.put(ciphertextWithTag);

            return byteBuffer.array();
        } catch (GeneralSecurityException e) {
            throw new EncryptionFailureException(ERROR_ENCRYPTION_FAILED, e);
        }
    }

    /**
     * Decrypts Base64-encoded ciphertext using AES-GCM.
     *
     * <p>The decryption process:
     * <ol>
     *   <li>Validates input is not null or blank</li>
     *   <li>Base64-decodes the ciphertext</li>
     *   <li>Extracts the IV from the first 12 bytes</li>
     *   <li>Decrypts remaining bytes using AES-GCM</li>
     *   <li>Verifies authentication tag (automatic in GCM mode)</li>
     *   <li>Converts decrypted bytes to UTF-8 string</li>
     * </ol>
     *
     * <p><b>Security Note:</b> If the authentication tag verification fails, GCM will throw
     * an exception, preventing tampering or corruption from going undetected.
     *
     * @param cipherText the Base64-encoded string containing [IV + ciphertext + auth tag]
     * @return the decrypted plaintext string
     * @throws DecryptionFailureException if cipherText is null/blank, too short, or decryption fails
     */
    public String decrypt(String cipherText) {
        validateCiphertext(cipherText);

        try {
            byte[] encryptedWithIv = Base64.getDecoder().decode(cipherText);
            byte[] decryptedBytes = decryptBytes(encryptedWithIv);
            return new String(decryptedBytes, StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            throw new DecryptionFailureException(ERROR_DECRYPTION_FAILED, e);
        }
    }

    /**
     * Decrypts encrypted byte array data using AES-GCM.
     *
     * <p>The input must contain the IV prepended to the ciphertext and auth tag.
     * This method performs authentication tag verification automatically.
     *
     * <p><b>Expected Format:</b> [IV (12 bytes)][Ciphertext][Auth Tag (16 bytes)]
     *
     * @param encryptedData the encrypted data containing [IV + ciphertext + auth tag]
     * @return the decrypted plaintext bytes
     * @throws DecryptionFailureException if encryptedData is null, too short, or decryption fails
     */
    public byte[] decryptBytes(byte[] encryptedData) {
        validateEncryptedData(encryptedData);

        try {
            ByteBuffer byteBuffer = ByteBuffer.wrap(encryptedData);

            byte[] iv = new byte[IV_LENGTH_BYTES];
            byteBuffer.get(iv);

            byte[] ciphertextWithTag = new byte[byteBuffer.remaining()];
            byteBuffer.get(ciphertextWithTag);

            Cipher cipher = Cipher.getInstance(CIPHER_TRANSFORMATION);
            GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(AUTH_TAG_LENGTH_BITS, iv);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmParameterSpec);

            return cipher.doFinal(ciphertextWithTag);
        } catch (GeneralSecurityException e) {
            throw new DecryptionFailureException(ERROR_DECRYPTION_FAILED, e);
        }
    }

    /**
     * Encrypts data using wrapper Byte[] type.
     *
     * @param data the data to encrypt as Byte wrapper array
     * @return encrypted data as Byte wrapper array containing [IV + ciphertext + auth tag]
     * @throws EncryptionFailureException if data is null or encryption fails
     */
    public Byte[] encrypt(Byte[] data) {
        if (data == null) {
            throw new EncryptionFailureException(ERROR_PLAINTEXT_BYTES_NULL);
        }

        byte[] primitiveData = convertToPrimitive(data);
        byte[] encryptedPrimitive = encryptBytes(primitiveData);
        return convertToWrapper(encryptedPrimitive);
    }

    /**
     * Decrypts data using wrapper Byte[] type.
     *
     * @param encryptedData the encrypted data as Byte wrapper array
     * @return decrypted plaintext as Byte wrapper array
     * @throws DecryptionFailureException if encryptedData is null or decryption fails
     */
    public Byte[] decrypt(Byte[] encryptedData) {
        if (encryptedData == null) {
            throw new DecryptionFailureException(ERROR_ENCRYPTED_DATA_NULL);
        }

        byte[] primitiveData = convertToPrimitive(encryptedData);
        byte[] decryptedPrimitive = decryptBytes(primitiveData);
        return convertToWrapper(decryptedPrimitive);
    }

    /**
     * Generates a cryptographically secure random IV.
     *
     * @return a 12-byte (96-bit) random IV
     */
    private byte[] generateIV() {
        byte[] iv = new byte[IV_LENGTH_BYTES];
        secureRandom.nextBytes(iv);
        return iv;
    }

    /**
     * Validates that plaintext string is not null or blank.
     *
     * @param plainText the plaintext to validate
     * @throws EncryptionFailureException if plainText is null or blank
     */
    private void validatePlaintext(String plainText) {
        if (plainText == null || plainText.isBlank()) {
            throw new EncryptionFailureException(ERROR_PLAINTEXT_NULL_OR_EMPTY);
        }
    }

    /**
     * Validates that plaintext bytes are not null.
     *
     * @param plaintextBytes the plaintext bytes to validate
     * @throws EncryptionFailureException if plaintextBytes is null
     */
    private void validatePlaintextBytes(byte[] plaintextBytes) {
        if (plaintextBytes == null) {
            throw new EncryptionFailureException(ERROR_PLAINTEXT_BYTES_NULL);
        }
    }

    /**
     * Validates that ciphertext string is not null or blank.
     *
     * @param cipherText the ciphertext to validate
     * @throws DecryptionFailureException if cipherText is null or blank
     */
    private void validateCiphertext(String cipherText) {
        if (cipherText == null || cipherText.isBlank()) {
            throw new DecryptionFailureException(ERROR_CIPHERTEXT_NULL_OR_EMPTY);
        }
    }

    /**
     * Validates that encrypted data is not null and meets minimum length requirements.
     *
     * @param encryptedData the encrypted data to validate
     * @throws DecryptionFailureException if encryptedData is null or too short
     */
    private void validateEncryptedData(byte[] encryptedData) {
        if (encryptedData == null) {
            throw new DecryptionFailureException(ERROR_ENCRYPTED_DATA_NULL);
        }

        if (encryptedData.length < MINIMUM_ENCRYPTED_DATA_LENGTH) {
            throw new DecryptionFailureException(
                    String.format(ERROR_ENCRYPTED_DATA_TOO_SHORT, MINIMUM_ENCRYPTED_DATA_LENGTH));
        }
    }

    /**
     * Converts wrapper Byte array to primitive byte array.
     *
     * @param wrapperArray the Byte wrapper array
     * @return primitive byte array
     */
    private byte[] convertToPrimitive(Byte[] wrapperArray) {
        byte[] primitiveArray = new byte[wrapperArray.length];
        for (int i = 0; i < wrapperArray.length; i++) {
            primitiveArray[i] = wrapperArray[i];
        }
        return primitiveArray;
    }

    /**
     * Converts primitive byte array to wrapper Byte array.
     *
     * @param primitiveArray the primitive byte array
     * @return Byte wrapper array
     */
    private Byte[] convertToWrapper(byte[] primitiveArray) {
        Byte[] wrapperArray = new Byte[primitiveArray.length];
        for (int i = 0; i < primitiveArray.length; i++) {
            wrapperArray[i] = primitiveArray[i];
        }
        return wrapperArray;
    }
}