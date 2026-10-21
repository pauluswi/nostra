package com.pswied.core.nostra.account;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransferPostingRepository extends JpaRepository<TransferPosting, UUID> {

    List<TransferPosting> findByTransferReference(String transferReference);
}
