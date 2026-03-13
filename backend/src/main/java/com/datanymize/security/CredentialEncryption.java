package com.datanymize.security;

import lombok.extern.slf4j.Slf4j;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

/**
 * Utility for encrypting and decrypting credentials using AES-256.
 * 
 * Validates Requirements: 14.1, 14.5
 */
@Slf4j
public class CredentialEncryption {
    
    private static final String ALGORITHM = "AES";
    private static final int KEY_SIZE = 256;
    private static final String CIPHER_ALGORITHM = "AES";
    
    private final SecretKey secretKey;
    
    /**
     * Initialize with a provided secret key.
     * 
     * @param encodedKey Base64-encoded secret key
     */
    public CredentialEncryption(String encodedKey) {
        byte[] decodedKey = Base64.getDecoder().decode(encodedKey);
        this.secretKey = new SecretKeySpec(decodedKey, 0, decodedKey.length, ALGORITHM);
    }
    
    /**
     * Initialize with a new randomly generated key.
     */
    public CredentialEncryption() {
        try {
            KeyGenerator keyGen = KeyGenerator.getInstance(ALGORITHM);
            keyGen.init(KEY_SIZE);
            this.secretKey = keyGen.generateKey();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate encryption key", e);
        }
    }
    
    /**
     * Encrypt a plaintext credential.
     * 
     * @param plaintext The credential to encrypt
     * @return Base64-encoded encrypted credential
     */
    public String encrypt(String plaintext) {
        try {
            Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            byte[] encryptedBytes = cipher.doFinal(plaintext.getBytes());
            return Base64.getEncoder().encodeToString(encryptedBytes);
        } catch (Exception e) {
            log.error("Failed to encrypt credential", e);
            throw new RuntimeException("Encryption failed", e);
        }
    }
    
    /**
     * Decrypt an encrypted credential.
     * 
     * @param encryptedText Base64-encoded encrypted credential
     * @return The decrypted plaintext credential
     */
    public String decrypt(String encryptedText) {
        try {
            Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, secretKey);
            byte[] decodedBytes = Base64.getDecoder().decode(encryptedText);
            byte[] decryptedBytes = cipher.doFinal(decodedBytes);
            return new String(decryptedBytes);
        } catch (Exception e) {
            log.error("Failed to decrypt credential", e);
            throw new RuntimeException("Decryption failed", e);
        }
    }
    
    /**
     * Get the secret key in Base64-encoded format for storage.
     * 
     * @return Base64-encoded secret key
     */
    public String getEncodedKey() {
        return Base64.getEncoder().encodeToString(secretKey.getEncoded());
    }
}
