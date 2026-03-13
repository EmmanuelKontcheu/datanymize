-- Create audit_logs table for GDPR compliance audit logging
-- Validates Requirements: 16.1, 16.2, 16.5, 16.6

CREATE TABLE IF NOT EXISTS audit_logs (
    id VARCHAR(36) PRIMARY KEY,
    tenant_id VARCHAR(255) NOT NULL,
    user_id VARCHAR(255) NOT NULL,
    action VARCHAR(255) NOT NULL,
    timestamp TIMESTAMP NOT NULL,
    source_database VARCHAR(255),
    target_database VARCHAR(255),
    rows_processed BIGINT DEFAULT 0,
    success BOOLEAN NOT NULL,
    error_message TEXT,
    ip_address VARCHAR(45),
    user_agent TEXT,
    metadata TEXT,
    created_at TIMESTAMP NOT NULL,
    
    -- Indexes for efficient querying
    INDEX idx_tenant_id (tenant_id),
    INDEX idx_user_id (user_id),
    INDEX idx_action (action),
    INDEX idx_timestamp (timestamp),
    INDEX idx_created_at (created_at)
);

-- Add comment for documentation
ALTER TABLE audit_logs COMMENT = 'Immutable audit log entries for GDPR compliance. Append-only, encrypted at rest.';
