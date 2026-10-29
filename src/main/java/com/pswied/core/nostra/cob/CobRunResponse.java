package com.pswied.core.nostra.cob;

import java.time.Instant;
import java.util.List;

public record CobRunResponse(
    String runReference,
    CobStatus status,
    long accountCount,
    long completedTransferCount,
    long rejectedTransferCount,
    long activeLoanCount,
    long pendingTransferCount,
    long overdueLoanCount,
    List<String> processingStages,
    Instant startedAt,
    Instant finishedAt,
    String summaryMessage
) {
}
