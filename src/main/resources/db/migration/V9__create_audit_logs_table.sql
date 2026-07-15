-- Tabela de auditoria
CREATE TABLE IF NOT EXISTS audit_logs (
                                          id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES users(id),
    user_role VARCHAR(20) NOT NULL CHECK (user_role IN ('CLIENT', 'ANALYST', 'ADMIN')),
    action VARCHAR(100) NOT NULL,
    entity_id VARCHAR(100),
    success BOOLEAN NOT NULL,
    error_message TEXT,
    duration_ms BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    );

CREATE INDEX IF NOT EXISTS idx_audit_logs_user_id ON audit_logs(user_id);
CREATE INDEX IF NOT EXISTS idx_audit_logs_created_at ON audit_logs(created_at);