package com.pswied.core.nostra.account;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransferAuditRepository extends JpaRepository<TransferAuditEntry, UUID> {

    List<TransferAuditEntry> findByTransferReferenceOrderByCreatedAtAsc(String transferReference);
}
