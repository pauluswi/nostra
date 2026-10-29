package com.pswied.core.nostra.cob;

import java.time.Instant;

public record CobRunResponse(
    String runReference,
    CobStatus status,
    long accountCount,
    long completedTransferCount,
    long rejectedTransferCount,
    long activeLoanCount,
    long pendingTransferCount,
    long overdueLoanCount,
    Instant startedAt,
    Instant finishedAt,
    String summaryMessage
) {
}
