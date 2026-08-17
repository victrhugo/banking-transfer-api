# Banking Transfer API

A simplified digital banking REST API built with Java 21 and Spring Boot 3, focused on data consistency and correctness under concurrent access — the kind of problem real banking systems have to get right.

## Why this project exists

Money transfer APIs are a classic concurrency trap: if two requests debit or credit the same account at nearly the same time, naive read-modify-write logic can lose an update or leave balances inconsistent. This project implements account management and fund transfers with that failure mode explicitly guarded against, rather than assumed away.

## Key features

- Account management and fund transfers between accounts
- Transaction history queries
- Race-condition protection on transfers via **pessimistic locking** (`PESSIMISTIC_WRITE`) on account records — a transfer acquires an exclusive lock on both the source and destination account rows before mutating balances, so concurrent transfers touching the same account serialize instead of racing
- Centralized exception handling with consistent error responses
- Automated tests (JUnit 5 + Mockito) and CI verification on every push and pull request via GitHub Actions

## Why pessimistic locking (and not optimistic)

Optimistic locking (version columns + retry-on-conflict) works well when contention is rare and clients can gracefully retry. For account balances, contention on a "hot" account can be frequent, and a failed transfer is a worse user experience than a slightly slower one. Pessimistic locking trades a bit of throughput for a strict guarantee: no transfer can read a balance that another in-flight transfer is about to change. Given the small scope of this API and the correctness-first goal, that trade-off made sense here.

## Tech stack

- Java 21, Spring Boot 3
- Spring Data JPA, PostgreSQL, Flyway migrations
- Bean Validation, Lombok
- JUnit 5, Mockito
- OpenAPI / Swagger UI
- Docker Compose (local PostgreSQL)
- GitHub Actions (CI on push/PR)

## Architecture

Layered design: controllers → services → repositories → DTOs, with exception handling centralized in a single layer rather than scattered across controllers.

## Getting started

```bash
# 1. start PostgreSQL
docker compose up -d

# 2. run the application
mvn clean spring-boot:run
```

The API runs at `http://localhost:8080`. Interactive API docs (Swagger UI) are available at `http://localhost:8080/swagger-ui.html`.

## Running tests

```bash
mvn test
```
