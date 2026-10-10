# Ledg-Tom

An educational double-entry ledger and payments API built with Java 21 and Spring Boot.

The goal is to build a service where money is never lost or duplicated, even under
concurrent requests and client retries.

> Educational project. It does not handle real money.

## Status

🚧 Under active development.

- [x] Project setup
- [x] Data model and balance rules
- [x] Accounts API (create and get)
- [ ] Deposits and transfers
- [ ] Concurrency control
- [ ] Idempotency
- [ ] Outbox and messaging

## Quick start

### Requirements

- Java 21
- Maven (or use `./mvnw` if the wrapper is included)
- Docker

### Run

```bash
mvn spring-boot:run
```

Spring Boot starts the PostgreSQL 18 container from `docker-compose.yml` by itself.
Flyway then creates the tables and the `EXTERNAL_FUNDS` system account.

### Check that it works

Open the Swagger UI: http://localhost:8080/swagger-ui.html

### Database (optional)

```bash
docker compose up -d       # start only the database
docker compose down -v     # stop it and delete all data (start from scratch)
```

The project is not deployed yet. Run it locally.

## API

| Method | Path | Description |
|---|---|---|
| POST | `/api/v1/accounts` | Create an account |
| GET | `/api/v1/accounts/{id}` | Get account details and balance |

Create an account:

```http
POST /api/v1/accounts
Content-Type: application/json

{ "owner": "Ana", "currency": "USD" }
```

```json
{
  "id": "8b4d5b3e-a546-44c5-8321-6ff561150fdb",
  "owner": "Ana",
  "currency": "USD",
  "balance": 0,
  "createdAt": "2026-10-07T15:30:00Z"
}
```

> All money amounts are integers in minor units (cents). `1050` means $10.50.

Errors follow the `ProblemDetail` format:

```json
{
  "title": "Validation Failed",
  "status": 400,
  "detail": "Request validation failed",
  "errors": [ { "field": "owner", "message": "must not be blank" } ]
}
```

## Design decisions

**Money as integer cents.** Amounts are stored as `BIGINT` and handled as `long`, never
as `double`. Sums are exact and there are no rounding errors.

**Immutable entries.** A database trigger rejects any `UPDATE` or `DELETE` on the
`entries` table. A mistake is corrected with a new reversing transaction, so the full
history is always kept.

**Balanced transactions enforced by the database.** A deferred constraint checks, at
commit time, that debits equal credits in every transaction. The rule holds even if
application code has a bug.

**Schema owned by Flyway.** Hibernate runs with `ddl-auto: validate`. If an entity and
a migration disagree, the application refuses to start.

## Known limitations

- One currency per account.
- Debit and credit follow a simplified convention: debit takes money out of an
  account, credit puts money in.

## Tech stack

Java 21 · Spring Boot 4.1.1 · Spring Data JPA / Hibernate · PostgreSQL 18 · Flyway ·
Maven · Docker Compose · Testcontainers · springdoc OpenAPI · Lombok