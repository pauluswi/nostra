package com.pswied.core.nostra.account;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class AccountServiceTest {

    @Autowired
    private AccountService accountService;

    @Autowired
    private AccountRepository accountRepository;

    @BeforeEach
    void setUp() {
        accountRepository.deleteAll();
        accountRepository.save(new Account("ACC-T1", "USD", AccountStatus.ACTIVE, new BigDecimal("1000.00")));
        accountRepository.save(new Account("ACC-T2", "USD", AccountStatus.ACTIVE, new BigDecimal("500.00")));
    }

    @Test
    void transfer_shouldMoveFundsBetweenActiveAccounts() {
        TransferResponse response = accountService.transfer(new TransferRequest(
            "ACC-T1",
            "ACC-T2",
            new BigDecimal("75.50"),
            "USD",
            "key-1"
        ));

        assertThat(response.reference()).startsWith("TRF-");
        assertThat(response.status()).isEqualTo("COMPLETED");
        assertThat(accountRepository.findByAccountNumber("ACC-T1").orElseThrow().getBalance())
            .isEqualByComparingTo("924.50");
        assertThat(accountRepository.findByAccountNumber("ACC-T2").orElseThrow().getBalance())
            .isEqualByComparingTo("575.50");
    }

    @Test
    void transfer_shouldRejectDuplicateIdempotencyKeyWithDifferentPayload() {
        accountService.transfer(new TransferRequest("ACC-T1", "ACC-T2", new BigDecimal("50.00"), "USD", "key-dup"));

        assertThatThrownBy(() -> accountService.transfer(new TransferRequest(
            "ACC-T1",
            "ACC-T2",
            new BigDecimal("60.00"),
            "USD",
            "key-dup"
        )))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("different payload");
    }

    @Test
    void transfer_shouldRejectWhenCurrencyDoesNotMatch() {
        assertThatThrownBy(() -> accountService.transfer(new TransferRequest(
            "ACC-T1",
            "ACC-T2",
            new BigDecimal("10.00"),
            "EUR",
            "key-2"
        )))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("currency");
    }
}
