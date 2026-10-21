package com.pswied.core.nostra.rules;

import com.pswied.core.nostra.account.Account;
import java.math.BigDecimal;
import java.util.Locale;
import org.springframework.stereotype.Component;

@Component
public class TransferRules {

    public void validate(Account source, Account destination, BigDecimal amount, String currency) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessRuleException("RULE_AMOUNT_POSITIVE", "Transfer amount must be positive");
        }

        if (source == null || destination == null) {
            throw new BusinessRuleException("RULE_ACCOUNT_REQUIRED", "Source and destination accounts are required");
        }

        if (!source.getStatus().name().equals("ACTIVE") || !destination.getStatus().name().equals("ACTIVE")) {
            throw new BusinessRuleException("RULE_ACCOUNT_STATUS", "Only active accounts can transfer");
        }

        if (source.getAccountNumber().equals(destination.getAccountNumber())) {
            throw new BusinessRuleException("RULE_SAME_ACCOUNT", "Source and destination account cannot be the same");
        }

        if (currency == null || currency.isBlank()) {
            throw new BusinessRuleException("RULE_CURRENCY_REQUIRED", "Transfer currency is required");
        }

        String normalizedCurrency = currency.trim().toUpperCase(Locale.ROOT);
        if (!source.getCurrency().equals(normalizedCurrency) || !destination.getCurrency().equals(normalizedCurrency)) {
            throw new BusinessRuleException("RULE_CURRENCY_MATCH", "Transfer currency must match both account currencies");
        }

        if (source.getBalance().compareTo(amount) < 0) {
            throw new BusinessRuleException("RULE_INSUFFICIENT_FUNDS", "Insufficient funds");
        }
    }
}
