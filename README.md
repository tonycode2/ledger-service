# Ledg-Tom
An educational double-entry ledger and payments API built with Java 21 and Spring Boot.

The goal is to build a service where money is never lost or duplicated, even under
concurrent requests and client retries.

> Educational project. It does not handle real money.

## Status

🚧 Under active development.

- [x] Project setup
- [x] Data model and balance rules
- [ ] Transfers and deposits
- [ ] Concurrency control
- [ ] Idempotency
- [ ] Outbox and messaging

## Used Stack
- Java 21
- Spring Boot 4.1.1
- Docker
- Testcontainers
- JPA
- Hibernate
- PostgreSQL
- FlyWay
- Validation