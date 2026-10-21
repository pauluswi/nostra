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
    private final TransferAuditRepository transferAuditRepository;

    public AccountService(AccountRepository accountRepository, TransferRules transferRules,
                          TransferRepository transferRepository, TransferPostingRepository transferPostingRepository,
                          TransferAuditRepository transferAuditRepository) {
        this.accountRepository = accountRepository;
        this.transferRules = transferRules;
        this.transferRepository = transferRepository;
        this.transferPostingRepository = transferPostingRepository;
        this.transferAuditRepository = transferAuditRepository;
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
                String reference = existing.getReference();
                recordAudit(reference, "IDEMPOTENCY_CONFLICT", "REJECTED", "Idempotency key already used with different payload");
                throw new BusinessRuleException(
                    "IDEMPOTENCY_KEY_CONFLICT",
                    "Idempotency key already used with different payload"
                );
            }
            recordAudit(existing.getReference(), "DUPLICATE_REQUEST", existing.getStatus().name(), "Returned original transfer response");
            return toResponse(existing);
        }

        String reference = "TRF-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        TransferRecord transfer = new TransferRecord(
            reference,
            request.idempotencyKey(),
            request.sourceAccountNumber(),
            request.destinationAccountNumber(),
            request.amount(),
            request.currency(),
            payloadHash,
            TransferStatus.RECEIVED
        );
        transfer = transferRepository.save(transfer);
        recordAudit(reference, "RECEIVED", transfer.getStatus().name(), "Transfer accepted for processing");

        Account source = accountRepository.findByAccountNumberForUpdate(request.sourceAccountNumber()).orElse(null);
        if (source == null) {
            transfer.setStatus(TransferStatus.REJECTED);
            transferRepository.save(transfer);
            recordAudit(reference, "VALIDATION_FAILED", "REJECTED", "Source account not found: " + request.sourceAccountNumber());
            throw new BusinessRuleException("ACCOUNT_NOT_FOUND", "Source account not found: " + request.sourceAccountNumber());
        }

        Account destination = accountRepository.findByAccountNumberForUpdate(request.destinationAccountNumber()).orElse(null);
        if (destination == null) {
            transfer.setStatus(TransferStatus.REJECTED);
            transferRepository.save(transfer);
            recordAudit(reference, "VALIDATION_FAILED", "REJECTED", "Destination account not found: " + request.destinationAccountNumber());
            throw new BusinessRuleException("ACCOUNT_NOT_FOUND", "Destination account not found: " + request.destinationAccountNumber());
        }

        try {
            transferRules.validate(source, destination, request.amount(), request.currency());
            transfer.setStatus(TransferStatus.VALIDATED);
            transferRepository.save(transfer);
            recordAudit(reference, "VALIDATED", transfer.getStatus().name(), "Transfer passed business validation");

            source.debit(request.amount());
            destination.credit(request.amount());
            transferPostingRepository.save(new TransferPosting(reference, source.getAccountNumber(), "DEBIT", request.amount(), request.currency()));
            transferPostingRepository.save(new TransferPosting(reference, destination.getAccountNumber(), "CREDIT", request.amount(), request.currency()));
            accountRepository.save(source);
            accountRepository.save(destination);

            transfer.setStatus(TransferStatus.COMPLETED);
            transfer = transferRepository.save(transfer);
            recordAudit(reference, "COMPLETED", transfer.getStatus().name(), "Transfer posted successfully");
            return toResponse(transfer);
        } catch (BusinessRuleException ex) {
            transfer.setStatus(TransferStatus.REJECTED);
            transferRepository.save(transfer);
            recordAudit(reference, "REJECTED", "REJECTED", ex.getMessage());
            throw ex;
        }
    }

    public TransferResponse getTransfer(String reference) {
        TransferRecord transfer = transferRepository.findByReference(reference)
            .orElseThrow(() -> new BusinessRuleException("TRANSFER_NOT_FOUND", "Transfer not found: " + reference));
        return toResponse(transfer);
    }

    public List<TransferAuditDto> getTransferAudit(String reference) {
        transferRepository.findByReference(reference)
            .orElseThrow(() -> new BusinessRuleException("TRANSFER_NOT_FOUND", "Transfer not found: " + reference));
        return transferAuditRepository.findByTransferReferenceOrderByCreatedAtAsc(reference).stream()
            .map(entry -> new TransferAuditDto(
                entry.getTransferReference(),
                entry.getActionName(),
                entry.getOutcome(),
                entry.getMessage(),
                entry.getCorrelationId(),
                entry.getCreatedAt()))
            .collect(Collectors.toList());
    }

    private TransferResponse toResponse(TransferRecord transfer) {
        String message = switch (transfer.getStatus()) {
            case COMPLETED -> "Transfer successful";
            case REJECTED -> "Transfer rejected";
            default -> "Transfer processed";
        };
        return new TransferResponse(
            transfer.getReference(),
            transfer.getStatus().name(),
            transfer.getSourceAccountNumber(),
            transfer.getDestinationAccountNumber(),
            transfer.getAmount(),
            transfer.getCurrency(),
            message
        );
    }

    private void recordAudit(String transferReference, String actionName, String outcome, String message) {
        transferAuditRepository.save(new TransferAuditEntry(
            transferReference,
            actionName,
            outcome,
            message,
            UUID.randomUUID().toString()
        ));
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
