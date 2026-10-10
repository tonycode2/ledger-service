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

## How to test it

You should have the following tools installed:
- Java 21
- Docker

Then, if you open the project in Visual Studio Code you also need:

- [Spring Boot Extension Pack](https://marketplace.visualstudio.com/items?itemName=vmware.vscode-boot-dev-pack)
- [Extension Pack for Java](https://marketplace.visualstudio.com/items?itemName=vscjava.vscode-java-pack)

And just run the project! The extensions added in this project will create the database, the tables and will add the system user to the database. It will also create the container with PostgreSQL 18 for the database. 

If you decide to generate the container by hand, you should run

```
docker compose up -d
```

This command will create the container and volumes necessary for the database. If, for testing reasons you need to delete the volumes, you should run

```
docker compose down -v
```

Also, if you need to start the project with the command line, you should run

```
mvn spring-boot:run
``` 

At this moment, this project is not hosted anywhere, so a local run is needed. But I will have it hosted very soon!

## What can I do with this API?

At this moment, you can create an account with the API. Also, if you have the account ID you can see the details of your account.

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