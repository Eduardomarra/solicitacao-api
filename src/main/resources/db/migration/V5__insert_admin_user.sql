-- Inserir usuário ADMIN inicial
-- Senha: admin123 (criptografada com BCrypt)
INSERT INTO users (id, name, email, password_hash, role, enabled)
VALUES (
           uuid_generate_v4(),
           'Administrador',
           'admin@sistema.com',
           '$2a$10$8G8J8K8L8M8N8O8P8Q8R8S8T8U8V8W8X8Y8Z8A8B8C8D8E8F8G8H8I',
           'ADMIN',
           TRUE
       ) ON CONFLICT (email) DO NOTHING;