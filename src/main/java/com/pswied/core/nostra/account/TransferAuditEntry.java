package com.pswied.core.nostra.account;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "transfer_audit_entries")
public class TransferAuditEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "transfer_reference", nullable = false)
    private String transferReference;

    @Column(name = "action_name", nullable = false)
    private String actionName;

    @Column(nullable = false)
    private String outcome;

    @Column(nullable = false)
    private String message;

    @Column(name = "correlation_id", nullable = false)
    private String correlationId;

    @Column(nullable = false)
    private Instant createdAt;

    protected TransferAuditEntry() {
    }

    public TransferAuditEntry(String transferReference, String actionName, String outcome,
                             String message, String correlationId) {
        this.transferReference = transferReference;
        this.actionName = actionName;
        this.outcome = outcome;
        this.message = message;
        this.correlationId = correlationId;
        this.createdAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public String getTransferReference() {
        return transferReference;
    }

    public String getActionName() {
        return actionName;
    }

    public String getOutcome() {
        return outcome;
    }

    public String getMessage() {
        return message;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
