package com.datanymize.security;

/**
 * Interface for managing encrypted credential storage and lifecycle.
 * 
 * Validates Requirements: 14.1, 14.4, 14.5
 */
public interface ICredentialManager {
    
    /**
     * Store a credential (password) encrypted with AES-256.
     * 
     * @param connectionId The connection identifier
     * @param password The plaintext password to encrypt and store
     */
    void storeCredential(String connectionId, String password);
    
    /**
     * Retrieve a stored credential (decrypted).
     * 
     * @param connectionId The connection identifier
     * @return The decrypted password
     * @throws IllegalArgumentException if credential not found
     */
    String retrieveCredential(String connectionId);
    
    /**
     * Delete a stored credential from memory.
     * 
     * @param connectionId The connection identifier
     */
    void deleteCredential(String connectionId);
    
    /**
     * Check if a credential exists for the given connection.
     * 
     * @param connectionId The connection identifier
     * @return true if credential exists, false otherwise
     */
    boolean hasCredential(String connectionId);
    
    /**
     * Clear all stored credentials from memory.
     * This should be called during shutdown or when clearing sensitive data.
     */
    void clearAllCredentials();
}
