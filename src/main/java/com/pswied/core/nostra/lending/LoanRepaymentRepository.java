package com.pswied.core.nostra.lending;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LoanRepaymentRepository extends JpaRepository<LoanRepayment, UUID> {

    Optional<LoanRepayment> findByIdempotencyKey(String idempotencyKey);
}
