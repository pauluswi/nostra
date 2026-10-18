package com.pswied.core.nostra.account;

import jakarta.transaction.Transactional;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class AccountService {

    private final AccountRepository accountRepository;

    public AccountService(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
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
            .orElseThrow(() -> new IllegalArgumentException("Source account not found: " + request.sourceAccountNumber()));
        Account destination = accountRepository.findByAccountNumberForUpdate(request.destinationAccountNumber())
            .orElseThrow(() -> new IllegalArgumentException("Destination account not found: " + request.destinationAccountNumber()));

        if (request.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Transfer amount must be positive");
        }
        if (!source.getCurrency().equals(request.currency()) || !destination.getCurrency().equals(request.currency())) {
            throw new IllegalArgumentException("Transfer currency mismatch");
        }
        if (source.getStatus() != AccountStatus.ACTIVE || destination.getStatus() != AccountStatus.ACTIVE) {
            throw new IllegalArgumentException("Only active accounts can transfer");
        }
        if (source.getAccountNumber().equals(destination.getAccountNumber())) {
            throw new IllegalArgumentException("Source and destination account cannot be the same");
        }
        if (source.getBalance().compareTo(request.amount()) < 0) {
            throw new IllegalArgumentException("Insufficient funds");
        }

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
