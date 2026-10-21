package com.pswied.core.nostra.lending;

import java.math.BigDecimal;

public record LoanResponse(
    String loanReference,
    String borrowerAccountNumber,
    BigDecimal principal,
    String currency,
    BigDecimal annualInterestRate,
    int termMonths,
    BigDecimal monthlyPayment,
    BigDecimal outstandingBalance,
    LoanStatus status
) {
}
