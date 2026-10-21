package com.pswied.core.nostra.lending;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.math.BigDecimal;

public record LoanRequest(
    @NotBlank String borrowerAccountNumber,
    @NotNull @DecimalMin(value = "0.01", inclusive = true) BigDecimal principal,
    @NotBlank @Pattern(regexp = "^[A-Z]{3}$", message = "Loan currency must be a 3-letter ISO code") String currency,
    @NotNull @DecimalMin(value = "0.00", inclusive = true) BigDecimal annualInterestRate,
    @Min(1) int termMonths
) {
}
