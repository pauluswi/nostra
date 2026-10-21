package com.pswied.core.nostra.account;

import com.pswied.core.nostra.rules.BusinessRuleException;
import com.pswied.core.nostra.rules.TransferRules;
import jakarta.transaction.Transactional;
import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class AccountService {

    private final AccountRepository accountRepository;
    private final TransferRules transferRules;

    public AccountService(AccountRepository accountRepository, TransferRules transferRules) {
        this.accountRepository = accountRepository;
        this.transferRules = transferRules;
    }

    public List<AccountDto> listAccounts() {
        return accountRepository.findAll().stream()
            .map(account -> new AccountDto(
                account.getAccountNumber(),
                account.getCurrency(),
                account.getStatus(),
                account.getBalance()))
            .collect(Collectors.toList());
    }

    public AccountDto getAccount(String accountNumber) {
        Account account = accountRepository.findByAccountNumber(accountNumber)
            .orElseThrow(() -> new IllegalArgumentException("Account not found: " + accountNumber));
        return new AccountDto(account.getAccountNumber(), account.getCurrency(), account.getStatus(), account.getBalance());
    }

    public AccountDto createAccount(String accountNumber, String currency, AccountStatus status, BigDecimal balance) {
        if (accountRepository.findByAccountNumber(accountNumber).isPresent()) {
            throw new IllegalArgumentException("Account already exists: " + accountNumber);
        }
        Account account = new Account(accountNumber, currency, status, balance);
        Account saved = accountRepository.save(account);
        return new AccountDto(saved.getAccountNumber(), saved.getCurrency(), saved.getStatus(), saved.getBalance());
    }

    @Transactional
    public TransferResponse transfer(TransferRequest request) {
        Account source = accountRepository.findByAccountNumberForUpdate(request.sourceAccountNumber())
            .orElseThrow(() -> new BusinessRuleException("ACCOUNT_NOT_FOUND", "Source account not found: " + request.sourceAccountNumber()));
        Account destination = accountRepository.findByAccountNumberForUpdate(request.destinationAccountNumber())
            .orElseThrow(() -> new BusinessRuleException("ACCOUNT_NOT_FOUND", "Destination account not found: " + request.destinationAccountNumber()));

        transferRules.validate(source, destination, request.amount(), request.currency());

        source.debit(request.amount());
        destination.credit(request.amount());

        accountRepository.save(source);
        accountRepository.save(destination);

        return new TransferResponse(
            "SUCCESS",
            source.getAccountNumber(),
            destination.getAccountNumber(),
            request.amount(),
            request.currency(),
            "Transfer successful"
        );
    }
}
