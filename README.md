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
- [x] Deposits
- [x] Transfers
- [ ] Concurrency control
- [x] Idempotency (sequential retries; concurrent retries pending)
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
| POST | `/api/v1/deposits` | Create a deposit |

### Create an account:

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
Code 201 Created

### Get an account:
```http
GET /api/v1/accounts/8b4d5b3e-a546-44c5-8321-6ff561150fdb
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
Code 200 Ok

### Create a deposit:
```http
POST /api/v1/deposits
Content-Type: application/json
X-Idempotency-Key: idk-123

{ "accountId": "8b4d5b3e-a546-44c5-8321-6ff561150fdb", "amount": 100, "currency": "USD" }
```

Note: The client generates the key (for example, a UUID) and reuses it only to retry the same request. Use a new key for every new deposit. 

When you send the deposit request, the system will:
1. Computes a SHA-256 hash of the deposit (Transaction Type, Account Id, Deposit Amount and Currency). 
2. When a deposit request arrives, the system: Checks if the idempotency key already exists, but the hash is not the same, the request is rejected with a 422 as the key has already been used with a different deposit information. 
3. When a deposit request arrives, the system: Checks if the idempotency key does not exist, then the system will check that the currency sent in the deposit is the same as the one configured in the customer's account
4. Creates the transaction and both entries (debit and credit) in one database transaction. 
5. On success, it responds with:

```json
{
  "transactionId": "7b2d5b30-8321-a546-44c5-6ff572220fdb",
  "type": "DEPOSIT",
  "accountId": "8b4d5b3e-a546-44c5-8321-6ff561150fdb",
  "amount": 100
}
```
Code 201 Created

> All money amounts are integers in minor units (cents). `100` means $1.00.

Errors follow the `ProblemDetail` format:

```json
{
  "title": "Validation Failed",
  "status": 400,
  "detail": "Request validation failed",
  "errors": [ { "field": "owner", "message": "must not be blank" } ]
}
```

### Transfer
```http
POST /api/v1/transfers
Content-Type: application/json
X-Idempotency-Key: idk-123

{ fromAccount": "76d4eb69-f737-4b61-b506-ddfc0995ad51", "toAccount": "4b3a44bb-5655-41ab-9e4d-8058c8794d7f", "amount": 3000,"currency": "USD" }
```

Note: The client generates the key (for example, a UUID) and reuses it only to retry the same request. Use a new key for every new deposit. 

When you send the transfer request, the system will:
1. Checks if the sending account is the same as the reciving account. This is so the system does not create unnecessary entries.
2. Computes a SHA-256 hash of the deposit (Transaction Type, Sending Account Id, Receiving Account Id, Deposit Amount and Currency).
3. When a transfer request arrives, the system: Checks if the idempotency key already exists, but the hash is not the same, the request is rejected with a 422 as the key has already been used with a different deposit information. 
4. When a transfer request arrives, the system: Checks if the idempotency key does not exist, then the system will check that the currency sent in the deposit is the same as the one configured in the customer's account
5. Then, the system checks if the transfer currency matches the currencies on both accounts.
6. It also checks if the account type on any of the accounts is a system account.
7. Finally, tne system creates the transaction, both entries and return the response.


```json
{
    "transactionId": "4172e71f-d447-466b-a692-2efb0654ee40",
    "fromAccount": "76d4eb69-f737-4b61-b506-ddfc0995ad51",
    "toAccount": "4b3a44bb-5655-41ab-9e4d-8058c8794d7f",
    "type": "TRANSFER",
    "amount": 3000
}
```
## Design decisions

**Money as integer cents.** Amounts are stored as `BIGINT` and handled as `long`, never
as `double`. Sums are exact and there are no rounding errors.

**Immutable entries.** A database trigger rejects any `UPDATE` or `DELETE` on the
`entries` table. *A mistake is corrected with a new reversing transaction (planned)*, so the full
history is always kept.

**Balanced transactions enforced by the database.** A deferred constraint checks, at
commit time, that debits equal credits in every transaction. *The rule holds even if
application code has a bug (test pending)*.

**Schema owned by Flyway.** Hibernate runs with `ddl-auto: validate`. If an entity and
a migration disagree, the application refuses to start.

**Idempotency.** The client generates an idempotency key for each operation and sends it 
in the `X-Idempotency-Key` header. This key is unique per transaction, and it's just re-used
with a retry. If the key already exists with the same content, nothing 
is written again and the original result is returned. This protects against failures such 
as a lost network response, where the client cannot know if the request succeeded and has 
to retry. If the client sends a request using an already existing Idempotency Key, but with
different content, it gets rejected with a 422. The content is compared so that a reused key 
cannot return the result of a different operation. This works for deposits and transfers

## Known limitations

- One currency per account.
- Debit and credit follow a simplified convention: debit takes money out of an
  account, credit puts money in.
- Concurrent retries with the same idempotency key are not handled yet (the database rejects the duplicate, but the client gets a generic error).
- No concurrency control yet on balances.
- The deposit response does not include the new balance. Use `GET /api/v1/accounts/{id}`.

## Tech stack

Java 21 · Spring Boot 4.1.1 · Spring Data JPA / Hibernate · PostgreSQL 18 · Flyway ·
Maven · Docker Compose · Testcontainers · springdoc OpenAPI · Lombok
