package com.pswied.core.nostra.lending;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.pswied.core.nostra.account.Account;
import com.pswied.core.nostra.account.AccountRepository;
import com.pswied.core.nostra.account.AccountStatus;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class LoanServiceTest {

    @Autowired
    private LoanService loanService;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private LoanRepository loanRepository;

    @Autowired
    private LoanRepaymentRepository loanRepaymentRepository;

    @BeforeEach
    void setUp() {
        loanRepaymentRepository.deleteAll();
        loanRepository.deleteAll();
        accountRepository.deleteAll();
        accountRepository.save(new Account("ACC-L1", "USD", AccountStatus.ACTIVE, new BigDecimal("10000.00")));
    }

    @Test
    void shouldCreateLoanAndScheduleDeterministically() {
        LoanResponse response = loanService.createLoan(new LoanRequest("ACC-L1", new BigDecimal("1200.00"), "USD", new BigDecimal("12.00"), 12));

        assertThat(response.loanReference()).startsWith("LN-");
        assertThat(response.status()).isEqualTo(LoanStatus.ACTIVE);

        var schedule = loanService.getLoanSchedule(response.loanReference());
        assertThat(schedule).hasSize(12);
        assertThat(schedule.getLast().closingBalance()).isEqualByComparingTo("0.00");

        BigDecimal totalPrincipal = schedule.stream().map(LoanScheduleEntryDto::principalComponent).reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(totalPrincipal).isEqualByComparingTo("1200.00");
    }

    @Test
    void shouldRejectInvalidLoanTerm() {
        assertThatThrownBy(() -> loanService.createLoan(new LoanRequest("ACC-L1", new BigDecimal("1000.00"), "USD", new BigDecimal("5.00"), 0)))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("term");
    }

    @Test
    void shouldRejectDuplicateRepaymentKey() {
        LoanResponse loan = loanService.createLoan(new LoanRequest("ACC-L1", new BigDecimal("1000.00"), "USD", new BigDecimal("0.00"), 4));
        LoanRepaymentResponse first = loanService.applyRepayment(loan.loanReference(), new LoanRepaymentRequest(new BigDecimal("250.00"), "repay-1"));
        assertThat(first.status()).isEqualTo(LoanStatus.ACTIVE);

        LoanRepaymentResponse duplicate = loanService.applyRepayment(loan.loanReference(), new LoanRepaymentRequest(new BigDecimal("250.00"), "repay-1"));
        assertThat(duplicate.principalComponent()).isEqualByComparingTo(first.principalComponent());
    }
}
