# API de Solicitações de Atendimento

## 📋 Sobre o Projeto

API REST para gerenciamento de solicitações de atendimento com fluxo multi-step (3 etapas), autenticação JWT, controle de acesso baseado em roles (CLIENT, ANALYST, ADMIN), integração com ViaCEP, auditoria com AOP e busca avançada com Elasticsearch.

---

## 🎯 Status do Projeto

| Módulo                  | Status      |
|-------------------------|-------------|
| Autenticação JWT        | ✅ Concluído |
| Multi-Step Solicitation | ✅ Concluído |
| Análise (ANALYST)       | ✅ Concluído |
| Administração (ADMIN)   | ✅ Concluído |
| Auditoria (AOP)         | ✅ Concluído |
| Elasticsearch           | ✅ Concluído |
| Testes                  | ✅ Concluído |
| Documentação            | ✅ Concluído |
| Collection Postman      | ✅ Concluído |

---

## 🚀 Tecnologias

| Tecnologia | Versão | Descrição |
|------------|--------|-----------|
| Java | 21 | Linguagem de programação |
| Spring Boot | 3.2.0 | Framework principal |
| Spring Security | 6.2.0 | Autenticação e autorização |
| JWT | 0.12.3 | Autenticação stateless |
| Spring Data JPA | 3.2.0 | Persistência de dados |
| PostgreSQL | 16 | Banco de dados relacional |
| H2 | 2.2.224 | Banco de dados em memória (testes) |
| Elasticsearch | 8.11.0 | Busca e indexação |
| Flyway | 9.22.3 | Migrações de banco de dados |
| OpenFeign | 4.1.0 | Cliente HTTP para ViaCEP |
| Lombok | 1.18.30 | Redução de boilerplate |
| SpringDoc OpenAPI | 2.5.0 | Documentação Swagger |
| Docker | - | Containerização |

---

## 📁 Estrutura do Projeto
````
src/main/java/br/com/solicitacao/
├── api/
│ ├── annotation/ # Anotações customizadas (@Audit)
│ ├── controller/ # Controllers REST
│ ├── dto/ # DTOs de request/response
│ └── handler/ # Tratamento de exceções
├── core/
│ ├── domain/ # Entidades de domínio e enums
│ ├── exception/ # Exceções de negócio
│ └── service/ # Regras de negócio
└── infrastructure/
├── aop/ # Aspectos (Auditoria)
├── client/ # Clientes HTTP (ViaCEP)
├── config/ # Configurações (Security, JWT, Elasticsearch)
├── elasticsearch/ # Documentos e repositórios Elasticsearch
└── persistence/ # Entidades JPA e repositórios
````

## 🔐 Roles e Permissões

| Role | Descrição | Permissões |
|------|-----------|------------|
| **CLIENT** | Cliente comum | Criar/editar suas solicitações, salvar rascunho, enviar para análise |
| **ANALYST** | Analista | Listar/analisar solicitações das UFs que cobre, decidir (Aprovar/Rejeitar) |
| **ADMIN** | Administrador | Gerenciar tudo, criar usuários (ANALYST/ADMIN), configurar cobertura de analistas |

---

## 🚀 Como Executar

### Pré-requisitos

- Java 21+
- Docker e Docker Compose (recomendado)
- Maven 3.9+

### Opção 1: Com Docker (Recomendado)

````bash
# Clone o repositório
git clone https://github.com/eduardomarra/solicitacao-api.git
cd solicitacao-api

# Subir todos os serviços
docker-compose up -d

# Aguardar a inicialização
docker-compose logs -f app
````

### Opção 2: Localmente (com H2)
````bash
# Compilar
mvn clean compile

# Executar
mvn spring-boot:run
````

### Opção 3: Localmente (com PostgreSQL e Elasticsearch via Docker)
````bash
# Subir apenas os serviços necessários
docker-compose up -d postgres elasticsearch

# Executar a aplicação
mvn spring-boot:run -Dspring.profiles.active=dev
````

## 📡 Endpoints Principais

### 🔐 Autenticação
| Método | Endpoint       | Descrição                       | Role    |
|--------|----------------|---------------------------------|---------|
| POST   | /auth/register | Registrar novo usuário (CLIENT) | Público |
| POST   | /auth/login    | Login e obtenção de token       | Público |
| POST   | /admin/users   | Criar usuário (ANALYST/ADMIN)   | ADMIN   |

### 📝 Solicitações (CLIENT)

| Método | Endpoint                   | Descrição                             |
|--------|----------------------------|---------------------------------------|
| POST   | /solicitations             | Criar nova solicitação (rascunho)     |
| PUT    | /solicitations/{id}/step1  | Salvar Step 1 - Dados básicos         |
| PUT    | /solicitations/{id}/step2  | Salvar Step 2 - Endereço (com ViaCEP) |
| PUT    | /solicitations/{id}/step3  | Salvar Step 3 - Confirmação           |
| POST   | /solicitations/{id}/submit | Enviar solicitação para análise       |
| GET    | /solicitations/{id}        | Buscar solicitação                    |

### 📊 Análise (ANALYST)

| Método | Endpoint                           | Descrição                            |
|--------|------------------------------------|--------------------------------------|
| GET    | /analyst/solicitations             | Listar solicitações das UFs cobertas |
| GET    | /analyst/solicitations/{id}        | Buscar solicitação                   |
| POST   | /analyst/solicitations/{id}/start  | Iniciar análise                      |
| POST   | /analyst/solicitations/{id}/decide | Decidir (APPROVE/REJECT)             |
| GET    | /analyst/solicitations/search      | Busca avançada com Elasticsearch     |

### 🛡️ Administração (ADMIN)

| Método | Endpoint                           | Descrição                        |
|--------|------------------------------------|----------------------------------|
| GET    | /admin/solicitations               | Listar todas as solicitações     |
| GET    | /admin/solicitations/{id}          | Buscar solicitação por ID        |
| PUT    | /admin/analysts/{userId}/coverage  | Configurar cobertura do analista |
| GET    | /admin/analysts/{userId}/coverage  | Buscar cobertura do analista     |


## Parâmetros de busca

| Parâmetro   | Tipo    | Descrição                                                    |
|-------------|---------|--------------------------------------------------------------|
| text        | String  | Busca em title e description                                 |
| statuses    | List    | Filtro por status (SUBMITTED, IN_REVIEW, APPROVED, REJECTED) |
| states      | List    | Filtro por UF (SP, RJ, MG, etc.)                             |
| serviceType | String  | Filtro por tipo de serviço                                   |
| priority    | String  | Filtro por prioridade (LOW, MEDIUM, HIGH)                    |
| dateFrom    | Date    | Data inicial                                                 |
| dateTo      | Date    | Data final                                                   |
| page        | Integer | Número da página (0-indexed)                                 |
| size        | Integer | Tamanho da página                                            |
| sort        | String  | Ordenação (ex: createdAt,desc)                               |


### 🐳 Docker Compose
````bash

# Subir todos os serviços
docker-compose up -d

# Ver logs
docker-compose logs -f app

# Parar todos os serviços
docker-compose down

# Parar e remover volumes
docker-compose down -v

# Reconstruir e subir
docker-compose up -d --build
````

## Serviços

| Serviço	       | Porta	 | Descrição         |
|----------------|--------|-------------------|
| Aplicação	     | 8080	  | Spring Boot API   |
| PostgreSQL	    | 5432	  | Banco de dados    |
| Elasticsearch	 | 9200	  | Busca e indexação |


## 📚 Documentação da API

### Swagger UI
````
text
http://localhost:8080/api/swagger-ui/index.html
````

### OpenAPI JSON
````
text
http://localhost:8080/api/v3/api-docs
````

### H2 Console (Desenvolvimento)
````
text
http://localhost:8080/api/h2-console
````

 JDBC URL: jdbc:h2:mem:solicitacao_db
 User: sa
 Password: (deixe em branco)

***

## 🧪 Testes

### Executar todos os testes
````bash
mvn test
````

### Executar testes específicos
````bash
mvn test -Dtest=AuthIntegrationTest
````

***

## 📊 Auditoria

O sistema utiliza AOP (Aspect Oriented Programming) para auditoria das ações críticas.

### Ações auditadas
- SUBMIT - SOLICITATION
- START_ANALYSIS - SOLICITATION
- DECIDE - SOLICITATION
- CREATE_USER - USER

### Tabela audit_logs

````sql
SELECT * FROM audit_logs ORDER BY created_at DESC;
````
***

## 🔧 Variáveis de Ambiente

| Variável                   | Descrição              | Padrão                                         |
|----------------------------|------------------------|------------------------------------------------|
| JWT_SECRET                 | Chave secreta para JWT | qU8sK2pL5xR9mZ7cW3vA4nY6bT1gH0jF               |
| SPRING_DATASOURCE_URL      | URL do banco de dados  | jdbc:postgresql://postgres:5432/solicitacao_db |
| SPRING_DATASOURCE_USERNAME | Usuário do banco       | admin                                          |
| SPRING_DATASOURCE_PASSWORD | Senha do banco         | 123456                                         |

***

## 📝 Regras de Negócio

### Step 1 - Dados Básicos

- serviceType: Obrigatório (INSTALLATION, MAINTENANCE, INSPECTION)
- title: 3 a 80 caracteres
- description: 20 a 1000 caracteres

### Step 2 - Endereço

- cep: Obrigatório, validado com ViaCEP
- number: Obrigatório, 1 a 20 caracteres
- state: UF com 2 letras maiúsculas
- CEP inválido → não permite concluir Step 2

### Step 3 - Confirmação

- priority: Obrigatório (LOW, MEDIUM, HIGH)
- preferredDate: Não pode ser no passado
- estimatedValue: >= 0
- termsAccepted: Deve ser true
- priority = HIGH → estimatedValue >= 100

### Submit (Envio para análise)

- Todos os steps devem estar completos
- Status muda de DRAFT para SUBMITTED
- Bloqueia edições pelo CLIENT

***

## 🤝 Contribuição

- Fork o projeto
- Crie uma branch (git checkout -b feature/nova-feature)
- Commit suas alterações (git commit -m 'Adiciona nova feature')
- Push para a branch (git push origin feature/nova-feature)
- Abra um Pull Request

***

## 📄 Licença

MIT License

## 📞 Contato

Email: eduardomarra@gmail.com\
GitHub: https://github.com/Eduardomarra
