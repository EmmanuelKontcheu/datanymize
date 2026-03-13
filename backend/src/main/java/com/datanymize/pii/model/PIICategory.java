package com.datanymize.pii.model;

/**
 * Enumeration of PII (Personally Identifiable Information) categories.
 */
public enum PIICategory {
    EMAIL("Email Address"),
    PHONE("Phone Number"),
    SSN("Social Security Number"),
    CREDIT_CARD("Credit Card Number"),
    NAME("Person Name"),
    ADDRESS("Physical Address"),
    IDENTIFIER("Identifier"),
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
}
