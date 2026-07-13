-- -- Criar extensão UUID
-- CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
--
-- -- Tabela de usuários
-- CREATE TABLE IF NOT EXISTS users (
--                                      id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
--     name VARCHAR(100) NOT NULL,
--     email VARCHAR(100) NOT NULL UNIQUE,
--     password_hash VARCHAR(255) NOT NULL,
--     role VARCHAR(20) NOT NULL,
--     enabled BOOLEAN DEFAULT TRUE,
--     created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
--     );
--
-- -- Criar índices
-- CREATE INDEX idx_users_email ON users(email);


-- Tabela de usuários (H2 compatible)
CREATE TABLE IF NOT EXISTS users (
                                     id UUID DEFAULT RANDOM_UUID() PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(100) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL CHECK (role IN ('CLIENT', 'ANALYST', 'ADMIN')),
    enabled BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    );

CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);