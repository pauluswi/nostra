package com.pswied.core.nostra.account;

import java.time.Instant;

public record TransferAuditDto(String transferReference, String actionName, String outcome, String message, String correlationId, Instant createdAt) {
}
