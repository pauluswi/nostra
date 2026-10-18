package com.pswied.core.nostra.account;

import java.math.BigDecimal;

public record AccountDto(String accountNumber, String currency, AccountStatus status, BigDecimal balance) {
}
