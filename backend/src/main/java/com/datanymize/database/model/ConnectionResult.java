package com.datanymize.database.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Result of a connection test operation.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConnectionResult {
    private boolean success;
    private String message;
    private String errorMessage;
    private long durationMs;
    private String errorCode;
    private String errorDetails;
}
