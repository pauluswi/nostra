package com.pswied.core.nostra.account;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransferRepository extends JpaRepository<TransferRecord, UUID> {

    Optional<TransferRecord> findByIdempotencyKey(String idempotencyKey);

    Optional<TransferRecord> findByReference(String reference);
}
