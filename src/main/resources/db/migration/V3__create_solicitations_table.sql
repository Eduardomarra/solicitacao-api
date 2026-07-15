-- Tabela de solicitações
CREATE TABLE IF NOT EXISTS solicitations (
                                             id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    client_id UUID NOT NULL REFERENCES users(id),

    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT' CHECK (status IN ('DRAFT', 'SUBMITTED', 'IN_REVIEW', 'APPROVED', 'REJECTED')),
    current_step INTEGER DEFAULT 0,

    service_type VARCHAR(20) CHECK (service_type IN ('INSTALLATION', 'MAINTENANCE', 'INSPECTION')),
    title VARCHAR(80),
    description VARCHAR(1000),

    cep VARCHAR(8),
    number VARCHAR(20),
    complement VARCHAR(100),
    street VARCHAR(255),
    neighborhood VARCHAR(100),
    city VARCHAR(100),
    state VARCHAR(2),

    priority VARCHAR(10) CHECK (priority IN ('LOW', 'MEDIUM', 'HIGH')),
    preferred_date DATE,
    estimated_value DECIMAL(15,2),
    terms_accepted BOOLEAN,

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    submitted_at TIMESTAMP,
    analyzed_at TIMESTAMP,
    analyzed_by UUID REFERENCES users(id),
    analysis_comment VARCHAR(1000)
    );

CREATE INDEX IF NOT EXISTS idx_solicitations_client_id ON solicitations(client_id);
CREATE INDEX IF NOT EXISTS idx_solicitations_status ON solicitations(status);
CREATE INDEX IF NOT EXISTS idx_solicitations_state ON solicitations(state);
CREATE INDEX IF NOT EXISTS idx_solicitations_created_at ON solicitations(created_at);