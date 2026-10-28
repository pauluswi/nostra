package com.pswied.core.nostra.cob;

import com.pswied.core.nostra.account.AccountRepository;
import com.pswied.core.nostra.account.TransferRepository;
import com.pswied.core.nostra.account.TransferStatus;
import com.pswied.core.nostra.lending.LoanRepository;
import com.pswied.core.nostra.lending.LoanStatus;
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
        long activeLoanCount = loanRepository.countByStatus(LoanStatus.ACTIVE);

        String runReference = "COB-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        CobRun run = new CobRun(
            runReference,
            CobStatus.COMPLETED,
            accountCount,
            completedTransferCount,
            rejectedTransferCount,
            activeLoanCount,
            "Daily close processed for local simulator"
        );

        run = cobRunRepository.save(run);
        return toResponse(run);
    }

    public CobRunResponse getLatestCob() {
        CobRun run = cobRunRepository.findFirstByOrderByStartedAtDesc()
            .orElseThrow(() -> new IllegalStateException("No COB run has been executed yet"));
        return toResponse(run);
    }

    private CobRunResponse toResponse(CobRun run) {
        return new CobRunResponse(
            run.getRunReference(),
            run.getStatus(),
            run.getAccountCount(),
            run.getCompletedTransferCount(),
            run.getRejectedTransferCount(),
            run.getActiveLoanCount(),
            run.getStartedAt(),
            run.getFinishedAt(),
            run.getSummaryMessage()
        );
    }
}
