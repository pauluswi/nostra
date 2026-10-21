# Demo script

This is a short walkthrough for validating the core showcase locally.

## 1. Start the app

```bash
./mvnw spring-boot:run
```

## 2. Verify health

```bash
curl http://localhost:8080/actuator/health
```

## 3. View accounts

```bash
curl http://localhost:8080/api/v1/accounts
```

## 4. Successful transfer

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

## 5. Duplicate request protection

Repeat the same request with the same idempotency key and confirm the original response is returned without double-posting.

## 6. Rejected transfer

```bash
curl -X POST http://localhost:8080/api/v1/transfers \
  -H 'Content-Type: application/json' \
  -d '{
    "sourceAccountNumber": "ACC-1001",
    "destinationAccountNumber": "ACC-1002",
    "amount": "999999.00",
    "currency": "USD",
    "idempotencyKey": "demo-transfer-reject-001"
  }'
```

## 7. Create a loan

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

## 8. Inspect the schedule

```bash
curl http://localhost:8080/api/v1/loans/{loanReference}/schedule
```

## 9. Inspect transfer audit trail

```bash
curl http://localhost:8080/api/v1/transfers/{reference}/audit
```

## 10. Open Swagger UI

Visit:

```text
http://localhost:8080/swagger-ui/index.html
```
