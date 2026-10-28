package com.pswied.core.nostra.cob;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CobRunRepository extends JpaRepository<CobRun, UUID> {

    Optional<CobRun> findFirstByOrderByStartedAtDesc();
}
