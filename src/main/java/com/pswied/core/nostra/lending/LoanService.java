package com.pswied.core.nostra.lending;

import com.pswied.core.nostra.account.Account;
import com.pswied.core.nostra.account.AccountRepository;
import com.pswied.core.nostra.account.AccountStatus;
import com.pswied.core.nostra.rules.BusinessRuleException;
import jakarta.transaction.Transactional;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class LoanService {

    private static final BigDecimal HUNDRED = new BigDecimal("100");
    private static final BigDecimal TWELVE = BigDecimal.valueOf(12);

    private final AccountRepository accountRepository;
    private final LoanRepository loanRepository;
    private final LoanRepaymentRepository loanRepaymentRepository;

    public LoanService(AccountRepository accountRepository, LoanRepository loanRepository,
                      LoanRepaymentRepository loanRepaymentRepository) {
        this.accountRepository = accountRepository;
        this.loanRepository = loanRepository;
        this.loanRepaymentRepository = loanRepaymentRepository;
    }

    public LoanResponse createLoan(LoanRequest request) {
        Account borrower = accountRepository.findByAccountNumber(request.borrowerAccountNumber())
            .orElseThrow(() -> new BusinessRuleException("LOAN_BORROWER_NOT_FOUND", "Borrower account not found: " + request.borrowerAccountNumber()));

        if (borrower.getStatus() != AccountStatus.ACTIVE) {
            throw new BusinessRuleException("LOAN_BORROWER_INACTIVE", "Borrower account must be active");
        }

        if (request.principal().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessRuleException("LOAN_PRINCIPAL_INVALID", "Loan principal must be positive");
        }

        if (request.termMonths() <= 0) {
            throw new BusinessRuleException("LOAN_TERM_INVALID", "Loan term must be greater than zero");
        }

        if (request.annualInterestRate().compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessRuleException("LOAN_RATE_INVALID", "Loan interest rate cannot be negative");
        }

        String currency = request.currency().trim().toUpperCase();
        if (currency.length() != 3) {
            throw new BusinessRuleException("LOAN_CURRENCY_INVALID", "Loan currency must be a 3-letter ISO code");
        }

        BigDecimal monthlyPayment = calculateMonthlyPayment(request.principal(), request.annualInterestRate(), request.termMonths());
        String loanReference = "LN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        Loan loan = new Loan(
            loanReference,
            borrower.getAccountNumber(),
            request.principal(),
            currency,
            request.annualInterestRate(),
            request.termMonths(),
            monthlyPayment,
            request.principal(),
            LoanStatus.ACTIVE
        );

        Loan saved = loanRepository.save(loan);
        return toResponse(saved);
    }

    public LoanResponse getLoan(String loanReference) {
        Loan loan = loanRepository.findByLoanReference(loanReference)
            .orElseThrow(() -> new BusinessRuleException("LOAN_NOT_FOUND", "Loan not found: " + loanReference));
        return toResponse(loan);
    }

    public List<LoanScheduleEntryDto> getLoanSchedule(String loanReference) {
        Loan loan = loanRepository.findByLoanReference(loanReference)
            .orElseThrow(() -> new BusinessRuleException("LOAN_NOT_FOUND", "Loan not found: " + loanReference));
        return buildSchedule(loan);
    }

    @Transactional
    public LoanRepaymentResponse applyRepayment(String loanReference, LoanRepaymentRequest request) {
        Loan loan = loanRepository.findByLoanReference(loanReference)
            .orElseThrow(() -> new BusinessRuleException("LOAN_NOT_FOUND", "Loan not found: " + loanReference));

        LoanRepayment existing = loanRepaymentRepository.findByIdempotencyKey(request.idempotencyKey()).orElse(null);
        if (existing != null) {
            if (!existing.getLoanReference().equals(loanReference)) {
                throw new BusinessRuleException("REPAYMENT_KEY_CONFLICT", "Idempotency key is already associated with a different loan");
            }
            return toRepaymentResponse(loan, existing);
        }

        List<LoanScheduleEntryDto> schedule = buildSchedule(loan);
        LoanScheduleEntryDto dueInstallment = schedule.getFirst();
        BigDecimal dueAmount = dueInstallment.paymentAmount();

        if (request.amount().compareTo(dueAmount) < 0) {
            throw new BusinessRuleException("REPAYMENT_AMOUNT_INVALID", "Repayment amount must be at least the scheduled installment");
        }

        BigDecimal interestComponent = dueInstallment.interestComponent();
        BigDecimal principalComponent = request.amount().subtract(interestComponent).setScale(4, RoundingMode.HALF_UP);
        if (principalComponent.compareTo(loan.getOutstandingBalance()) > 0) {
            principalComponent = loan.getOutstandingBalance();
        }

        BigDecimal updatedBalance = loan.getOutstandingBalance().subtract(principalComponent).setScale(4, RoundingMode.HALF_UP);
        if (updatedBalance.compareTo(BigDecimal.ZERO) <= 0) {
            updatedBalance = BigDecimal.ZERO;
            loan.setStatus(LoanStatus.PAID);
        }

        loan.setOutstandingBalance(updatedBalance);
        loanRepository.save(loan);

        LoanRepayment repayment = new LoanRepayment(
            loanReference,
            request.idempotencyKey(),
            request.amount(),
            principalComponent,
            interestComponent
        );
        repayment = loanRepaymentRepository.save(repayment);

        return toRepaymentResponse(loan, repayment);
    }

    private BigDecimal calculateMonthlyPayment(BigDecimal principal, BigDecimal annualRate, int termMonths) {
        BigDecimal monthlyRate = annualRate.divide(HUNDRED, 12, RoundingMode.HALF_UP)
            .divide(TWELVE, 12, RoundingMode.HALF_UP);

        if (monthlyRate.compareTo(BigDecimal.ZERO) == 0) {
            return principal.divide(BigDecimal.valueOf(termMonths), 4, RoundingMode.HALF_UP);
        }

        BigDecimal factor = BigDecimal.ONE.add(monthlyRate);
        BigDecimal compounded = factor.pow(termMonths);
        BigDecimal denominator = BigDecimal.ONE.subtract(BigDecimal.ONE.divide(compounded, 12, RoundingMode.HALF_UP));
        BigDecimal numerator = principal.multiply(monthlyRate);
        return numerator.divide(denominator, 4, RoundingMode.HALF_UP);
    }

    private List<LoanScheduleEntryDto> buildSchedule(Loan loan) {
        BigDecimal balance = loan.getPrincipal();
        BigDecimal monthlyRate = loan.getAnnualInterestRate().divide(HUNDRED, 12, RoundingMode.HALF_UP)
            .divide(TWELVE, 12, RoundingMode.HALF_UP);
        BigDecimal monthlyPayment = loan.getMonthlyPayment();
        List<LoanScheduleEntryDto> entries = new ArrayList<>();

        for (int installment = 1; installment <= loan.getTermMonths(); installment++) {
            LocalDate dueDate = LocalDate.now().plusMonths(installment);
            BigDecimal openingBalance = balance.setScale(4, RoundingMode.HALF_UP);
            BigDecimal interestComponent = balance.multiply(monthlyRate).setScale(4, RoundingMode.HALF_UP);
            BigDecimal principalComponent;
            BigDecimal paymentAmount;

            if (installment == loan.getTermMonths()) {
                principalComponent = balance;
                paymentAmount = balance.add(interestComponent).setScale(4, RoundingMode.HALF_UP);
            } else {
                paymentAmount = monthlyPayment.setScale(4, RoundingMode.HALF_UP);
                principalComponent = paymentAmount.subtract(interestComponent).setScale(4, RoundingMode.HALF_UP);
                if (principalComponent.compareTo(balance) > 0) {
                    principalComponent = balance;
                    paymentAmount = principalComponent.add(interestComponent).setScale(4, RoundingMode.HALF_UP);
                }
            }

            BigDecimal closingBalance = balance.subtract(principalComponent).setScale(4, RoundingMode.HALF_UP);
            entries.add(new LoanScheduleEntryDto(
                installment,
                dueDate,
                openingBalance,
                paymentAmount,
                principalComponent,
                interestComponent,
                closingBalance
            ));
            balance = closingBalance;
        }

        return entries;
    }

    private LoanResponse toResponse(Loan loan) {
        return new LoanResponse(
            loan.getLoanReference(),
            loan.getBorrowerAccountNumber(),
            loan.getPrincipal(),
            loan.getCurrency(),
            loan.getAnnualInterestRate(),
            loan.getTermMonths(),
            loan.getMonthlyPayment(),
            loan.getOutstandingBalance(),
            loan.getStatus()
        );
    }

    private LoanRepaymentResponse toRepaymentResponse(Loan loan, LoanRepayment repayment) {
        return new LoanRepaymentResponse(
            loan.getLoanReference(),
            repayment.getAmount(),
            repayment.getPrincipalComponent(),
            repayment.getInterestComponent(),
            loan.getOutstandingBalance(),
            loan.getStatus()
        );
    }
}
