package com.pswied.core.nostra.lending;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "loans")
public class Loan {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "loan_reference", nullable = false, unique = true)
    private String loanReference;

    @Column(name = "borrower_account_number", nullable = false)
    private String borrowerAccountNumber;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal principal;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(name = "annual_interest_rate", nullable = false, precision = 19, scale = 6)
    private BigDecimal annualInterestRate;

    @Column(name = "term_months", nullable = false)
    private int termMonths;

    @Column(name = "monthly_payment", nullable = false, precision = 19, scale = 4)
    private BigDecimal monthlyPayment;

    @Column(name = "outstanding_balance", nullable = false, precision = 19, scale = 4)
    private BigDecimal outstandingBalance;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LoanStatus status;

    @Column(nullable = false)
    private Instant createdAt;

    protected Loan() {
    }

    public Loan(String loanReference, String borrowerAccountNumber, BigDecimal principal,
               String currency, BigDecimal annualInterestRate, int termMonths,
               BigDecimal monthlyPayment, BigDecimal outstandingBalance, LoanStatus status) {
        this.loanReference = loanReference;
        this.borrowerAccountNumber = borrowerAccountNumber;
        this.principal = principal;
        this.currency = currency;
        this.annualInterestRate = annualInterestRate;
        this.termMonths = termMonths;
        this.monthlyPayment = monthlyPayment;
        this.outstandingBalance = outstandingBalance;
        this.status = status;
        this.createdAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public String getLoanReference() {
        return loanReference;
    }

    public String getBorrowerAccountNumber() {
        return borrowerAccountNumber;
    }

    public BigDecimal getPrincipal() {
        return principal;
    }

    public String getCurrency() {
        return currency;
    }

    public BigDecimal getAnnualInterestRate() {
        return annualInterestRate;
    }

    public int getTermMonths() {
        return termMonths;
    }

    public BigDecimal getMonthlyPayment() {
        return monthlyPayment;
    }

    public BigDecimal getOutstandingBalance() {
        return outstandingBalance;
    }

    public LoanStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setOutstandingBalance(BigDecimal outstandingBalance) {
        this.outstandingBalance = outstandingBalance;
    }

    public void setStatus(LoanStatus status) {
        this.status = status;
    }
}
