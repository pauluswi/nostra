package com.pswied.core.nostra.account;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.math.BigDecimal;

public record TransferRequest(
    @NotBlank String sourceAccountNumber,
    @NotBlank String destinationAccountNumber,
    @NotNull @DecimalMin(value = "0.01", inclusive = true) BigDecimal amount,
    @NotBlank @Pattern(regexp = "^[A-Z]{3}$", message = "Transfer currency must be a 3-letter ISO code") String currency,
    @NotBlank String idempotencyKey
) {
}
