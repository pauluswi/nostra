# Demo narrative for reviewers

This walkthrough is designed for a short live review of the banking simulator. The emphasis is not on a broad bank platform; it is on a clear REST contract, business-rule validation, idempotent transfer handling, auditable state transitions, and a compact lending flow.

The examples below are intentionally concrete so reviewers can compare the HTTP contract to the underlying behavior.

## 1. Start the app

From the repository root:

```bash
./mvnw spring-boot:run
```

Expected outcome:
- the local profile is active
- the app starts quickly without Docker
- the H2 in-memory database is initialized for the default local path

## 2. Confirm the API is healthy

```bash
curl -i http://localhost:8080/actuator/health
```

Expected HTTP response:

```http
HTTP/1.1 200 OK
Content-Type: application/json

{"status":"UP","components":{"db":{"status":"UP"},"ping":{"status":"UP"}}}
```

## 3. Read account state

```bash
curl -i http://localhost:8080/api/v1/accounts
```

Expected response shape:

```http
HTTP/1.1 200 OK
Content-Type: application/json

[
  {
    "accountNumber": "ACC-1001",
    "currency": "USD",
    "status": "ACTIVE",
    "balance": 1500.00
  },
  {
    "accountNumber": "ACC-1002",
    "currency": "USD",
    "status": "ACTIVE",
    "balance": 800.00
  }
]
```

This shows the contract for balance lookup and the fact that the data model is deliberately simple and readable.

## 4. Successful transfer example

Request:

```bash
curl -i -X POST http://localhost:8080/api/v1/transfers \
  -H 'Content-Type: application/json' \
  -d '{
    "sourceAccountNumber": "ACC-1001",
    "destinationAccountNumber": "ACC-1002",
    "amount": "75.00",
    "currency": "USD",
    "idempotencyKey": "demo-transfer-001"
  }'
```

Expected response:

```http
HTTP/1.1 200 OK
Content-Type: application/json

{
  "reference": "TRF-AB12CD34",
  "status": "COMPLETED",
  "sourceAccountNumber": "ACC-1001",
  "destinationAccountNumber": "ACC-1002",
  "amount": 75.00,
  "currency": "USD",
  "message": "Transfer successful"
}
```

What to review:
- the transfer is successful and complete
- the response includes a unique transfer reference
- the flow uses explicit monetary values and a transaction status

## 5. Idempotency contract

Run the same transfer again with the same idempotency key:

```bash
curl -i -X POST http://localhost:8080/api/v1/transfers \
  -H 'Content-Type: application/json' \
  -d '{
    "sourceAccountNumber": "ACC-1001",
    "destinationAccountNumber": "ACC-1002",
    "amount": "75.00",
    "currency": "USD",
    "idempotencyKey": "demo-transfer-001"
  }'
```

Expected behavior:
- the same logical result is returned
- no duplicate debit/credit posting occurs
- the system treats the request as an idempotent retry rather than a second transfer

If the same idempotency key is reused with different payload data, the API returns a 400 response with a contract like:

```http
HTTP/1.1 400 Bad Request
Content-Type: application/json

{
  "timestamp": "2026-10-21T15:35:00Z",
  "status": 400,
  "error": "BUSINESS_RULE_ERROR",
  "code": "IDEMPOTENCY_KEY_CONFLICT",
  "message": "Idempotency key already used with different payload"
}
```

## 6. Validation error contract

This request is intentionally invalid:

```bash
curl -i -X POST http://localhost:8080/api/v1/transfers \
  -H 'Content-Type: application/json' \
  -d '{
    "sourceAccountNumber": "ACC-1001",
    "destinationAccountNumber": "ACC-1002",
    "amount": "999999.00",
    "currency": "USD",
    "idempotencyKey": "demo-transfer-reject-001"
  }'
```

Expected response:

```http
HTTP/1.1 400 Bad Request
Content-Type: application/json

{
  "timestamp": "2026-10-21T15:35:01Z",
  "status": 400,
  "error": "BUSINESS_RULE_ERROR",
  "code": "RULE_INSUFFICIENT_FUNDS",
  "message": "Insufficient funds"
}
```

This demonstrates the explicit error model used by the application: a small, consistent envelope with a machine-readable code and narrative message.

## 7. Malformed or invalid request body

Example malformed payload:

```bash
curl -i -X POST http://localhost:8080/api/v1/transfers \
  -H 'Content-Type: application/json' \
  -d '{"amount":"oops"}'
```

Expected response:

```http
HTTP/1.1 400 Bad Request
Content-Type: application/json

{
  "timestamp": "2026-10-21T15:35:02Z",
  "status": 400,
  "error": "INVALID_REQUEST_BODY",
  "code": "INVALID_REQUEST_BODY",
  "message": "Request payload is malformed or unreadable"
}
```

## 8. Loan creation example

Request:

```bash
curl -i -X POST http://localhost:8080/api/v1/loans \
  -H 'Content-Type: application/json' \
  -d '{
    "borrowerAccountNumber": "ACC-1001",
    "principal": "1200.00",
    "currency": "USD",
    "annualInterestRate": "12.00",
    "termMonths": 12
  }'
```

Expected response:

```http
HTTP/1.1 201 Created
Content-Type: application/json

{
  "loanReference": "LN-AB12CD34",
  "borrowerAccountNumber": "ACC-1001",
  "principal": 1200.00,
  "currency": "USD",
  "annualInterestRate": 12.00,
  "termMonths": 12,
  "monthlyPayment": 106.56,
  "outstandingBalance": 1200.00,
  "status": "ACTIVE"
}
```

## 9. Inspect the generated schedule

```bash
curl -i http://localhost:8080/api/v1/loans/{loanReference}/schedule
```

Expected response shape:

```http
HTTP/1.1 200 OK
Content-Type: application/json

[
  {
    "installmentNumber": 1,
    "dueDate": "2026-11-21",
    "openingBalance": 1200.00,
    "paymentAmount": 106.56,
    "principalComponent": 26.56,
    "interestComponent": 80.00,
    "closingBalance": 1173.44
  }
]
```

This demonstrates that the lending workflow is deterministic and reviewable in a very small contract surface.

## 10. Inspect the transfer audit trail

```bash
curl -i http://localhost:8080/api/v1/transfers/{reference}/audit
```

Expected response shape:

```http
HTTP/1.1 200 OK
Content-Type: application/json

[
  {
    "transferReference": "TRF-AB12CD34",
    "actionName": "RECEIVED",
    "outcome": "RECEIVED",
    "message": "Transfer accepted for processing",
    "correlationId": "...",
    "createdAt": "2026-10-21T15:35:00Z"
  },
  {
    "transferReference": "TRF-AB12CD34",
    "actionName": "VALIDATED",
    "outcome": "VALIDATED",
    "message": "Transfer passed business validation",
    "correlationId": "...",
    "createdAt": "2026-10-21T15:35:00Z"
  },
  {
    "transferReference": "TRF-AB12CD34",
    "actionName": "COMPLETED",
    "outcome": "COMPLETED",
    "message": "Transfer posted successfully",
    "correlationId": "...",
    "createdAt": "2026-10-21T15:35:00Z"
  }
]
```

## 11. Swagger documentation

Open:

```text
http://localhost:8080/swagger-ui/index.html
```

This is the fastest way for a reviewer to inspect the API contract directly from the running application.

## Reviewer focus

The most important things to highlight during a review are:

- the contract is small and explicit
- errors are consistent and machine-readable
- duplicate requests are safe through idempotency
- transfer and loan flows are auditable and testable
- the simulator stays intentionally transparent and review-friendly

This keeps the conversation anchored on the actual value of the project: a clear, testable core-banking simulator rather than a generic app scaffold.
