package com.pswied.core.nostra.cob;

import java.time.Instant;
import java.util.List;

public record CobReportResponse(
    String runReference,
    String summaryMessage,
    List<String> processingStages,
    long pendingTransferCount,
    long rejectedTransferCount,
    long overdueLoanCount,
    List<String> pendingTransferReferences,
    List<String> overdueLoanReferences,
    Instant generatedAt
) {
}
