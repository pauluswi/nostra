package com.pswied.core.nostra.cob;

import com.pswied.core.nostra.account.AccountRepository;
import com.pswied.core.nostra.account.TransferRecord;
import com.pswied.core.nostra.account.TransferRepository;
import com.pswied.core.nostra.account.TransferStatus;
import com.pswied.core.nostra.lending.Loan;
import com.pswied.core.nostra.lending.LoanRepository;
import com.pswied.core.nostra.lending.LoanStatus;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class CobService {

    private final AccountRepository accountRepository;
    private final TransferRepository transferRepository;
    private final LoanRepository loanRepository;
    private final CobRunRepository cobRunRepository;

    public CobService(AccountRepository accountRepository,
                      TransferRepository transferRepository,
                      LoanRepository loanRepository,
                      CobRunRepository cobRunRepository) {
        this.accountRepository = accountRepository;
        this.transferRepository = transferRepository;
        this.loanRepository = loanRepository;
        this.cobRunRepository = cobRunRepository;
    }

    public CobRunResponse runDailyCob() {
        long accountCount = accountRepository.count();
        long completedTransferCount = transferRepository.countByStatus(TransferStatus.COMPLETED);
        long rejectedTransferCount = transferRepository.countByStatus(TransferStatus.REJECTED);
        long pendingTransferCount = transferRepository.countByStatus(TransferStatus.RECEIVED)
            + transferRepository.countByStatus(TransferStatus.VALIDATED);
        long activeLoanCount = loanRepository.countByStatus(LoanStatus.ACTIVE);
        long overdueLoanCount = loanRepository.countByStatus(LoanStatus.OVERDUE);

        List<String> processingStages = List.of(
            "PENDING_ITEMS_REVIEW: " + (pendingTransferCount + overdueLoanCount) + " item(s) require review.",
            "SETTLEMENT_CHECK: " + completedTransferCount + " completed transfer(s) are ready for settlement.",
            "EXCEPTION_REVIEW: " + rejectedTransferCount + " rejected transfer(s) need investigation.",
            "DAILY_CLOSE: close summary recorded."
        );

        String summaryMessage = String.format(
            "Daily close processed for local simulator: %d completed transfers, %d rejected transfers, %d pending transfers, %d overdue loans.",
            completedTransferCount,
            rejectedTransferCount,
            pendingTransferCount,
            overdueLoanCount
        );

        String runReference = "COB-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        CobRun run = new CobRun(
            runReference,
            CobStatus.COMPLETED,
            accountCount,
            completedTransferCount,
            rejectedTransferCount,
            activeLoanCount,
            pendingTransferCount,
            overdueLoanCount,
            summaryMessage,
            String.join("|", processingStages)
        );

        run = cobRunRepository.save(run);
        return toResponse(run);
    }

    public CobRunResponse getLatestCob() {
        CobRun run = cobRunRepository.findFirstByOrderByStartedAtDesc()
            .orElseThrow(() -> new IllegalStateException("No COB run has been executed yet"));
        return toResponse(run);
    }

    public CobReportResponse getLatestCobReport() {
        CobRun run = cobRunRepository.findFirstByOrderByStartedAtDesc()
            .orElseThrow(() -> new IllegalStateException("No COB run has been executed yet"));

        List<String> pendingTransferReferences = transferRepository.findAll().stream()
            .filter(transfer -> transfer.getStatus() == TransferStatus.RECEIVED || transfer.getStatus() == TransferStatus.VALIDATED)
            .map(TransferRecord::getReference)
            .sorted()
            .toList();

        List<String> overdueLoanReferences = loanRepository.findAll().stream()
            .filter(loan -> loan.getStatus() == LoanStatus.OVERDUE)
            .map(Loan::getLoanReference)
            .sorted()
            .toList();

        return new CobReportResponse(
            run.getRunReference(),
            run.getSummaryMessage(),
            parseStages(run.getProcessingStages()),
            run.getPendingTransferCount(),
            run.getRejectedTransferCount(),
            run.getOverdueLoanCount(),
            pendingTransferReferences,
            overdueLoanReferences,
            Instant.now()
        );
    }

    private CobRunResponse toResponse(CobRun run) {
        List<String> processingStages = parseStages(run.getProcessingStages());
        return new CobRunResponse(
            run.getRunReference(),
            run.getStatus(),
            run.getAccountCount(),
            run.getCompletedTransferCount(),
            run.getRejectedTransferCount(),
            run.getActiveLoanCount(),
            run.getPendingTransferCount(),
            run.getOverdueLoanCount(),
            processingStages,
            run.getStartedAt(),
            run.getFinishedAt(),
            run.getSummaryMessage()
        );
    }

    private List<String> parseStages(String processingStages) {
        if (processingStages == null || processingStages.isBlank()) {
            return List.of();
        }
        return List.of(processingStages.split("\\|", -1));
    }
}
