package com.pswied.core.nostra.cob;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.pswied.core.nostra.account.Account;
import com.pswied.core.nostra.account.AccountRepository;
import com.pswied.core.nostra.account.AccountStatus;
import com.pswied.core.nostra.account.TransferRecord;
import com.pswied.core.nostra.account.TransferRepository;
import com.pswied.core.nostra.account.TransferStatus;
import com.pswied.core.nostra.lending.Loan;
import com.pswied.core.nostra.lending.LoanRepository;
import com.pswied.core.nostra.lending.LoanStatus;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CobControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private TransferRepository transferRepository;

    @Autowired
    private LoanRepository loanRepository;

    @BeforeEach
    void setUp() {
        transferRepository.deleteAll();
        loanRepository.deleteAll();
        accountRepository.deleteAll();

        accountRepository.save(new Account("ACC-COB-1", "USD", AccountStatus.ACTIVE, new BigDecimal("1000.00")));
        accountRepository.save(new Account("ACC-COB-2", "USD", AccountStatus.ACTIVE, new BigDecimal("250.00")));

        transferRepository.save(new TransferRecord(
            "TRF-001",
            "idempotent-1",
            "ACC-COB-1",
            "ACC-COB-2",
            new BigDecimal("50.00"),
            "USD",
            "hash-1",
            TransferStatus.COMPLETED
        ));
        transferRepository.save(new TransferRecord(
            "TRF-002",
            "idempotent-2",
            "ACC-COB-1",
            "ACC-COB-2",
            new BigDecimal("120.00"),
            "USD",
            "hash-2",
            TransferStatus.REJECTED
        ));
        transferRepository.save(new TransferRecord(
            "TRF-003",
            "idempotent-3",
            "ACC-COB-2",
            "ACC-COB-1",
            new BigDecimal("25.00"),
            "USD",
            "hash-3",
            TransferStatus.RECEIVED
        ));
        transferRepository.save(new TransferRecord(
            "TRF-004",
            "idempotent-4",
            "ACC-COB-1",
            "ACC-COB-2",
            new BigDecimal("80.00"),
            "USD",
            "hash-4",
            TransferStatus.VALIDATED
        ));

        loanRepository.save(new Loan(
            "LN-COB-1",
            "ACC-COB-1",
            new BigDecimal("900.00"),
            "USD",
            new BigDecimal("10.00"),
            6,
            new BigDecimal("155.25"),
            new BigDecimal("900.00"),
            LoanStatus.ACTIVE
        ));
        loanRepository.save(new Loan(
            "LN-COB-2",
            "ACC-COB-2",
            new BigDecimal("500.00"),
            "USD",
            new BigDecimal("12.00"),
            3,
            new BigDecimal("170.00"),
            new BigDecimal("120.00"),
            LoanStatus.OVERDUE
        ));
    }

    @Test
    void runCob_shouldCreateRunSummary() throws Exception {
        mockMvc.perform(post("/api/v1/cob/run"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.status").value("COMPLETED"))
            .andExpect(jsonPath("$.accountCount").value(2))
            .andExpect(jsonPath("$.completedTransferCount").value(1))
            .andExpect(jsonPath("$.rejectedTransferCount").value(1))
            .andExpect(jsonPath("$.activeLoanCount").value(1))
            .andExpect(jsonPath("$.pendingTransferCount").value(2))
            .andExpect(jsonPath("$.overdueLoanCount").value(1));
    }

    @Test
    void getLatestCob_shouldReturnMostRecentRun() throws Exception {
        mockMvc.perform(post("/api/v1/cob/run"))
            .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/cob/latest"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("COMPLETED"))
            .andExpect(jsonPath("$.summaryMessage").value(
                "Daily close processed for local simulator: 1 completed transfers, 1 rejected transfers, 2 pending transfers, 1 overdue loans."
            ));
    }
}
