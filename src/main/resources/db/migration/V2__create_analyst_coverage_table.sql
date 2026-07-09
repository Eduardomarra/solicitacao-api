-- Tabela de cobertura do analista
CREATE TABLE IF NOT EXISTS analyst_coverage (
                                                id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    state VARCHAR(2) NOT NULL,
    UNIQUE(user_id, state)
    );

CREATE INDEX idx_analyst_coverage_user_id ON analyst_coverage(user_id);