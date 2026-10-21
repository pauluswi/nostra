package com.pswied.core.nostra.lending;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record LoanRequest(
    @NotBlank String borrowerAccountNumber,
    @NotNull @DecimalMin(value = "0.01", inclusive = true) BigDecimal principal,
    @NotBlank String currency,
    @NotNull @DecimalMin(value = "0.00", inclusive = true) BigDecimal annualInterestRate,
    @Min(1) int termMonths
) {
}
