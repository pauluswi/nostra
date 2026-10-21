package com.pswied.core.nostra.account;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "transfer_postings")
public class TransferPosting {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "transfer_reference", nullable = false)
    private String transferReference;

    @Column(name = "account_number", nullable = false)
    private String accountNumber;

    @Column(nullable = false, length = 10)
    private String direction;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(nullable = false)
    private Instant createdAt;

    protected TransferPosting() {
    }

    public TransferPosting(String transferReference, String accountNumber, String direction,
                          BigDecimal amount, String currency) {
        this.transferReference = transferReference;
        this.accountNumber = accountNumber;
        this.direction = direction;
        this.amount = amount;
        this.currency = currency;
        this.createdAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public String getTransferReference() {
        return transferReference;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public String getDirection() {
        return direction;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getCurrency() {
        return currency;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
