package com.pswied.core.nostra.lending;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LoanRepository extends JpaRepository<Loan, UUID> {

    Optional<Loan> findByLoanReference(String loanReference);
}
