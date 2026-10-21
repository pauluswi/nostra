package com.pswied.core.nostra.lending;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.pswied.core.nostra.account.Account;
import com.pswied.core.nostra.account.AccountRepository;
import com.pswied.core.nostra.account.AccountStatus;
import java.math.BigDecimal;
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
class LoanControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AccountRepository accountRepository;

    @BeforeEach
    void setUp() {
        accountRepository.deleteAll();
        accountRepository.save(new Account("ACC-LN-1", "USD", AccountStatus.ACTIVE, new BigDecimal("10000.00")));
    }

    @Test
    void createLoan_shouldReturnCreatedLoan() throws Exception {
        String requestBody = "{"
            + "\"borrowerAccountNumber\":\"ACC-LN-1\","
            + "\"principal\":\"1200.00\","
            + "\"currency\":\"USD\","
            + "\"annualInterestRate\":\"12.00\","
            + "\"termMonths\":12"
            + "}";

        mockMvc.perform(post("/api/v1/loans")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.currency").value("USD"))
            .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void createLoan_shouldRejectInvalidCurrencyFormat() throws Exception {
        String requestBody = "{"
            + "\"borrowerAccountNumber\":\"ACC-LN-1\","
            + "\"principal\":\"1200.00\","
            + "\"currency\":\"usd\","
            + "\"annualInterestRate\":\"12.00\","
            + "\"termMonths\":12"
            + "}";

        mockMvc.perform(post("/api/v1/loans")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void createLoan_shouldRejectNonPositivePrincipal() throws Exception {
        String requestBody = "{"
            + "\"borrowerAccountNumber\":\"ACC-LN-1\","
            + "\"principal\":\"0.00\","
            + "\"currency\":\"USD\","
            + "\"annualInterestRate\":\"12.00\","
            + "\"termMonths\":12"
            + "}";

        mockMvc.perform(post("/api/v1/loans")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }
}
