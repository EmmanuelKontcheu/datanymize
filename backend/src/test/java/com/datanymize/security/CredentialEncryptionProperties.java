package com.datanymize.security;

import net.jqwik.api.*;
import net.jqwik.api.constraints.StringLength;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Property-based tests for credential encryption and lifecycle management.
 * 
 * **Validates: Requirements 14.1, 14.2, 14.3, 14.4, 14.5**
 */
@PropertyDefaults(tries = 100)
public class CredentialEncryptionProperties {
    
    /**
     * Property 21: Credential Encryption
     * 
     * Verifies that passwords are encrypted and cannot be read in plaintext.
     */
    @Property
    void credentialEncryptionProperty(
            @ForAll @StringLength(min = 8, max = 128) String password) {
        
        CredentialEncryption encryption = new CredentialEncryption();
        
        // Encrypt the password
        String encrypted = encryption.encrypt(password);
        
        // Encrypted password should not be the same as plaintext
        assertNotEquals(password, encrypted,
            "Encrypted password should differ from plaintext");
        
        // Encrypted password should not be empty
        assertNotNull(encrypted);
        assertFalse(encrypted.isEmpty(),
            "Encrypted password should not be empty");
        
        // Encrypted password should be decodable (base64)
        assertTrue(isValidBase64(encrypted),
            "Encrypted password should be valid base64");
        
        // Decryption should recover the original password
        String decrypted = encryption.decrypt(encrypted);
        assertEquals(password, decrypted,
            "Decrypted password should match original");
    }
    
    /**
     * Property 22: Credential Lifecycle Management
     * 
     * Verifies that credentials can be stored, retrieved, and cleared.
     */
    @Property
    void credentialLifecycleManagementProperty(
            @ForAll @StringLength(min = 8, max = 128) String password,
            @ForAll @StringLength(min = 1, max = 50) String connectionId) {
        
        CredentialManager manager = new CredentialManager();
        
        // Store credential
        assertDoesNotThrow(() -> manager.storeCredential(connectionId, password),
            "Should be able to store credential");
        
        // Verify credential exists
        assertTrue(manager.hasCredential(connectionId),
            "Credential should exist after storage");
        
        // Retrieve credential
        String retrieved = manager.retrieveCredential(connectionId);
        assertEquals(password, retrieved,
            "Retrieved credential should match stored password");
        
        // Delete credential
        assertDoesNotThrow(() -> manager.deleteCredential(connectionId),
            "Should be able to delete credential");
        
        // Verify credential is deleted
        assertFalse(manager.hasCredential(connectionId),
            "Credential should not exist after deletion");
    }
    
    /**
     * Property: Credential sanitization removes sensitive data
     * 
     * Verifies that sensitive data is masked in logs and error messages.
     */
    @Property
    void credentialSanitizationProperty(
            @ForAll @StringLength(min = 8, max = 128) String password) {
        
        String connectionString = "jdbc:mysql://user:" + password + "@localhost:3306/db";
        
        String sanitized = CredentialSanitizer.sanitize(connectionString);
        
        // Sanitized string should not contain the password
        assertFalse(sanitized.contains(password),
            "Sanitized string should not contain password");
        
        // Sanitized string should contain a mask indicator
        assertTrue(sanitized.contains("***") || sanitized.contains("REDACTED"),
            "Sanitized string should contain mask indicator");
        
        // Sanitized string should still be readable
        assertTrue(sanitized.contains("jdbc:mysql://"),
            "Sanitized string should preserve connection type");
    }
    
    /**
     * Property: Sensitive data detection works correctly
     * 
     * Verifies that sensitive data is correctly identified.
     */
    @Property
    void sensitiveDataDetectionProperty(
            @ForAll @StringLength(min = 8, max = 128) String password) {
        
        String withPassword = "password=" + password;
        String withoutPassword = "username=john";
        
        assertTrue(CredentialSanitizer.containsSensitiveData(withPassword),
            "Should detect password in string");
        
        assertFalse(CredentialSanitizer.containsSensitiveData(withoutPassword),
            "Should not detect sensitive data in safe string");
    }
    
    /**
     * Property: Encryption is deterministic
     * 
     * Verifies that the same password always encrypts to the same value.
     */
    @Property
    void encryptionDeterminismProperty(
            @ForAll @StringLength(min = 8, max = 128) String password) {
        
        CredentialEncryption encryption = new CredentialEncryption();
        
        // Encrypt the same password multiple times
        String encrypted1 = encryption.encrypt(password);
        String encrypted2 = encryption.encrypt(password);
        
        // Both encryptions should be identical (deterministic)
        assertEquals(encrypted1, encrypted2,
            "Encryption should be deterministic");
    }
    
    /**
     * Property: Different passwords produce different encrypted values
     * 
     * Verifies that different passwords encrypt to different values.
     */
    @Property
    void differentPasswordsProperty(
            @ForAll @StringLength(min = 8, max = 128) String password1,
            @ForAll @StringLength(min = 8, max = 128) String password2) {
        
        Assume.that(!password1.equals(password2));
        
        CredentialEncryption encryption = new CredentialEncryption();
        
        String encrypted1 = encryption.encrypt(password1);
        String encrypted2 = encryption.encrypt(password2);
        
        assertNotEquals(encrypted1, encrypted2,
            "Different passwords should encrypt to different values");
    }
    
    /**
     * Property: Decryption of invalid data fails gracefully
     * 
     * Verifies that decryption of invalid data throws an exception.
     */
    @Property
    void invalidDecryptionProperty(
            @ForAll @StringLength(min = 1, max = 50) String invalidData) {
        
        Assume.that(!isValidBase64(invalidData));
        
        CredentialEncryption encryption = new CredentialEncryption();
        
        assertThrows(Exception.class,
            () -> encryption.decrypt(invalidData),
            "Decryption of invalid data should throw exception");
    }
    
    // Helper methods
    
    private boolean isValidBase64(String str) {
        try {
            java.util.Base64.getDecoder().decode(str);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
