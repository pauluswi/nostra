# Nostra

[![Java 21+](https://img.shields.io/badge/Java-21%2B-ED8B00?logo=openjdk&logoColor=white)](https://openjdk.java.net/)
[![Spring Boot 3.4.2](https://img.shields.io/badge/Spring%20Boot-3.4.2-6DB33F?logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![H2 in-memory](https://img.shields.io/badge/Database-H2%20in--memory-003545?logo=h2&logoColor=white)](https://www.h2database.com/)
[![No Docker](https://img.shields.io/badge/Local%20run-No%20Docker-4B5563?logo=docker&logoColor=white)](https://www.docker.com/)
[![Maven Wrapper](https://img.shields.io/badge/Build-Maven%20Wrapper-5C5C5C?logo=apachemaven&logoColor=white)](https://maven.apache.org/)

A small, fast local banking simulator designed for quick iteration without Docker.

## Why this version is different

This project intentionally avoids PostgreSQL in Docker for local development and instead uses an in-memory H2 database. That keeps the app startup fast and makes it easy to run tests, mock scenarios, and validate business rules without container overhead.

The original project goal remains the same: simulate a core-banking domain with explicit account validation, transfer logic, routines, and safe financial rules.

## Foundation and startup

This is the first milestone in the showcase build: creating a runnable local application foundation with an explicit local-dev profile and reproducible startup path.

### Prerequisites
- Java 21+ (JDK 25 is also compatible in this environment)
- Maven wrapper is included; no global Maven installation is required

### Run locally

From the repository root:

```bash
./mvnw spring-boot:run
```

Optional environment overrides:

```bash
export APP_PROFILE=local
export SERVER_PORT=8080
export DB_URL='jdbc:h2:mem:nostra;MODE=PostgreSQL;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE'
./mvnw spring-boot:run
```

Then open:
- http://localhost:8080/actuator/health
- http://localhost:8080/h2-console

A sample `.env.example` file is included at the repository root for local overrides.

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

## OpenAPI and API docs

The project exposes Swagger UI for the local API profile.

Open:
- http://localhost:8080/swagger-ui/index.html
- http://localhost:8080/v3/api-docs

## Demo script

A short walkthrough for a reviewer:

1. Start the app with `./mvnw spring-boot:run`
2. Check health: `curl http://localhost:8080/actuator/health`
3. List accounts: `curl http://localhost:8080/api/v1/accounts`
4. Submit a transfer:
   ```bash
   curl -X POST http://localhost:8080/api/v1/transfers \
     -H 'Content-Type: application/json' \
     -d '{
       "sourceAccountNumber": "ACC-1001",
       "destinationAccountNumber": "ACC-1002",
       "amount": "75.00",
       "currency": "USD",
       "idempotencyKey": "demo-transfer-001"
     }'
   ```
5. Re-submit the same idempotency key to confirm duplicate protection
6. Create a loan:
   ```bash
   curl -X POST http://localhost:8080/api/v1/loans \
     -H 'Content-Type: application/json' \
     -d '{
       "borrowerAccountNumber": "ACC-1001",
       "principal": "1200.00",
       "currency": "USD",
       "annualInterestRate": "12.00",
       "termMonths": 12
     }'
   ```
7. View the generated schedule: `curl http://localhost:8080/api/v1/loans/{loanReference}/schedule`
8. Inspect transfer audit: `curl http://localhost:8080/api/v1/transfers/{reference}/audit`

## Notes

- No Docker is required for the local fast path.
- This is an in-memory development profile, not a production database choice.
- The architecture is intentionally kept simple to maximize developer speed.
- The app uses environment-based config so the local setup can be adjusted without editing source files.
- The project is a simulator and intentionally not a native Temenos runtime.
