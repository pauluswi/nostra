package com.pswied.core.nostra.cob;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "cob_runs")
public class CobRun {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "run_reference", nullable = false, unique = true)
    private String runReference;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CobStatus status;

    @Column(name = "account_count", nullable = false)
    private long accountCount;

    @Column(name = "completed_transfer_count", nullable = false)
    private long completedTransferCount;

    @Column(name = "rejected_transfer_count", nullable = false)
    private long rejectedTransferCount;

    @Column(name = "active_loan_count", nullable = false)
    private long activeLoanCount;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "finished_at", nullable = false)
    private Instant finishedAt;

    @Column(name = "summary_message", nullable = false)
    private String summaryMessage;

    protected CobRun() {
    }

    public CobRun(String runReference, CobStatus status, long accountCount,
                  long completedTransferCount, long rejectedTransferCount,
                  long activeLoanCount, String summaryMessage) {
        this.runReference = runReference;
        this.status = status;
        this.accountCount = accountCount;
        this.completedTransferCount = completedTransferCount;
        this.rejectedTransferCount = rejectedTransferCount;
        this.activeLoanCount = activeLoanCount;
        this.startedAt = Instant.now();
        this.finishedAt = Instant.now();
        this.summaryMessage = summaryMessage;
    }

    public UUID getId() {
        return id;
    }

    public String getRunReference() {
        return runReference;
    }

    public CobStatus getStatus() {
        return status;
    }

    public long getAccountCount() {
        return accountCount;
    }

    public long getCompletedTransferCount() {
        return completedTransferCount;
    }

    public long getRejectedTransferCount() {
        return rejectedTransferCount;
    }

    public long getActiveLoanCount() {
        return activeLoanCount;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public Instant getFinishedAt() {
        return finishedAt;
    }

    public String getSummaryMessage() {
        return summaryMessage;
    }
}
