**Document status:** Initial baseline
**Version:** 0.1
**Project type:** Educational core-banking development simulator and portfolio showcase
**Primary objective:** Demonstrate traditional Temenos Transact/T24-inspired Funds Transfer (FT) and Lending (LD) development concepts without claiming to reproduce or replace the proprietary Temenos runtime.

---

## 1. Purpose

Build a runnable local application that simulates selected core-banking development workflows: configurable business rules, routine-style processing, transaction lifecycle, account records, funds transfers, and fixed-term lending.

The project is intended to demonstrate software architecture, Java engineering, transaction integrity, testing, observability, and banking-domain understanding. It is **not** a Temenos product, a TAFJ/TAFC implementation, or a source-compatible native T24 environment.

### 1.1 Goals

- Provide a reproducible local development environment using Java 25, Spring Boot, Maven, PostgreSQL, and Docker Compose.
- Model selected traditional FT and LD business flows.
- Separate application/domain logic from persistence, API transport, and simulated runtime behavior.
- Demonstrate validation rules, routine dispatch, transaction states, auditability, and safe retry behavior.
- Include automated tests and clear documentation so reviewers can run and understand the showcase.
- Keep the initial scope small enough to deliver incrementally.

### 1.2 Non-goals

- Reproduce proprietary Temenos source code, libraries, internal data structures, or runtime behavior.
- Claim compatibility with TAFJ, TAFC, jBASE, or any licensed Transact release.
- Implement every T24 application, field, version, local table, enquiry, COB process, or accounting event.
- Provide a production-ready banking ledger, regulatory-certified calculations, or real payment connectivity.
- Connect to a real Temenos environment unless a separately authorized integration is added later.
- Build a full visual IDE or a faithful copy of the T24 user interface.

---

## 2. Product principles

1. **Explicit simulation:** Every T24-inspired behavior must be documented as a simulator convention unless verified against authorized official documentation.
2. **Correctness before breadth:** Prefer a small set of tested banking flows over broad but shallow module coverage.
3. **Atomic financial updates:** A transfer or repayment must not leave partial debit/credit or posting results.
4. **Safe retries:** Requests that are retried must not apply the same financial effect twice.
5. **Auditability:** Important state transitions and financial postings must be traceable.
6. **Deterministic tests:** Core domain tests should run without external network services.
7. **Security by default:** No real customer data, credentials, or production connection details in the repository.

---

## 3. Primary use cases

- Create and inspect simulated customer accounts.
- Execute a funds transfer between eligible accounts.
- Reject a transfer when a rule fails, such as inactive account, currency mismatch, invalid amount, or insufficient funds.
- Submit a transfer again with the same idempotency key and avoid duplicate financial effects.
- Inspect transaction status, debit/credit postings, and audit history.
- Create a fixed-term loan with a defined principal, annual interest rate, term, and repayment frequency.
- Generate and inspect a repayment schedule.
- Post a simulated loan repayment with principal and interest allocation.
- Execute a named routine through the simulated routine dispatcher.
- Change a business rule through configuration and verify its effect with tests.

---

## 4. Technology baseline

| Area | Requirement |
|---|---|
| Language | Java 25 |
| Framework | Spring Boot, using a release officially compatible with Java 25 |
| Build | Maven Wrapper and Maven |
| Database | PostgreSQL |
| Local orchestration | Docker Compose |
| Persistence | Spring Data JPA or Spring JDBC; choose one primary approach and document it |
| Schema management | Flyway or an equivalent versioned migration tool |
| API | REST/JSON with OpenAPI documentation |
| Testing | JUnit 5, Spring Boot Test, and Testcontainers where practical |
| Observability | Structured application logs, health endpoint, and useful transaction identifiers |
| Source control | Git |
| Optional later addition | Kafka for domain events; not required for the initial milestone |

Pin compatible dependency versions and container image tags. Avoid floating `latest` tags for reproducible runs. Do not introduce Kafka or other infrastructure before the core transaction flows are working.

---

## 5. Functional requirements

Priority definitions: **P0** = required for the first usable release; **P1** = required for the FT/LD showcase; **P2** = optional extension.

### FR-001 — Local environment and application startup (P0)

- The repository MUST include a Maven Wrapper and Docker Compose configuration.
- A developer MUST be able to start PostgreSQL and the application using documented commands.
- Application configuration MUST be externalized through environment variables or Spring configuration.
- The application MUST expose a health endpoint and fail clearly when required configuration is invalid.
- Database schema changes MUST be version-controlled and repeatable.
- The README MUST document prerequisites, startup, shutdown, test execution, and troubleshooting.

**Acceptance criteria**
- A fresh checkout can be built using the documented workflow.
- PostgreSQL becomes healthy before the application attempts normal operation.
- Schema migrations run successfully against a clean database.
- Secrets are not committed to source control.

### FR-002 — Simulated routine contract and dispatcher (P0)

- The simulator MUST define a documented routine interface with a routine name, execution context, input, and structured result.
- A dispatcher MUST resolve a requested routine by registered name.
- The result MUST distinguish success from a business rejection and an unexpected technical failure.
- Unknown routine names MUST return a clear error without changing financial state.
- Routine implementations MUST not depend directly on HTTP request objects.
- At least one routine MUST be covered by unit tests.

**Acceptance criteria**
- A registered routine can be executed through an application service or developer command.
- An unknown routine is rejected deterministically.
- Business rejections are distinguishable from technical exceptions.
- Routine behavior is explicitly labelled as simulator behavior, not native T24 execution.

### FR-003 — Configurable business rules (P0)

- The application MUST support a documented way to configure a small set of business rules.
- Initial rules MUST cover positive transfer amount, account eligibility, matching account/transfer currency, and available balance.
- Rule failures MUST return stable machine-readable error codes and readable messages.
- Rules MUST be tested independently of HTTP controllers.
- Invalid rule configuration MUST fail fast or be reported clearly; it MUST NOT silently disable validation.

**Acceptance criteria**
- A rule can be changed through the documented configuration mechanism and its behavior verified by a test.
- Each rejection identifies the relevant rule or error code.
- No rejected transfer changes account balances or creates successful financial postings.

### FR-004 — Account records (P0)

- The system MUST represent a simulated account with a unique account identifier, currency, status, and balance.
- Supported initial account statuses MUST include at least `ACTIVE`, `BLOCKED`, and `CLOSED`.
- Monetary values MUST use `BigDecimal` or an equivalent exact decimal representation, never binary floating-point types.
- Currency MUST be explicit and validated; the initial release may use a deliberately small supported-currency set.
- Account balances MUST be updated only through validated application use cases, not arbitrary public update endpoints.
- The project MUST use synthetic seed data only.

**Acceptance criteria**
- An account can be created through a controlled seed/development workflow and retrieved through the API.
- Duplicate account identifiers are rejected.
- Blocked and closed accounts cannot participate in new transfers.
- Monetary precision and currency validation have automated tests.

### FR-005 — Funds Transfer (FT) (P0)

- The system MUST support a transfer from one account to another.
- A transfer request MUST include source account, destination account, amount, currency, and an idempotency key or equivalent request identifier.
- The system MUST validate positive amount, account status, currency compatibility, and available source balance.
- A transfer MUST debit the source and credit the destination atomically.
- A transfer MUST NOT permit the source and destination to be the same account in the initial release.
- Each successful transfer MUST have a unique simulator transaction reference and a traceable result.
- The system MUST persist the transaction and its financial postings in the same database transaction.
- A failed transfer MUST NOT leave a partial debit, partial credit, or successful posting.
- Concurrent transfers MUST not allow an account to be overdrawn because of a check-then-update race.
- The API MUST distinguish business rejection from technical failure.

**Acceptance criteria**
- A successful transfer reduces the source balance and increases the destination balance by the same amount.
- A rejected transfer leaves both balances unchanged.
- Repeating a request with the same idempotency key and same payload returns the original result or a documented equivalent without applying the transfer twice.
- Reusing an idempotency key with a different payload is rejected.
- Automated tests cover insufficient funds, blocked account, currency mismatch, duplicate request, and concurrent debit attempts.
- Transfer postings balance: total debit equals total credit for each completed transfer.

### FR-006 — Transaction lifecycle and audit trail (P1)

- The simulator MUST define its own explicit transaction states and permitted transitions.
- The initial lifecycle SHOULD include `RECEIVED`, `VALIDATED`, `COMPLETED`, and `REJECTED`; additional states may be added when their semantics are documented.
- State transitions MUST be validated by application logic.
- Important actions MUST record timestamp, transaction reference, action, outcome, and relevant correlation identifier.
- Sensitive values MUST not be written to logs unnecessarily.
- The project MUST document how technical failure differs from a business rejection.

**Acceptance criteria**
- A transaction can be inspected with its current state and audit history.
- Invalid state transitions are rejected.
- Rejected and completed transactions are distinguishable.
- Audit entries are created consistently for documented lifecycle events.

**Scope note:** This is a simplified lifecycle for the simulator. It is not intended to reproduce the complete native T24 input, validation, authorisation, and commit lifecycle.

### FR-007 — Fixed-term Lending (LD) (P1)

- The system MUST support creating a simulated fixed-term installment loan.
- A loan MUST include a unique loan reference, borrower/account reference, principal, currency, annual interest rate, term, repayment frequency, start date, and status.
- The initial release MUST define and document one interest and repayment calculation convention.
- The system MUST generate a repayment schedule with due dates, opening balance, payment amount, principal component, interest component, and closing balance.
- Schedule calculations MUST use decimal arithmetic and a documented rounding policy.
- The system MUST validate positive principal, valid term, valid rate, eligible borrower/account, and supported currency.
- The system MUST support a simulated repayment against a due installment.
- Repayment posting MUST atomically update the loan balance/status and create corresponding financial records.
- The system MUST prevent the same repayment request from being applied twice.

**Acceptance criteria**
- A valid loan produces a deterministic schedule for the same inputs.
- Schedule totals reconcile to the documented rounding policy.
- A repayment allocates amounts to principal and interest according to the selected convention.
- An invalid loan or repayment request is rejected without partial financial updates.
- Repeated repayment requests with the same idempotency key do not double-post.
- Tests cover rounding, final installment adjustment, invalid terms, repayment amount, and duplicate repayment.

**Scope note:** This is a simplified LD-inspired model, not a full implementation of Temenos LD, AA, accrual, COB, rescheduling, prepayment, penalty, tax, or product-condition functionality.

### FR-008 — REST API and API documentation (P0)

- Core use cases MUST be exposed through versioned REST endpoints.
- Requests and responses MUST use documented DTOs rather than exposing persistence entities directly.
- API errors MUST have a consistent response structure with a stable error code and useful message.
- Validation failures MUST return appropriate HTTP status codes.
- The API MUST expose account lookup, transfer submission and inquiry, and routine execution for the initial milestone.
- Lending endpoints MUST be added for the LD milestone.
- OpenAPI documentation MUST describe endpoints, schemas, and representative error responses.

**Acceptance criteria**
- API endpoints can be exercised with documented `curl` examples or an included HTTP collection.
- Malformed requests return consistent errors.
- Internal stack traces and secrets are not returned to clients.

### FR-009 — Persistence and data integrity (P0)

- PostgreSQL MUST be the authoritative store for accounts, transfers, postings, and audit records.
- Lending tables MUST be introduced with the LD milestone.
- Database constraints MUST enforce appropriate uniqueness and referential integrity.
- Financial operations MUST use database transactions and suitable locking or concurrency control.
- Idempotency keys MUST be persisted with enough information to detect conflicting payload reuse.
- Schema changes MUST be delivered as migrations.
- Monetary columns MUST use appropriate fixed-precision numeric types.

**Acceptance criteria**
- Restarting the application does not lose committed transactions.
- Duplicate identifiers are rejected by both application validation and appropriate database constraints.
- Failure injection tests or integration tests demonstrate rollback of a failed financial operation.
- Concurrency strategy is documented and tested against PostgreSQL, not only an in-memory database.

### FR-010 — Testing and quality gates (P0)

- Domain rules and calculations MUST have unit tests.
- Repository and transaction behavior MUST have integration tests.
- FT and LD workflows MUST have API-level or end-to-end tests.
- Tests MUST cover normal, rejection, duplicate request, and relevant concurrency paths.
- The build MUST run tests through Maven.
- The project MUST document the test command and any container requirements.

**Acceptance criteria**
- A clean build and test run succeeds using documented commands.
- Critical financial invariants are asserted in tests.
- Test names explain the scenario and expected outcome.
- Tests do not require access to a real Temenos installation.

### FR-011 — Observability and operational diagnostics (P1)

- The application MUST emit structured logs for transaction processing.
- Logs SHOULD include a correlation ID and simulator transaction reference where available.
- Health/readiness information MUST be available for local operation.
- Unexpected errors MUST be diagnosable without exposing credentials or sensitive data.
- Metrics MAY include transfer success/rejection counts, processing duration, and routine outcomes.

### FR-012 — Developer experience and demonstration (P1)

- The repository MUST include an architecture overview and a component diagram.
- The project MUST provide sample accounts and documented demonstration scenarios.
- A reviewer MUST be able to demonstrate one successful transfer, one rejected transfer, duplicate-request protection, and one loan schedule.
- The project MUST identify which behaviors are simulated and which are standard Spring Boot/PostgreSQL implementations.
- A short demo script SHOULD guide a five-to-ten-minute walkthrough.

---

## 6. Domain and accounting invariants

The following invariants are mandatory:

1. A completed transfer has exactly one logical debit and one logical credit for the same amount and currency, subject to the simulator's explicitly documented posting model.
2. Total debit equals total credit for each completed transfer.
3. No rejected transfer changes account balances.
4. A transfer cannot spend more than the available balance under the selected balance model.
5. One idempotency key cannot produce more than one financial effect.
6. All committed postings refer to a valid business transaction.
7. Loan principal and repayment allocation reconcile under the documented rounding convention.
8. A failed database transaction leaves no partial financial effect.
9. All amount and currency validation occurs before posting.
10. Database constraints and transaction controls complement, rather than replace, domain validation.

The initial release may model a simplified balance and posting ledger. The documentation MUST state that this is an educational model and not a production general ledger.

---

## 7. Suggested API surface

The exact endpoint names may be refined during design, but the initial API SHOULD cover:

| Method | Example path | Purpose |
|---|---|---|
| `GET` | `/api/v1/accounts/{accountId}` | Retrieve an account |
| `POST` | `/api/v1/transfers` | Submit a transfer |
| `GET` | `/api/v1/transfers/{reference}` | Inspect transfer outcome |
| `POST` | `/api/v1/routines/{routineName}/execute` | Execute a registered simulated routine |
| `POST` | `/api/v1/loans` | Create a fixed-term loan |
| `GET` | `/api/v1/loans/{loanId}` | Inspect loan details |
| `GET` | `/api/v1/loans/{loanId}/schedule` | Retrieve repayment schedule |
| `POST` | `/api/v1/loans/{loanId}/repayments` | Submit a simulated repayment |
| `GET` | `/actuator/health` | Application health |

The API MUST document idempotency-key handling, expected status codes, and error response schemas.

---

## 8. Non-functional requirements

### NFR-001 — Maintainability

- Use clear package boundaries and dependency direction.
- Keep domain logic independent from HTTP and persistence frameworks where practical.
- Avoid unnecessary abstractions and premature microservices.
- Use consistent naming, formatting, and meaningful JavaDoc for non-obvious domain decisions.

### NFR-002 — Reliability and consistency

- Financial operations MUST be atomic.
- Retry behavior MUST be explicit and safe.
- Database isolation and locking choices MUST be documented.
- Technical failures MUST not be silently converted into successful business outcomes.

### NFR-003 — Security

- Use synthetic demonstration data only.
- Store credentials in local environment configuration, not committed files.
- Do not expose database administration endpoints publicly.
- Validate input and apply sensible request-size limits.
- Do not log secrets or unnecessary personal data.
- Document that local development defaults are not production security settings.

### NFR-004 — Reproducibility

- Pin tool and container versions where practical.
- Provide Maven Wrapper and versioned database migrations.
- Ensure the documented local setup works on a typical developer workstation with Docker installed.
- Provide clear instructions for resetting local demo data.

### NFR-005 — Performance

- Optimize for correctness and clarity, not production-scale throughput.
- Add a small repeatable load test only after the core invariants and concurrency behavior are verified.
- Record benchmark conditions before making performance claims.

### NFR-006 — Compatibility and licensing clarity

- Use only independently written code and authorized public documentation.
- Do not include proprietary Temenos code, confidential screenshots, customer data, or licensed runtime artifacts in the repository.
- State prominently that the project is an independent educational simulator and is not affiliated with or endorsed by Temenos.

---

## 9. Architecture constraints

- Use a modular monolith for the initial release.
- Suggested logical modules/packages:
  - `account`
  - `fundstransfer`
  - `lending`
  - `routine`
  - `rules`
  - `transaction`
  - `audit`
  - `shared`
  - `configuration`
- Keep controllers thin; application services coordinate use cases.
- Keep domain types and financial calculations explicit.
- Use a single PostgreSQL database for the initial release.
- Do not require Kafka for FT/LD correctness. If event publication is later added, use an outbox or another documented reliable-publication strategy.
- Prefer a simple, deterministic rule configuration format for the first milestone. A general-purpose scripting engine is out of scope.
- Do not emulate proprietary T24 internals or undocumented native behavior.

---

## 10. Delivery milestones

### Milestone 0 — Repository foundation

- [ ] Initialize Maven project and Maven Wrapper.
- [ ] Configure Java 25 and a compatible Spring Boot release.
- [ ] Add Docker Compose and PostgreSQL health checks.
- [ ] Add schema migrations, configuration profiles, health endpoint, and basic CI/build instructions.
- [ ] Add README, license decision, and simulator disclaimer.

**Exit criteria:** The application starts locally and connects to PostgreSQL from a clean checkout.

### Milestone 1 — Routine and rule foundation

- [ ] Define routine contract, context, result, and dispatcher.
- [ ] Implement configurable validation rules.
- [ ] Add stable error codes and API error handling.
- [ ] Add unit tests and a demonstration routine.

**Exit criteria:** A routine can execute, succeed, reject input, and be tested without a real T24 runtime.

### Milestone 2 — Account and FT

- [ ] Define account, transfer, posting, and audit models.
- [ ] Implement account lookup and synthetic seed data.
- [ ] Implement atomic FT processing and balance checks.
- [ ] Implement idempotency and conflicting-key detection.
- [ ] Add locking/concurrency controls and integration tests.
- [ ] Document a successful and a rejected transfer.

**Exit criteria:** FT invariants pass automated tests, including duplicate and concurrent requests.

### Milestone 3 — LD

- [ ] Define loan, installment, repayment, and posting models.
- [ ] Document interest, date, and rounding conventions.
- [ ] Generate deterministic schedules.
- [ ] Implement repayment processing and idempotency.
- [ ] Add calculation, rollback, and workflow tests.

**Exit criteria:** A sample loan schedule and repayment workflow are reproducible and reconcile under documented rules.

### Milestone 4 — Showcase polish

- [ ] Add OpenAPI examples and a runnable demo collection.
- [ ] Add architecture and sequence diagrams.
- [ ] Add structured logs and correlation IDs.
- [ ] Write a five-to-ten-minute demonstration guide.
- [ ] Review repository content for secrets, licensed materials, and unsupported compatibility claims.

**Exit criteria:** A reviewer can clone, run, test, and understand the project without private setup instructions.

---

## 11. Definition of Done

A requirement is complete only when:

- Its implementation is committed.
- Automated tests cover its expected behavior and important failure cases.
- API/configuration behavior is documented where relevant.
- Financial invariants remain true.
- Logs and errors are useful without exposing secrets.
- The local environment remains reproducible.
- No unsupported claim of native T24/TAFJ compatibility is made.

The initial project release is considered complete when Milestones 0–2 are delivered and tested. Milestone 3 completes the first FT + LD showcase.

---

## 12. Open design decisions

Resolve these in the architecture/design phase before implementing the affected feature:

1. **FT posting model:** Minimal debit/credit posting table versus a more explicit journal model. Recommendation: use an explicit immutable posting table with a transaction reference.
2. **Concurrency strategy:** Pessimistic row locking versus optimistic versioning. Recommendation: begin with PostgreSQL row locks for the source/destination accounts in a deterministic order, then test concurrent debits.
3. **Idempotency response:** Return the original stored result for an identical request; reject a key reused with a different normalized payload.
4. **LD calculation convention:** Select a documented fixed-rate installment method, day-count convention, schedule-date policy, and rounding mode. Recommendation: start with monthly equal installments and explicitly adjust the final installment for rounding.
5. **Rule configuration:** YAML loaded at startup versus persisted rule versions. Recommendation: YAML for the first milestone; add versioning/authorisation only if it materially improves the showcase.
6. **Authorisation simulation:** Include a simplified maker/checker flow now or defer it. Recommendation: defer until FT and LD processing is stable; do not imply it reproduces native T24 authorisation.
7. **Event publication:** No broker in the first release; consider an outbox and Kafka only after database transaction semantics are tested.
8. **License:** Choose and add an appropriate open-source license before publishing the repository. Do not choose a license that conflicts with dependencies or employer policies.

---

## 13. Reference and terminology policy

- Use official Temenos documentation or authorized training materials when describing actual T24/Transact terminology or behavior.
- Label invented names, sample rule formats, Java routine interfaces, API paths, and lifecycle states as simulator-specific.
- Distinguish the actual product concepts from the simulator's implementation choices in the README and architecture documents.
- Do not use Temenos trademarks or branding in a way that implies sponsorship, certification, or product compatibility.

---

## 14. Initial release summary

The first release delivers a local Java 25/Spring Boot application backed by PostgreSQL, with a documented routine dispatcher, configurable rules, account records, and an atomic/idempotent FT workflow. The next milestone adds a clearly scoped fixed-term LD simulation with repayment schedule and repayment posting.

**Success means a reviewer can run the project, observe correct financial invariants, inspect the code and tests, and understand exactly which behaviors are simulated—without needing a licensed Temenos environment.**
"""
