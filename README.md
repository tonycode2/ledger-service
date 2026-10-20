# Ledger Service

A double-entry ledger and payments API built with Java 21, Spring Boot and PostgreSQL.

The goal is a service where money is never lost or duplicated, even under concurrent
requests and client retries. The rules that matter are enforced by the database, not only
by application code.

> Educational project. It does not handle real money.

## What it does

- **Accounts:** create accounts, read balances, list an account's entries (paginated).
- **Deposits:** money enters a customer account from a system account (`EXTERNAL_FUNDS`).
- **Transfers:** money moves between two customer accounts.
- **Double-entry bookkeeping:** every operation writes one transaction with a debit entry and a credit entry.
- **Idempotency:** deposits and transfers can be retried safely with an `X-Idempotency-Key` header.
- **Concurrency control:** concurrent transfers never overspend an account and never deadlock.

## Highlights

| Topic | What the project does |
|---|---|
| Money | Integer minor units (`BIGINT` / `long`), never floating point. |
| Immutability | A database trigger rejects `UPDATE` and `DELETE` on `entries`. |
| Balance integrity | A deferred constraint trigger checks that debits equal credits in every transaction at commit time. |
| Concurrency | `SELECT ... FOR UPDATE` with locks taken in a fixed order (see [ADR 0001](src/main/resources/docs/adr/0001-locking-strategy.md)). |
| Idempotency | Unique key per transaction plus a SHA-256 hash of the request content. |
| Schema | Owned by Flyway. Hibernate runs with `ddl-auto: validate`. |
| Tests | Unit and integration tests against a real PostgreSQL using Testcontainers. |
| CI | GitHub Actions builds and tests on every push and pull request to `main`. |

## Status

Implemented:

- [x] Data model and balance rules
- [x] Accounts API (create, get, list entries)
- [x] Deposits
- [x] Transfers
- [x] Concurrency control on transfers
- [x] Idempotency for sequential retries
- [x] Continuous integration

Prepared in the schema but not implemented yet:

- [ ] Reversals: the `REVERSAL` transaction type, the `reverses_id` column and its database constraints exist, but there is no endpoint or service that creates a reversal yet.
- [ ] Outbox and messaging: the `outbox` table and its index exist, but nothing writes to it or publishes from it yet.

## Quick start

### Requirements

- Java 21
- Maven (or `./mvnw`)
- Docker

### Run

```bash
mvn spring-boot:run
```

Spring Boot starts the PostgreSQL 18 container from `docker-compose.yml` by itself.
Flyway then creates the tables and the `EXTERNAL_FUNDS` system account.

### Explore the API

Swagger UI: http://localhost:8080/swagger-ui.html

### Database only (optional)

```bash
docker compose up -d       # start only the database
docker compose down -v     # stop it and delete all data
```

### Run the tests

```bash
mvn test
```

Docker must be running, because the integration tests start a PostgreSQL container with Testcontainers.

## API

| Method | Path | Description |
|---|---|---|
| POST | `/api/v1/accounts` | Create an account |
| GET | `/api/v1/accounts/{id}` | Get account details and balance |
| GET | `/api/v1/accounts/{id}/entries` | List the entries of an account (paginated) |
| POST | `/api/v1/deposits` | Create a deposit |
| POST | `/api/v1/transfers` | Create a transfer |

> All money amounts are integers in minor units (cents). `100` means $1.00.

### Create an account

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

`201 Created`

### Get an account

```http
GET /api/v1/accounts/8b4d5b3e-a546-44c5-8321-6ff561150fdb
```

Returns the same body as above with `200 OK`.

### Create a deposit

```http
POST /api/v1/deposits
Content-Type: application/json
X-Idempotency-Key: idk-123

{ "accountId": "8b4d5b3e-a546-44c5-8321-6ff561150fdb", "amount": 100, "currency": "USD" }
```

```json
{
  "transactionId": "7b2d5b30-8321-a546-44c5-6ff572220fdb",
  "type": "DEPOSIT",
  "accountId": "8b4d5b3e-a546-44c5-8321-6ff561150fdb",
  "amount": 100
}
```

`201 Created`

What the service does with a deposit:

1. Computes a SHA-256 hash of the request (type, account id, amount, currency).
2. If the idempotency key already exists with the same hash, it returns the stored result and writes nothing.
3. If the key exists with a different hash, it rejects the request with `422`.
4. If the key is new, it checks that the deposit currency matches the account currency.
5. It creates the transaction and both entries (debit and credit) in one database transaction.

### Create a transfer

```http
POST /api/v1/transfers
Content-Type: application/json
X-Idempotency-Key: idk-456

{
  "fromAccount": "76d4eb69-f737-4b61-b506-ddfc0995ad51",
  "toAccount": "4b3a44bb-5655-41ab-9e4d-8058c8794d7f",
  "amount": 3000,
  "currency": "USD"
}
```

```json
{
  "transactionId": "4172e71f-d447-466b-a692-2efb0654ee40",
  "fromAccount": "76d4eb69-f737-4b61-b506-ddfc0995ad51",
  "toAccount": "4b3a44bb-5655-41ab-9e4d-8058c8794d7f",
  "type": "TRANSFER",
  "amount": 3000
}
```

`201 Created`

What the service does with a transfer:

1. Rejects a transfer where the source and destination are the same account.
2. Computes a SHA-256 hash of the request (type, source, destination, amount, currency).
3. Checks the idempotency key. Same key with the same hash returns the stored result; same key with a different hash returns `422`.
4. Locks both accounts with `SELECT ... FOR UPDATE`, smaller id first.
5. Checks that the currency matches both accounts, that neither account is a system account, and that the source has enough funds.
6. Creates the transaction and both entries in one database transaction.

The client generates the idempotency key (for example, a UUID) and reuses it only to retry the same request. A new operation needs a new key.

### Errors

Errors follow the `ProblemDetail` format:

```json
{
  "title": "Validation Failed",
  "status": 400,
  "detail": "Request validation failed",
  "errors": [ { "field": "owner", "message": "must not be blank" } ]
}
```

Lock and version conflicts are returned as `409 Conflict`, which tells the client that retrying is reasonable.

## Design decisions

**Money as integer cents.** Amounts are stored as `BIGINT` and handled as `long`. Sums are exact and there are no rounding errors.

**Immutable entries.** A database trigger rejects any `UPDATE` or `DELETE` on the `entries` table, so the history is always kept. The schema already supports correcting a mistake with a new `REVERSAL` transaction; the reversal operation itself is not implemented yet.

**Balanced transactions enforced by the database.** A deferred constraint trigger checks, at commit time, that debits equal credits in every transaction. The rule holds even if application code has a bug. Migration tests cover this rule, the immutability triggers and the positive-amount check.

**Schema owned by Flyway.** Hibernate runs with `ddl-auto: validate`. If an entity and a migration disagree, the application refuses to start.

**Idempotency.** The client sends an `X-Idempotency-Key` header. If the key already exists with the same content, nothing is written again and the original result is returned. This protects against failures such as a lost network response, where the client cannot know whether the request succeeded. If the key is reused with different content, the request is rejected with `422`, so a reused key can never return the result of a different operation.

**Pessimistic locking with ordered acquisition.** Two concurrent transfers from the same account can both read the same balance and both approve, spending more than the account holds. Transfers lock both accounts with `SELECT ... FOR UPDATE`, always in ascending id order, so the second request waits and then reads the committed balance. The fixed order also prevents deadlocks between opposite transfers (A to B and B to A). `@Version` stays as a safety net. The full reasoning, the alternatives considered and the measured results are in [ADR 0001](src/main/resources/docs/adr/0001-locking-strategy.md).

| Scenario | Accepted | Insufficient funds | Conflicts | Lock failures |
|---|---|---|---|---|
| `@Version` only | 12 | 88 | 100 | 0 |
| Pessimistic + ordered locks | 100 | 100 | 0 | 0 |
| Opposite transfers, locks not ordered | 84 | n/a | 16 | 16 |
| Opposite transfers, locks ordered | 200 | n/a | 0 | 0 |

The first two rows are 200 concurrent transfers of 1 from a balance of 100. The last two are 100 transfers A to B and 100 transfers B to A at the same time. Measured with Java 21 and PostgreSQL 18 through Testcontainers.

## Testing

- Unit tests for the domain model, DTOs, services and exception handling.
- Integration tests against a real PostgreSQL 18 with Testcontainers.
- Migration tests for the database rules (unbalanced transactions, immutable entries, invalid amounts).
- Concurrency tests (`TransferConcurrencyTest`): invariants under concurrent load, exactly the affordable transfers succeed, and no deadlocks between opposite transfers.

## Known limitations

- One currency per account.
- Debit and credit follow a simplified convention: debit takes money out of an account, credit puts money in.
- Deposits are not protected by the locking pattern yet. Every deposit updates the same `EXTERNAL_FUNDS` row, so concurrent deposits can conflict.
- Concurrent retries with the same idempotency key are only guarded by the `UNIQUE` constraint: the database rejects the duplicate, but the client gets a generic error.
- No lock timeout is configured yet.
- The deposit response does not include the new balance. Use `GET /api/v1/accounts/{id}`.
- Reversals and the outbox are not implemented (see Status).

## Tech stack

Java 21 · Spring Boot · Spring Data JPA / Hibernate · PostgreSQL 18 · Flyway · Maven · Docker Compose · Testcontainers · springdoc OpenAPI · GitHub Actions · Lombok

## License

See [LICENSE](LICENSE).
