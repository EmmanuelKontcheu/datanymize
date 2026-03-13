package com.datanymize.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Generic API response wrapper for all endpoints.
 * 
 * Validates Requirements: 1.1, 1.2, 1.3, 2.1, 2.2, 2.3
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {
    
    private boolean success;
    
    private String message;
    
    private T data;
    
    private ErrorDetails error;
    
    private Instant timestamp;
    
    private String requestId;
    
    /**
     * Creates a successful response.
     */
    public static <T> ApiResponse<T> success(T data, String message) {
        return ApiResponse.<T>builder()
            .success(true)
            .message(message)
            .data(data)
            .timestamp(Instant.now())
            .build();
    }
    
    /**
     * Creates a successful response without data.
     */
    public static <T> ApiResponse<T> success(String message) {
        return ApiResponse.<T>builder()
            .success(true)
            .message(message)
            .timestamp(Instant.now())
            .build();
    }
    
    /**
     * Creates an error response.
     */
    public static <T> ApiResponse<T> error(String message, String errorCode, int statusCode) {
        return ApiResponse.<T>builder()
            .success(false)
            .message(message)
            .error(ErrorDetails.builder()
                .code(errorCode)
                .statusCode(statusCode)
                .build())
            .timestamp(Instant.now())
            .build();
    }
    
    /**
     * Creates an error response with details.
     */
    public static <T> ApiResponse<T> error(String message, String errorCode, int statusCode, String details) {
        return ApiResponse.<T>builder()
            .success(false)
            .message(message)
            .error(ErrorDetails.builder()
                .code(errorCode)
                .statusCode(statusCode)
                .details(details)
                .build())
            .timestamp(Instant.now())
            .build();
    }
    
    /**
     * Error details nested in response.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ErrorDetails {
        private String code;
        private int statusCode;
        private String details;
        private String suggestion;
    }
}
