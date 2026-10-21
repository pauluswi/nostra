package com.pswied.core.nostra.rules;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.pswied.core.nostra.account.Account;
import com.pswied.core.nostra.account.AccountStatus;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class TransferRulesTest {

    private final TransferRules rules = new TransferRules();

    @Test
    void shouldRejectNegativeAmount() {
        Account source = new Account("ACC-1", "USD", AccountStatus.ACTIVE, new BigDecimal("100"));
        Account destination = new Account("ACC-2", "USD", AccountStatus.ACTIVE, new BigDecimal("100"));

        assertThatThrownBy(() -> rules.validate(source, destination, new BigDecimal("-1"), "USD"))
            .isInstanceOf(BusinessRuleException.class)
            .hasMessageContaining("positive");
    }

    @Test
    void shouldRejectCurrencyMismatch() {
        Account source = new Account("ACC-1", "USD", AccountStatus.ACTIVE, new BigDecimal("100"));
        Account destination = new Account("ACC-2", "EUR", AccountStatus.ACTIVE, new BigDecimal("100"));

        assertThatThrownBy(() -> rules.validate(source, destination, new BigDecimal("10"), "USD"))
            .isInstanceOf(BusinessRuleException.class)
            .hasMessageContaining("currency");
    }
}
