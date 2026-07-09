-- Tabela de solicitações
CREATE TABLE IF NOT EXISTS solicitations (
                                             id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    client_id UUID NOT NULL REFERENCES users(id),

    -- Status e step
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    current_step INTEGER DEFAULT 0,

    -- Step 1 - Dados básicos
    service_type VARCHAR(20),
    title VARCHAR(80),
    description VARCHAR(1000),

    -- Step 2 - Endereço
    cep VARCHAR(8),
    number VARCHAR(20),
    complement VARCHAR(100),
    street VARCHAR(255),
    neighborhood VARCHAR(100),
    city VARCHAR(100),
    state VARCHAR(2),

    -- Step 3 - Confirmação
    priority VARCHAR(10),
    preferred_date DATE,
    estimated_value DECIMAL(15,2),
    terms_accepted BOOLEAN,

    -- Auditoria
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    submitted_at TIMESTAMP,
    analyzed_at TIMESTAMP,
    analyzed_by UUID REFERENCES users(id),
    analysis_comment VARCHAR(1000)
    );

-- Criar índices para performance
CREATE INDEX idx_solicitations_client_id ON solicitations(client_id);
CREATE INDEX idx_solicitations_status ON solicitations(status);
CREATE INDEX idx_solicitations_state ON solicitations(state);
CREATE INDEX idx_solicitations_created_at ON solicitations(created_at);