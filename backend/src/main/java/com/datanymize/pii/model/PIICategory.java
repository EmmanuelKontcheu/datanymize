package com.datanymize.pii.model;

/**
 * Enumeration of PII (Personally Identifiable Information) categories.
 * Used to classify sensitive data columns.
 * 
 * Validates Requirements: 3.1, 3.2
 */
public enum PIICategory {
    EMAIL("Email Address"),
    PHONE("Phone Number"),
    SSN("Social Security Number"),
    CREDIT_CARD("Credit Card Number"),
    NAME("Person Name"),
    ADDRESS("Physical Address"),
    IDENTIFIER("Identifier (ID, Account Number)"),
    FINANCIAL("Financial Information"),
    MEDICAL("Medical Information"),
    BIOMETRIC("Biometric Data"),
    NONE("Not PII");
    
    private final String displayName;
    
    PIICategory(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    /**
     * Parse a string to PIICategory.
     * 
     * @param value String value to parse
     * @return PIICategory or NONE if not recognized
     */
    public static PIICategory fromString(String value) {
        if (value == null || value.trim().isEmpty()) {
            return NONE;
        }
        
        try {
            return PIICategory.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return NONE;
        }
    }
}
