package com.pswied.core.nostra.lending;

import java.math.BigDecimal;
import java.time.LocalDate;

public record LoanScheduleEntryDto(
    int installmentNumber,
    LocalDate dueDate,
    BigDecimal openingBalance,
    BigDecimal paymentAmount,
    BigDecimal principalComponent,
    BigDecimal interestComponent,
    BigDecimal closingBalance
) {
}
