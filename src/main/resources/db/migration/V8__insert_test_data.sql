-- ============================================
-- INSERIR DADOS DE TESTE (CLIENTES E SOLICITAÇÕES)
-- ============================================

-- ============================================
-- 1. CLIENTES
-- ============================================

-- CLIENT 1 - João Silva (SP)
MERGE INTO users (id, name, email, password_hash, role, enabled, created_at)
    KEY (email)
    VALUES (
    RANDOM_UUID(),
    'João Silva',
    'joao@email.com',
    '$2a$10$8G8J8K8L8M8N8O8P8Q8R8S8T8U8V8W8X8Y8Z8A8B8C8D8E8F8G8H8I',
    'CLIENT',
    TRUE,
    CURRENT_TIMESTAMP
    );

-- CLIENT 2 - Maria Souza (MG)
MERGE INTO users (id, name, email, password_hash, role, enabled, created_at)
    KEY (email)
    VALUES (
    RANDOM_UUID(),
    'Maria Souza',
    'maria@email.com',
    '$2a$10$8G8J8K8L8M8N8O8P8Q8R8S8T8U8V8W8X8Y8Z8A8B8C8D8E8F8G8H8I',
    'CLIENT',
    TRUE,
    CURRENT_TIMESTAMP
    );

-- CLIENT 3 - Pedro Santos (RJ)
MERGE INTO users (id, name, email, password_hash, role, enabled, created_at)
    KEY (email)
    VALUES (
    RANDOM_UUID(),
    'Pedro Santos',
    'pedro@email.com',
    '$2a$10$8G8J8K8L8M8N8O8P8Q8R8S8T8U8V8W8X8Y8Z8A8B8C8D8E8F8G8H8I',
    'CLIENT',
    TRUE,
    CURRENT_TIMESTAMP
    );

-- ============================================
-- 2. SOLICITAÇÕES
-- ============================================

-- Solicitação 1 - João Silva (SP) - INSTALLATION
INSERT INTO solicitations (
    id, client_id, status, current_step,
    service_type, title, description,
    cep, number, complement, street, neighborhood, city, state,
    priority, preferred_date, estimated_value, terms_accepted,
    created_at, updated_at, submitted_at
) VALUES (
             RANDOM_UUID(),
             (SELECT id FROM users WHERE email = 'joao@email.com'),
             'SUBMITTED',
             3,
             'INSTALLATION',
             'Instalação de Internet Fibra Ótica',
             'Solicitação de instalação de internet fibra ótica com velocidade de 500mb no endereço residencial.',
             '01310000',
             '100',
             'Apto 101',
             'Avenida Paulista',
             'Bela Vista',
             'São Paulo',
             'SP',
             'MEDIUM',
             '2026-07-25',
             150.00,
             TRUE,
             CURRENT_TIMESTAMP,
             CURRENT_TIMESTAMP,
             CURRENT_TIMESTAMP
         );

-- Solicitação 2 - Maria Souza (MG) - MAINTENANCE
INSERT INTO solicitations (
    id, client_id, status, current_step,
    service_type, title, description,
    cep, number, complement, street, neighborhood, city, state,
    priority, preferred_date, estimated_value, terms_accepted,
    created_at, updated_at, submitted_at
) VALUES (
             RANDOM_UUID(),
             (SELECT id FROM users WHERE email = 'maria@email.com'),
             'SUBMITTED',
             3,
             'MAINTENANCE',
             'Manutenção de Ar Condicionado',
             'Solicitação de manutenção preventiva em sistema de ar condicionado split. Unidade apresenta ruído anormal.',
             '30130000',
             '200',
             'Sala 502',
             'Avenida Afonso Pena',
             'Centro',
             'Belo Horizonte',
             'MG',
             'HIGH',
             '2026-07-20',
             350.00,
             TRUE,
             CURRENT_TIMESTAMP,
             CURRENT_TIMESTAMP,
             CURRENT_TIMESTAMP
         );

-- Solicitação 3 - Pedro Santos (RJ) - INSPECTION
INSERT INTO solicitations (
    id, client_id, status, current_step,
    service_type, title, description,
    cep, number, complement, street, neighborhood, city, state,
    priority, preferred_date, estimated_value, terms_accepted,
    created_at, updated_at, submitted_at
) VALUES (
             RANDOM_UUID(),
             (SELECT id FROM users WHERE email = 'pedro@email.com'),
             'DRAFT',
             1,
             'INSPECTION',
             'Inspeção de Sistema Elétrico',
             'Solicitação de inspeção preventiva em sistema elétrico do escritório. Verificar possíveis sobrecargas.',
             '20040030',
             '50',
             'Sala 301',
             'Avenida Presidente Vargas',
             'Centro',
             'Rio de Janeiro',
             'RJ',
             'LOW',
             '2026-08-10',
             80.00,
             TRUE,
             CURRENT_TIMESTAMP,
             CURRENT_TIMESTAMP,
             NULL
         );

-- ============================================
-- 3. VERIFICAR DADOS INSERIDOS (OPCIONAL)
-- ============================================
-- SELECT u.name, u.email, u.role, s.title, s.city, s.state, s.status
-- FROM users u
-- LEFT JOIN solicitations s ON u.id = s.client_id
-- WHERE u.role = 'CLIENT'
-- ORDER BY u.name;