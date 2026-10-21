package com.pswied.core.nostra.lending;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "loan_repayments", uniqueConstraints = {
    @UniqueConstraint(name = "uk_loan_repayment_idempotency", columnNames = "idempotency_key")
})
public class LoanRepayment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "loan_reference", nullable = false)
    private String loanReference;

    @Column(name = "idempotency_key", nullable = false, unique = true)
    private String idempotencyKey;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(name = "principal_component", nullable = false, precision = 19, scale = 4)
    private BigDecimal principalComponent;

    @Column(name = "interest_component", nullable = false, precision = 19, scale = 4)
    private BigDecimal interestComponent;

    @Column(nullable = false)
    private Instant createdAt;

    protected LoanRepayment() {
    }

    public LoanRepayment(String loanReference, String idempotencyKey, BigDecimal amount,
                        BigDecimal principalComponent, BigDecimal interestComponent) {
        this.loanReference = loanReference;
        this.idempotencyKey = idempotencyKey;
        this.amount = amount;
        this.principalComponent = principalComponent;
        this.interestComponent = interestComponent;
        this.createdAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public String getLoanReference() {
        return loanReference;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public BigDecimal getPrincipalComponent() {
        return principalComponent;
    }

    public BigDecimal getInterestComponent() {
        return interestComponent;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
