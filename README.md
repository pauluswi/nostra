# Nostra

A small, fast local banking simulator designed for quick iteration without Docker.

## Why this version is different

This project intentionally avoids PostgreSQL in Docker for local development and instead uses an in-memory H2 database. That keeps the app startup fast and makes it easy to run tests, mock scenarios, and validate business rules without container overhead.

The original project goal remains the same: simulate a core-banking domain with explicit account validation, transfer logic, routines, and safe financial rules.

## Run locally

Prerequisites:
- Java 21+ (the development environment is using JDK 25-compatible toolchain)
- Maven

From the repository root:

```bash
mvn spring-boot:run
```

Then open:
- http://localhost:8080/actuator/health
- http://localhost:8080/h2-console

## Seed data

The application loads a couple of seeded accounts at startup so the transfer flow can be tested immediately.

## Example API calls

```bash
curl http://localhost:8080/api/v1/accounts
curl -X POST http://localhost:8080/api/v1/transfers \
  -H 'Content-Type: application/json' \
  -d '{
    "sourceAccountNumber": "ACC-1001",
    "destinationAccountNumber": "ACC-1002",
    "amount": "75.00",
    "currency": "USD",
    "idempotencyKey": "transfer-001"
  }'
```

## Notes

- No Docker is required for the local fast path.
- This is an in-memory development profile, not a production database choice.
- The architecture is intentionally kept simple to maximize developer speed.
