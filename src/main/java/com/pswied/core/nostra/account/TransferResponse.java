package com.pswied.core.nostra.account;

import java.math.BigDecimal;

public record TransferResponse(
    String reference,
    String status,
    String sourceAccountNumber,
    String destinationAccountNumber,
    BigDecimal amount,
    String currency,
    String message
) {
}
