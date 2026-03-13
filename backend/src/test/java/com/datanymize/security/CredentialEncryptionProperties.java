package com.datanymize.security;

import net.jqwik.api.*;
import net.jqwik.api.constraints.StringLength;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Property-based tests for credential encryption and management.
 * 
 * **Validates: Requirements 14.1, 14.4, 14.5**
 * 
 * Tests verify:
 * - Encrypted credentials cannot be read without decryption
 * - Same password encrypts to different ciphertexts (due to random IV)
 * - Decryption returns the original password
 * - Credential lifecycle (store, retrieve, delete)
 * - Deleted credentials cannot be retrieved
 */
@DisplayName("Credential Encryption Properties")
public class CredentialEncryptionProperties {
    
    /**
     * Property 21: Credential Encryption
     * 
     * For any password, encrypting it should produce a ciphertext that:
     * 1. Is different from the plaintext
     * 2. Can be decrypted back to the original password
     * 3. Is Base64-encoded (safe for storage)
     */
    @Property(tries = 100)
    @DisplayName("Encrypted credentials cannot be read without decryption")
    void testEncryptedCredentialsAreNotReadable(
        @ForAll @StringLength(min = 8, max = 128) String password
    ) {
        // Given a credential encryption utility
        CredentialEncryption encryption = new CredentialEncryption();
        
        // When encrypting a password
        String encrypted = encryption.encrypt(password);
        
        // Then the encrypted value should not equal the plaintext
        assertNotEquals(password, encrypted, "Encrypted password should differ from plaintext");
        
        // And the encrypted value should be Base64-encoded (contain only valid Base64 chars)
        assertTrue(isValidBase64(encrypted), "Encrypted password should be Base64-encoded");
        
        // And decryption should return the original password
        String decrypted = encryption.decrypt(encrypted);
        assertEquals(password, decrypted, "Decrypted password should match original");
    }
    
    /**
     * Property 21: Credential Encryption (Randomness)
     * 
     * For any password, encrypting it multiple times should produce different ciphertexts
     * (due to random IV generation), but all should decrypt to the same plaintext.
     */
    @Property(tries = 100)
    @DisplayName("Same password encrypts to different ciphertexts due to random IV")
    void testEncryptionRandomness(
        @ForAll @StringLength(min = 8, max = 128) String password
    ) {
        // Given a credential encryption utility
        CredentialEncryption encryption = new CredentialEncryption();
        
        // When encrypting the same password multiple times
        String encrypted1 = encryption.encrypt(password);
        String encrypted2 = encryption.encrypt(password);
        String encrypted3 = encryption.encrypt(password);
        
        // Then all ciphertexts should be different (due to random IV)
        assertNotEquals(encrypted1, encrypted2, "Encryptions should produce different ciphertexts");
        assertNotEquals(encrypted2, encrypted3, "Encryptions should produce different ciphertexts");
        assertNotEquals(encrypted1, encrypted3, "Encryptions should produce different ciphertexts");
        
        // But all should decrypt to the same plaintext
        assertEquals(password, encryption.decrypt(encrypted1), "All should decrypt to original");
        assertEquals(password, encryption.decrypt(encrypted2), "All should decrypt to original");
        assertEquals(password, encryption.decrypt(encrypted3), "All should decrypt to original");
    }
    
    /**
     * Property 21: Credential Encryption (Decryption Correctness)
     * 
     * For any password, the round-trip encrypt-then-decrypt should return the original.
     */
    @Property(tries = 100)
    @DisplayName("Decryption returns the original password")
    void testDecryptionCorrectness(
        @ForAll @StringLength(min = 8, max = 128) String password
    ) {
        // Given a credential encryption utility
        CredentialEncryption encryption = new CredentialEncryption();
        
        // When encrypting and then decrypting
        String encrypted = encryption.encrypt(password);
        String decrypted = encryption.decrypt(encrypted);
        
        // Then the result should equal the original password
        assertEquals(password, decrypted, "Round-trip should preserve password");
    }
    
    /**
     * Property 22: Credential Lifecycle Management
     * 
     * For any connection ID and password, the credential manager should:
     * 1. Store the credential
     * 2. Retrieve it correctly
     * 3. Delete it
     * 4. Not be able to retrieve it after deletion
     */
    @Property(tries = 100)
    @DisplayName("Credential lifecycle (store, retrieve, delete)")
    void testCredentialLifecycle(
        @ForAll @StringLength(min = 5, max = 50) String connectionId,
        @ForAll @StringLength(min = 8, max = 128) String password
    ) {
        // Given a credential manager
        CredentialEncryption encryption = new CredentialEncryption();
        CredentialManager manager = new CredentialManager(encryption);
        
        // When storing a credential
        manager.storeCredential(connectionId, password);
        
        // Then it should exist
        assertTrue(manager.hasCredential(connectionId), "Credential should exist after storage");
        
        // And retrieving it should return the original password
        String retrieved = manager.retrieveCredential(connectionId);
        assertEquals(password, retrieved, "Retrieved password should match original");
        
        // When deleting the credential
        manager.deleteCredential(connectionId);
        
        // Then it should no longer exist
        assertFalse(manager.hasCredential(connectionId), "Credential should not exist after deletion");
        
        // And attempting to retrieve it should throw an exception
        assertThrows(
            IllegalArgumentException.class,
            () -> manager.retrieveCredential(connectionId),
            "Should throw exception when retrieving deleted credential"
        );
    }
    
    /**
     * Property 22: Credential Lifecycle Management (Multiple Credentials)
     * 
     * For any set of connection IDs and passwords, the credential manager should
     * maintain them independently without interference.
     */
    @Property(tries = 50)
    @DisplayName("Multiple credentials are stored and retrieved independently")
    void testMultipleCredentials(
        @ForAll @StringLength(min = 5, max = 50) String connId1,
        @ForAll @StringLength(min = 5, max = 50) String connId2,
        @ForAll @StringLength(min = 8, max = 128) String password1,
        @ForAll @StringLength(min = 8, max = 128) String password2
    ) {
        // Assume connection IDs are different
        Assume.that(!connId1.equals(connId2));
        
        // Given a credential manager
        CredentialEncryption encryption = new CredentialEncryption();
        CredentialManager manager = new CredentialManager(encryption);
        
        // When storing two credentials
        manager.storeCredential(connId1, password1);
        manager.storeCredential(connId2, password2);
        
        // Then both should exist
        assertTrue(manager.hasCredential(connId1), "First credential should exist");
        assertTrue(manager.hasCredential(connId2), "Second credential should exist");
        
        // And retrieving them should return the correct passwords
        assertEquals(password1, manager.retrieveCredential(connId1), "First password should match");
        assertEquals(password2, manager.retrieveCredential(connId2), "Second password should match");
        
        // When deleting the first credential
        manager.deleteCredential(connId1);
        
        // Then only the first should be deleted
        assertFalse(manager.hasCredential(connId1), "First credential should be deleted");
        assertTrue(manager.hasCredential(connId2), "Second credential should still exist");
        
        // And the second should still be retrievable
        assertEquals(password2, manager.retrieveCredential(connId2), "Second password should still be retrievable");
    }
    
    /**
     * Property 22: Credential Lifecycle Management (Clear All)
     * 
     * For any set of credentials, clearing all should remove them all from memory.
     */
    @Property(tries = 50)
    @DisplayName("Clearing all credentials removes them from memory")
    void testClearAllCredentials(
        @ForAll @StringLength(min = 5, max = 50) String connId1,
        @ForAll @StringLength(min = 5, max = 50) String connId2,
        @ForAll @StringLength(min = 8, max = 128) String password1,
        @ForAll @StringLength(min = 8, max = 128) String password2
    ) {
        // Assume connection IDs are different
        Assume.that(!connId1.equals(connId2));
        
        // Given a credential manager with stored credentials
        CredentialEncryption encryption = new CredentialEncryption();
        CredentialManager manager = new CredentialManager(encryption);
        manager.storeCredential(connId1, password1);
        manager.storeCredential(connId2, password2);
        
        // When clearing all credentials
        manager.clearAllCredentials();
        
        // Then both should be gone
        assertFalse(manager.hasCredential(connId1), "First credential should be cleared");
        assertFalse(manager.hasCredential(connId2), "Second credential should be cleared");
    }
    
    /**
     * Property 22: Credential Lifecycle Management (Null Handling)
     * 
     * For invalid inputs (null or empty), the credential manager should throw exceptions.
     */
    @Property(tries = 50)
    @DisplayName("Invalid inputs are rejected with meaningful errors")
    void testInvalidInputHandling(
        @ForAll @StringLength(min = 8, max = 128) String password
    ) {
        // Given a credential manager
        CredentialEncryption encryption = new CredentialEncryption();
        CredentialManager manager = new CredentialManager(encryption);
        
        // When attempting to store with null connection ID
        assertThrows(
            IllegalArgumentException.class,
            () -> manager.storeCredential(null, password),
            "Should reject null connection ID"
        );
        
        // When attempting to store with empty connection ID
        assertThrows(
            IllegalArgumentException.class,
            () -> manager.storeCredential("", password),
            "Should reject empty connection ID"
        );
        
        // When attempting to store with null password
        assertThrows(
            IllegalArgumentException.class,
            () -> manager.storeCredential("conn1", null),
            "Should reject null password"
        );
    }
    
    /**
     * Helper method to validate Base64 encoding.
     */
    private boolean isValidBase64(String value) {
        try {
            java.util.Base64.getDecoder().decode(value);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
