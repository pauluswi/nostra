package com.pswied.core.nostra.account;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AccountControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AccountRepository accountRepository;

    @BeforeEach
    void setUp() {
        accountRepository.deleteAll();
        accountRepository.save(new Account("ACC-API-1", "USD", AccountStatus.ACTIVE, new java.math.BigDecimal("1000.00")));
        accountRepository.save(new Account("ACC-API-2", "USD", AccountStatus.ACTIVE, new java.math.BigDecimal("500.00")));
    }

    @Test
    void listAccounts_shouldReturnSeededAccounts() throws Exception {
        mockMvc.perform(get("/api/v1/accounts"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].accountNumber").value("ACC-API-1"));
    }

    @Test
    void transfer_shouldReturnBusinessRuleErrorForInvalidIdempotencyConflict() throws Exception {
        String requestBody = "{"
            + "\"sourceAccountNumber\":\"ACC-API-1\","
            + "\"destinationAccountNumber\":\"ACC-API-2\","
            + "\"amount\":\"50.00\","
            + "\"currency\":\"USD\","
            + "\"idempotencyKey\":\"api-key-dup\""
            + "}";

        mockMvc.perform(post("/api/v1/transfers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isOk());

        String secondRequest = "{"
            + "\"sourceAccountNumber\":\"ACC-API-1\","
            + "\"destinationAccountNumber\":\"ACC-API-2\","
            + "\"amount\":\"60.00\","
            + "\"currency\":\"USD\","
            + "\"idempotencyKey\":\"api-key-dup\""
            + "}";

        mockMvc.perform(post("/api/v1/transfers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(secondRequest))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("IDEMPOTENCY_KEY_CONFLICT"));
    }

    @Test
    void transfer_shouldRejectMalformedPayload() throws Exception {
        mockMvc.perform(post("/api/v1/transfers")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"amount\":\"oops\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("INVALID_REQUEST_BODY"));
    }

    @Test
    void transfer_shouldRejectInvalidCurrencyFormat() throws Exception {
        String requestBody = "{"
            + "\"sourceAccountNumber\":\"ACC-API-1\","
            + "\"destinationAccountNumber\":\"ACC-API-2\","
            + "\"amount\":\"25.00\","
            + "\"currency\":\"usd\","
            + "\"idempotencyKey\":\"api-key-currency\""
            + "}";

        mockMvc.perform(post("/api/v1/transfers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }
}
