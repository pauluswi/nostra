package com.pswied.core.nostra.routine;

import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class BalanceSummaryRoutine implements SimulationRoutine {

    @Override
    public String name() {
        return "balance-summary";
    }

    @Override
    public RoutineResult execute(RoutineContext context) {
        Object accountId = context.values().getOrDefault("accountId", "unknown");
        return new RoutineResult(
            name(),
            true,
            "SUCCESS",
            Map.of("accountId", accountId, "status", "simulated")
        );
    }
}
