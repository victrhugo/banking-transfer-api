# Banking API

API REST para um banco digital simplificado, implementada em Java 21 com Spring Boot 3.

## Descrição do projeto

Esta aplicação oferece gestão de contas, transferência de fundos e consulta de movimentações com foco em:

- arquitetura em camadas
- validações via Bean Validation
- tratamento global de exceções
- proteção contra condições de corrida em transferências
- documentação Swagger/OpenAPI
- testes unitários para regras de negócio

## Tecnologias utilizadas

- Java 21
- Spring Boot 3
- Maven
- Spring Data JPA
- PostgreSQL
- JUnit 5
- Mockito
- Bean Validation
- Lombok
- Springdoc OpenAPI

## Como executar

1. Inicie o banco de dados PostgreSQL:

```bash
docker compose up -d
```

2. Execute a aplicação:

```bash
mvn clean spring-boot:run
```

### Usando migrações (Flyway)

O projeto usa Flyway para migrações de banco. O arquivo de migração inicial está em `src/main/resources/db/migration/V1__init.sql` e já inclui contas de exemplo.

Ao subir o PostgreSQL via Docker Compose e iniciar a aplicação, o Flyway aplicará as migrations automaticamente.

### Integração contínua

Um workflow GitHub Actions (`.github/workflows/ci.yml`) roda `mvn clean verify` em pushes e PRs nas branches `main`/`master`.

3. Acesse a API em:

```
http://localhost:8080
```

## Como subir PostgreSQL usando Docker

O projeto inclui `docker-compose.yml` com configuração pronta:

```bash
docker compose up -d
```

Credenciais padrão:

- banco: `compass_bank`
- usuário: `compass`
- senha: `compass`
- porta: `5432`

## Como acessar Swagger

Após iniciar a aplicação, abra:

```
http://localhost:8080/swagger-ui.html
```

## Estrutura do projeto

```
src/main/java/com/compassuol/bank
  ├── account
  │   ├── controller
  │   ├── dto
  │   ├── entity
  │   ├── repository
  │   └── service
  ├── transfer
  │   ├── controller
  │   ├── dto
  │   ├── entity
  │   ├── repository
  │   └── service
  ├── notification
  │   └── service
  └── common
      ├── config
      ├── exception
      └── handler
```

## Decisões arquiteturais

- camada de controller para entrada HTTP
- camada de serviço para lógica de negócio
- camada de repositório para acesso a dados
- DTOs como contracts de API para evitar exposição de entidades
- tratamento de exceções centralizado para respostas consistentes

## Estratégia utilizada para concorrência

A transferência utiliza `PESSIMISTIC_WRITE` no `AccountRepository` para bloquear registros de conta durante a operação.
Isso evita condições de corrida em cenários de alta concorrência e garante que saldo não seja alterado simultaneamente por duas transações.

## Como executar os testes

```bash
mvn test
```
