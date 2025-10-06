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

@Service
public class AESGCMEncryptionService {
    private static final String AES = "AES";
    private static final String AES_GCM = "AES/GCM/NoPadding";
    private static final int IV_SIZE = 12;
    private static final int GCM_IV_LENGTH = 12;
    private static final int TAG_LENGTH = 128;
    private final SecretKeySpec secretKey;

    public AESGCMEncryptionService(@Value("${security.encryption-key}") String base64Key) {
        byte[] keyBytes = Base64.getDecoder().decode(base64Key);
        this.secretKey = new SecretKeySpec(keyBytes, AES);
    }

    /**
     * Encrypts plaintext using AES/GCM/NoPadding.
     * The IV is prepended to the ciphertext (which already includes the GCM Tag).
     * The final result is a Base64 encoded string.
     * @param plaintextBytes The plaintext to encrypt.
     * @return Base64 encoded string of IV + Ciphertext + GCM Tag.
     */
    public String encrypt(byte[] plaintextBytes) throws EncryptionFailureException {
        byte[] iv = new byte[GCM_IV_LENGTH];
        new SecureRandom().nextBytes(iv); // Generate a unique, random IV for each encryption
        try {
            Cipher cipher = Cipher.getInstance(AES_GCM);
            GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmParameterSpec);

            byte[] ciphertextWithTag = cipher.doFinal(plaintextBytes);

            ByteBuffer byteBuffer = ByteBuffer.allocate(iv.length + ciphertextWithTag.length);
            byteBuffer.put(iv);
            byteBuffer.put(ciphertextWithTag);
            return Base64.getEncoder().encodeToString(byteBuffer.array());
        } catch(Exception e) { 
            throw new EncryptionFailureException(e);
        }
    }

    public String encrypt(String plainText) throws EncryptionFailureException {
        if(plainText == null || plainText.isBlank()) {
            throw new EncryptionFailureException("Invalid argument, cannot encrypt!");
        }
        return encrypt(plainText.getBytes());
    }

        /**
         * Decrypts Base64 encoded ciphertext (including prepended IV and GCM Tag) using AES/GCM/NoPadding.
         * @param cipherText The Base64 encoded string of IV + Ciphertext + GCM Tag.
         * @return Decrypted plaintext as a String.
         * @throws DecryptionFailureException if decryption fails (e.g., Tag Mismatch).
         */
    public String decrypt(String cipherText) throws DecryptionFailureException {
        try {
            byte[] encryptedWithIv = Base64.getDecoder().decode(cipherText);

            if (encryptedWithIv.length < IV_SIZE + (TAG_LENGTH / 8)) { // Ensure the byte array is long enough
                throw new IllegalArgumentException("Invalid encrypted data length");
            }

            ByteBuffer byteBuffer = ByteBuffer.wrap(encryptedWithIv);
            byte[] iv = new byte[IV_SIZE];
            byteBuffer.get(iv); // Extract IV from the start

            byte[] encryptedWithTag = new byte[byteBuffer.remaining()];
            byteBuffer.get(encryptedWithTag); // Extract the rest of the bytes (ciphertext + tag)

            Cipher cipher = Cipher.getInstance(AES_GCM);
            GCMParameterSpec spec = new GCMParameterSpec(TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, spec);

            byte[] decrypted = cipher.doFinal(encryptedWithTag);
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (GeneralSecurityException | IllegalArgumentException e) {
            throw new DecryptionFailureException(e);
        }
    }

       public Byte[] encrypt(Byte[] data) {
        try {
            Cipher cipher = Cipher.getInstance(AES_GCM);
            byte[] iv = new byte[IV_SIZE];
            SecureRandom secureRandom = new SecureRandom();
            secureRandom.nextBytes(iv);

            GCMParameterSpec spec = new GCMParameterSpec(TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, spec);
            byte[] encrypted = cipher.doFinal(toPrimitive(data));

            ByteBuffer byteBuffer = ByteBuffer.allocate(iv.length + encrypted.length);
            byteBuffer.put(iv);
            byteBuffer.put(encrypted);
            return toObject(byteBuffer.array());
        } catch (Exception e) {
            throw new EncryptionFailureException(e);
        }
    }

    public Byte[] decrypt(Byte[] encryptedData) {
        try {
            ByteBuffer byteBuffer = ByteBuffer.wrap(toPrimitive(encryptedData));
            byte[] iv = new byte[12];
            byteBuffer.get(iv);

            byte[] encrypted = new byte[byteBuffer.remaining()];
            byteBuffer.get(encrypted);

            Cipher cipher = Cipher.getInstance(AES_GCM);
            GCMParameterSpec spec = new GCMParameterSpec(TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, spec);
            return toObject(cipher.doFinal(encrypted));
        } catch (Exception e) {
            throw new DecryptionFailureException(e);
        }
    }

    public byte[] decrypt(byte[] encryptedData) {
        try {
            ByteBuffer byteBuffer = ByteBuffer.wrap(encryptedData);
            byte[] iv = new byte[12];
            byteBuffer.get(iv);

            byte[] encrypted = new byte[byteBuffer.remaining()];
            byteBuffer.get(encrypted);

            Cipher cipher = Cipher.getInstance(AES_GCM);
            GCMParameterSpec spec = new GCMParameterSpec(TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, spec);
            return cipher.doFinal(encrypted);
        } catch (Exception e) {
            throw new DecryptionFailureException(e);
        }
    }

    private static Byte[] toObject(byte[] bytes) {
        Byte[] result = new Byte[bytes.length];
        for (int i = 0; i < bytes.length; i++) {
            result[i] = bytes[i];
        }
        return result;
    }

    private static byte[] toPrimitive(Byte[] bytes) {
        byte[] result = new byte[bytes.length];
        for (int i = 0; i < bytes.length; i++) {
            result[i] = bytes[i];
        }
        return result;
    }
}
