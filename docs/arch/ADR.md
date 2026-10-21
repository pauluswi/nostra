# Architecture Decision Records

This document captures the key architectural decisions for Nostra. These decisions reflect the project goal: a small, testable, and auditable banking simulator that can run locally without needing Docker or a proprietary Temenos runtime.

## ADR-01: Use a modular monolith instead of microservices

### Status
Accepted

### Context
The project is intended to demonstrate core-banking concepts in a small, understandable codebase. It must support account management, funds transfers, business rules, routines, and later lending flows without introducing distributed-system complexity.

A microservices design would add infrastructure complexity, inter-service transactions, and slower local development. The team wants to keep the implementation easy to run, test, and explain.

### Decision
Use a single Spring Boot application with clearly named domain packages, rather than splitting the system into multiple services.

### Consequences
Positive:
- simpler local startup and developer experience
- easier transaction tracing within one database
- clear package structure and fewer moving parts
- faster feedback for tests and demos

Negative:
- some domain boundaries are conceptual rather than fully isolated by service
- the system would need decomposition later if it grew beyond the educational scope

---

## ADR-02: Use Spring Boot with a simple local data store for the fast path

### Status
Accepted

### Context
The project requirements emphasize reproducible local execution, deterministic behavior, and quick feedback. The original architecture target identified PostgreSQL and Docker Compose, but the local implementation needs a faster path that avoids container startup overhead.

The project must still be easy to understand and test in a realistic Java/Spring stack.

### Decision
Use Spring Boot 3.x with an in-memory H2 database for the default local development profile. PostgreSQL remains the conceptual target for the full architecture, but H2 is used for the fast local path and repeatable test execution.

### Consequences
Positive:
- rapid startup without Docker
- low friction for local validation
- good fit for unit and integration tests
- easy demonstration of the core flows in a small environment

Negative:
- not production-grade persistence
- not equivalent to PostgreSQL semantics in all cases
- needs explicit documentation that this is a fast local mock profile

---

## ADR-03: Keep the domain model explicit and not T24-imitative

### Status
Accepted

### Context
The project is inspired by core-banking concepts but is not a faithful recreation of Temenos T24 or TAFJ. This distinction is critical to avoid unsupported compatibility claims and to maintain clarity for reviewers and learners.

### Decision
Model the domain with explicit concepts such as Account, Transfer, Rule, Routine, and Loan schedule, and document them as simulator-specific conventions rather than native T24 behaviors.

### Consequences
Positive:
- avoids misleading compatibility claims
- makes implementation choices traceable and understandable
- keeps the project educational and review-friendly

Negative:
- some terminology may be familiar to banking practitioners but will not match native T24 internals exactly
- the simulator is intentionally simplified and not feature-complete

---

## ADR-04: Validate business rules before posting financial changes

### Status
Accepted

### Context
A core principle of the project is avoiding partial or invalid financial effects. Transfer and loan workflows must reject invalid states before any balance changes occur.

### Decision
Perform validation of amount, account status, currency compatibility, and balance sufficiency before applying debit and credit operations. Use stable error codes or clear business-rule exceptions to represent rejection reasons.

### Consequences
Positive:
- stronger financial correctness
- easier debugging of failed operations
- clean separation between validation and persistence logic

Negative:
- requires careful handling in service logic and tests
- domain logic must stay explicit and not silently skip validation

---

## ADR-05: Use idempotency keys for transfer safety

### Status
Accepted

### Context
Repeated requests can otherwise cause duplicate financial effects. In banking workflows, retries and duplicate submissions must not double-post money.

### Decision
Every transfer request includes an idempotency key, and the system treats identical requests consistently without applying the same effect twice. Reusing the same key with different payloads is rejected.

### Consequences
Positive:
- safe retries and duplicate handling
- simpler support for client retries
- improved correctness for financial workflows

Negative:
- requires persistent tracking or a comparable mechanism for duplicate detection
- adds some complexity to the API contract

---

## ADR-06: Use exact decimal arithmetic for money

### Status
Accepted

### Context
Financial calculations must not rely on binary floating-point values because that causes precision errors in monetary operations.

### Decision
Use `BigDecimal` for account balances and financial amounts in the domain model and service calculations.

### Consequences
Positive:
- deterministic and precise money handling
- easier testing of balances and schedule totals
- aligns with core-banking expectations

Negative:
- more verbose code than using double/float
- requires explicit rounding decisions where needed

---

## ADR-07: Favor layered service design over direct controller persistence

### Status
Accepted

### Context
The application should remain maintainable and not expose database entities directly. The web layer should stay thin and the domain use cases should be orchestrated in a dedicated service layer.

### Decision
Controllers handle HTTP concerns, services coordinate use cases and validation, and repositories are responsible for persistence.

### Consequences
Positive:
- clearer responsibilities and better separation of concerns
- easier unit testing
- safer DTO-based APIs

Negative:
- several small classes and interfaces are required
- more ceremony than a single-class prototype

---

## ADR-08: Keep the architecture review-friendly and intentionally limited

### Status
Accepted

### Context
The project is educational and demonstration-oriented. Broad feature scope and heavy architectural complexity would distract from the core goal: showing a correct, accessible banking simulator.

### Decision
Limit the initial implementation to core flows and keep the design explicit and narrow. Add more features only once the base invariants are proven and understood.

### Consequences
Positive:
- faster implementation and learning curve
- easier review and onboarding
- better quality for the essential scenarios

Negative:
- not a broad banking platform
- some advanced banking features are intentionally deferred

---

## ADR-09: Default to no-Docker local execution for faster iteration

### Status
Accepted

### Context
The project is a simulator and fast development environment. Developer speed matters. Docker adds complexity and startup time without being essential for a local mock flow.

### Decision
The default local run path uses a direct Spring Boot startup with H2, without Docker. This is treated as a fast local profile rather than a production deployment model.

### Consequences
Positive:
- faster feedback loop for development and demos
- easier onboarding for contributors
- simpler tests and local validation

Negative:
- does not reflect full PostgreSQL/Docker production architecture
- should not be confused with production-grade deployment choices

---

## ADR-10: Keep the system explicit about what is simulated

### Status
Accepted

### Context
The project aims to demonstrate financial rules and software design patterns without claiming to replicate a licensed banking product or runtime.

### Decision
The documentation and code clearly label the implementation as an educational simulator and separate it from actual T24 runtime semantics.

### Consequences
Positive:
- honest scope and design boundaries
- easier legal and documentation clarity
- better trust with reviewers and maintainers

Negative:
- some readers may expect a broader modeling of real bank internals
- the project remains intentionally narrower than a full banking platform

---

## Summary

These decisions keep the system fast, understandable, and aligned with the project’s purpose: demonstrating core-banking concepts and safe financial behavior without relying on heavy infrastructure or unsupported Temenos compatibility claims.
