# arc42 - Architecture Documentation

## 1. Introduction and Goals

### 1.1 Overview
This project is a local Java 25 / Spring Boot banking simulator used to teach and demonstrate core-banking concepts in a small, testable, and auditable form. It models selected Temenos T24/T24-inspired Funds Transfer (FT) and Lending (LD) workflows without claiming to reproduce proprietary Temenos runtime behavior.

The system is intentionally scoped to a modular monolith that can be run locally with PostgreSQL and Docker Compose. The goal is to demonstrate:

- routine-style processing
- business-rule validation
- account management
- atomic funds transfer
- idempotent financial operations
- transaction lifecycle and audit trails
- fixed-term loan calculations and repayments
- application observability and local development ergonomics

### 1.2 Business and technical goals
- Provide a reproducible local developer setup
- Demonstrate a small but realistic core-banking domain model
- Guarantee financial invariants with explicit validation and transactional integrity
- Keep the project understandable for review and education
- Make the simulator clearly distinct from native Temenos products and runtime behavior

### 1.3 Stakeholders
- Developers and reviewers learning banking-domain architecture
- QA and test engineers validating transaction integrity
- Demonstration users running sample transfer and loan scenarios
- Maintainers wanting a simple, deterministic architecture with good test coverage

### 1.4 Constraints
- Java 25
- Spring Boot compatible with Java 25
- PostgreSQL as authoritative store
- Docker Compose for local orchestration
- Maven build with versioned migrations
- Simulated business logic only; no real Temenos environment required

---

## 2. Architecture Constraints

The first release is intentionally a modular monolith. The architecture avoids distributed systems and keeps dependencies simple so that correctness and financial invariants remain easier to inspect and test.

Key constraints:

- Single PostgreSQL database for the initial release
- Domain logic separated from HTTP and persistence concerns where practical
- No reliance on Kafka or other messaging infrastructure for FT/LD correctness
- Monetary values represented with exact decimal semantics (BigDecimal)
- Business rules configurable in a simple, explicit format
- All financial operations must be atomic and idempotent

---

## 3. Context and Scope

### 3.1 System context
The system is a self-contained application that exposes REST endpoints for account lookup, transfer submission, routine execution, loan lifecycle operations, and health checks.

It interacts with:

- PostgreSQL for account, transfer, posting, audit, and loan records
- Docker Compose-managed local services for developer environment
- External clients via REST/JSON APIs

### 3.2 Scope
In scope for the first milestone:

- routine contract and dispatcher
- configurable business rules
- account model and seed data
- funds transfer flow with validation and idempotency
- audit trail and transaction lifecycle
- health endpoint and startup config

Later scope includes:

- fixed-term loan calculation and repayment workflow
- richer observability and demo polish
- OpenAPI examples and walkthrough scripts

### 3.3 Out of scope
- Native Temenos runtime compatibility
- Full T24 application replication
- Real payment rails or production ledger behavior
- Full AA/LD product configuration parity
- UI IDE or full Temenos user experience

---

## 4. Solution Strategy

The architecture follows a layered, domain-centered approach:

1. Domain layer
   - Account, transfer, posting, audit, routine, and lending concepts
   - Business rules, validation logic, and calculation rules
   - Explicit financial invariants

2. Application layer
   - Use cases such as creating accounts, executing transfers, and generating loan schedules
   - Transaction coordination and idempotency handling
   - Routine dispatch and business rejection handling

3. Persistence layer
   - Spring Data JPA or JDBC-backed repositories
   - Flyway migrations for schema evolution
   - Database constraints for uniqueness and referential integrity

4. API layer
   - REST controllers with DTOs
   - Stable error model
   - Versioned endpoints under /api/v1

5. Operations layer
   - Health endpoint
   - Structured logs and correlation identifiers
   - Docker Compose startup and PostgreSQL health checks

This strategy prioritizes correctness, clarity, and testability over breadth of features.

---

## 5. Building Block View

### 5.1 Top-level decomposition

```text
+---------------------------+
| Client / API Consumers    |
| - curl, tests, UI, demo   |
+-------------+-------------+
              |
              v
+---------------------------+
| REST API Layer             |
| Controllers / DTOs        |
+-------------+-------------+
              |
              v
+---------------------------+
| Application Services      |
| Use cases / orchestration  |
| routine dispatch           |
| validation + idempotency  |
+-------------+-------------+
              |
              v
+---------------------------+
| Domain Layer               |
| rules, accounts, FT, LD   |
| transaction lifecycle      |
| audit + calculations       |
+-------------+-------------+
              |
              v
+---------------------------+
| Persistence Layer          |
| JPA/JDBC + PostgreSQL      |
| Flyway migrations         |
+---------------------------+
```

### 5.2 Main modules

- account
  - account entity, status values, balance rules, seed/supporting initialization

- fundstransfer
  - transfer request and processing flow
  - atomic debit/credit posting
  - idempotency handling

- routine
  - routine interface, dispatcher, execution context, result types

- rules
  - transfer validation rules
  - stable machine-readable error codes

- transaction
  - explicit transaction states and allowed transitions
  - lifecycle tracking

- audit
  - action records, timestamps, transaction references, correlation identifiers

- lending
  - loan model, schedule generation, repayment flow, rounding policy

- shared
  - common values, money handling, exception types, UUID generation, currency validation

- configuration
  - Spring configuration, YAML properties, startup validation, environment variables

---

## 6. Runtime View

### 6.1 Request flow: create account

```text
Client -> API: POST /api/v1/accounts
API -> Application: CreateAccountCommand
Application -> Domain: Validate account data
Domain -> Persistence: Persist account
Persistence -> API: Account created
API -> Client: 201 Created + DTO
```

### 6.2 Request flow: funds transfer

```text
Client -> API: POST /api/v1/transfers
API -> Application: SubmitTransferCommand
Application -> Domain: Validate amount, currency, status, balance
Application -> Persistence: Lock source + destination accounts
Application -> Domain: Create transfer, postings, audit
Application -> Persistence: Commit transaction
API -> Client: transfer result + status
```

### 6.3 Runtime guarantees
- A transfer is executed inside a database transaction
- Account balance changes are only applied through controlled use cases
- Duplicate requests with identical idempotency keys return the same logical outcome
- Business rejections are distinct from infrastructure failures
- Failed database transactions leave no partial financial effects

---

## 7. Deployment View

### 7.1 Local development deployment

```text
+------------------+
| Docker Compose   |
| - PostgreSQL     |
| - App container  |
+---------+--------+
          |
          v
+------------------+
| Java app         |
| Spring Boot      |
| port: app port   |
+---------+--------+
          |
          v
+------------------+
| PostgreSQL       |
| schema + tables  |
+------------------+
```

### 7.2 Environment configuration
The application must be configured using environment variables and/or Spring config. Example concerns include:

- database host, port, database name, credentials
- application port and profiles
- feature toggles and rule configuration
- any local demo or seed configuration

The project must avoid committing secrets and must fail clearly when required configuration is missing or invalid.

---

## 8. Cross-Cutting Concepts

### 8.1 Domain invariants
The implementation must enforce and test important rules:

- completed transfers have balanced debit and credit postings
- rejected transfers do not change balances
- account status checks are enforced before posting
- source balance cannot be reduced below available limit
- idempotency keys are applied safely and deterministically
- loan schedule calculations follow a documented rounding and date convention

### 8.2 Transaction and audit model
The simulator uses explicit transaction states and includes an audit trail for lifecycle events. Each action should include:

- transaction reference
- timestamp
- action name
- outcome
- correlation identifier

Sensitive values are not dumped into logs unnecessarily.

### 8.3 Business rules
Rules are configurable and validated at startup. The initial rule set covers:

- positive transfer amount
- eligible account state
- matching transfer currency
- sufficient available balance

Rules are machine-readable and should be testable independently of controllers.

### 8.4 Idempotency
The project treats idempotency as a first-class concern. Reusing the same idempotency key with the same payload returns the original result; using the same key with a different payload is rejected.

### 8.5 Observability
The system exposes at least a health endpoint and should emit structured logs for transaction processing and routine execution. Correlation identifiers are recommended for tracing a request or financial flow through logs.

---

## 9. Architecture Decisions

### ADR-01: Modular monolith
Use one deployable application with clear package/module boundaries instead of microservices.

Reason:
- simpler to run locally
- easier to reason about transaction boundaries
- faster to implement in an educational simulator

### ADR-02: PostgreSQL as the source of truth
The database holds accounts, transfers, postings, audit records, and later loan tables.

Reason:
- atomicity and transactional integrity
- consistent persistence across restarts
- compatibility with Flyway-managed schema evolution

### ADR-03: Explicit financial ledger model
Use an immutable posting structure or equivalent explicit posting table with references to business transactions.

Reason:
- easier to validate debit/credit balance
- supports tracing and auditability
- keeps the system simple and inspectable

### ADR-04: Row-level locking for concurrent balance updates
Prefer deterministic locking on affected account rows during a transfer, then test concurrency through PostgreSQL-backed integration tests.

Reason:
- prevents check-then-update races
- keeps behavior explicit for the educational project

### ADR-05: YAML-based rule configuration for milestone 1
Use a small, explicit configuration format for business rules.

Reason:
- easy to understand and test
- suitable for small-scope simulator
- avoids unnecessary persistence complexity before core flows are stable

### ADR-06: Simulated patterns only
The project will document behavior as simulator conventions and not claim native T24 compatibility.

Reason:
- compliance with project requirement
- honesty about scope and limitations
- maintainability of technical documentation

---

## 10. Quality Requirements

### 10.1 Functional quality
- correctness of transfer and loan logic
- deterministic rule evaluation
- stable API responses and error codes
- idempotent operations
- no partial posting on failures

### 10.2 Reliability
- database transactions for financial updates
- explicit handling of technical failure vs business rejection
- concurrency safe transfer processing
- restart-safe persistent data model

### 10.3 Testability
- unit tests for rules and calculations
- repository/integration tests for database behavior
- API-level or end-to-end tests for FT and LD workflows
- stable test naming and scenario-based coverage

### 10.4 Maintainability
- clear package boundaries
- simple dependency direction
- explicit domain model instead of scattered business logic
- migration-based schema evolution

### 10.5 Security and safety
- synthetic demo data only
- no secret material checked into repository
- no excessive data in logs
- no public admin endpoints

---

## 11. Risks and Technical Debt

- The project may overreach if scope grows beyond FT and LD fundamentals
- Without careful transaction boundaries, concurrency bugs can emerge even in simple balance logic
- Rule configuration may become too flexible if expanded prematurely
- Loan calculation conventions must be explicitly documented to avoid ambiguity
- A future move to event-driven architecture must be handled with outbox or equivalent safe publication patterns

These risks are mitigated by keeping the first release focused, versioned, and test-driven.

---

## 12. Glossary

- Account: simulated customer account with currency, balance, and status
- Transfer: movement of money from one account to another
- Posting: an immutable ledger-style record that represents a debit or credit
- Routine: a named, simulated business operation resolved by a dispatcher
- Rule: a validation or policy used to approve or reject a financial action
- Idempotency key: unique request identifier used to prevent duplicate financial effects
- Loan schedule: deterministic set of installments, interest and principal allocations, and due dates
- Audit trail: system record of transaction lifecycle events and actions
- Simulator: educational business logic implementation, not a native T24 runtime

---

## 13. Summary

This architecture is intentionally simple, explicit, and review-friendly. It prioritizes correctness and financial integrity over broad imitation of a proprietary banking platform. The project aims to provide a clean, local Java/Spring/PostgreSQL implementation of key core-banking concepts with strong tests, deterministic behavior, and clear documentation.
