package com.pswied.core.nostra.account;

import com.pswied.core.nostra.rules.BusinessRuleException;
import com.pswied.core.nostra.rules.TransferRules;
import jakarta.transaction.Transactional;
import java.math.BigDecimal;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class AccountService {

    private final AccountRepository accountRepository;
    private final TransferRules transferRules;
    private final TransferRepository transferRepository;
    private final TransferPostingRepository transferPostingRepository;

    public AccountService(AccountRepository accountRepository, TransferRules transferRules,
                          TransferRepository transferRepository, TransferPostingRepository transferPostingRepository) {
        this.accountRepository = accountRepository;
        this.transferRules = transferRules;
        this.transferRepository = transferRepository;
        this.transferPostingRepository = transferPostingRepository;
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
        String payloadHash = hashPayload(request);
        TransferRecord existing = transferRepository.findByIdempotencyKey(request.idempotencyKey()).orElse(null);
        if (existing != null) {
            if (!existing.getPayloadHash().equals(payloadHash)) {
                throw new BusinessRuleException(
                    "IDEMPOTENCY_KEY_CONFLICT",
                    "Idempotency key already used with different payload"
                );
            }
            return toResponse(existing);
        }

        Account source = accountRepository.findByAccountNumberForUpdate(request.sourceAccountNumber())
            .orElseThrow(() -> new BusinessRuleException("ACCOUNT_NOT_FOUND", "Source account not found: " + request.sourceAccountNumber()));
        Account destination = accountRepository.findByAccountNumberForUpdate(request.destinationAccountNumber())
            .orElseThrow(() -> new BusinessRuleException("ACCOUNT_NOT_FOUND", "Destination account not found: " + request.destinationAccountNumber()));

        transferRules.validate(source, destination, request.amount(), request.currency());

        String reference = "TRF-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        TransferRecord transfer = new TransferRecord(
            reference,
            request.idempotencyKey(),
            source.getAccountNumber(),
            destination.getAccountNumber(),
            request.amount(),
            request.currency(),
            payloadHash,
            TransferStatus.VALIDATED
        );

        source.debit(request.amount());
        destination.credit(request.amount());
        transfer = transferRepository.save(transfer);

        transferPostingRepository.save(new TransferPosting(reference, source.getAccountNumber(), "DEBIT", request.amount(), request.currency()));
        transferPostingRepository.save(new TransferPosting(reference, destination.getAccountNumber(), "CREDIT", request.amount(), request.currency()));
        accountRepository.save(source);
        accountRepository.save(destination);

        transfer.setStatus(TransferStatus.COMPLETED);
        transfer = transferRepository.save(transfer);

        return toResponse(transfer);
    }

    public TransferResponse getTransfer(String reference) {
        TransferRecord transfer = transferRepository.findByReference(reference)
            .orElseThrow(() -> new BusinessRuleException("TRANSFER_NOT_FOUND", "Transfer not found: " + reference));
        return toResponse(transfer);
    }

    private TransferResponse toResponse(TransferRecord transfer) {
        return new TransferResponse(
            transfer.getReference(),
            transfer.getStatus().name(),
            transfer.getSourceAccountNumber(),
            transfer.getDestinationAccountNumber(),
            transfer.getAmount(),
            transfer.getCurrency(),
            "Transfer successful"
        );
    }

    private String hashPayload(TransferRequest request) {
        String payload = request.sourceAccountNumber() + "|" + request.destinationAccountNumber() + "|" + request.amount() + "|" + request.currency();
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(payload.getBytes());
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm unavailable", e);
        }
    }
}
