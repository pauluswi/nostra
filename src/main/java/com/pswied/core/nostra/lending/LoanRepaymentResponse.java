package com.pswied.core.nostra.lending;

import java.math.BigDecimal;

public record LoanRepaymentResponse(
    String loanReference,
    BigDecimal amount,
    BigDecimal principalComponent,
    BigDecimal interestComponent,
    BigDecimal outstandingBalance,
    LoanStatus status
) {
}
