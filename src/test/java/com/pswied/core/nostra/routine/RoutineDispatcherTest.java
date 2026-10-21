package com.pswied.core.nostra.routine;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class RoutineDispatcherTest {

    @Test
    void shouldExecuteKnownRoutine() {
        RoutineDispatcher dispatcher = new RoutineDispatcher(List.of(new BalanceSummaryRoutine()));

        RoutineResult result = dispatcher.execute("balance-summary", new RoutineContext(Map.of("accountId", "ACC-1")));

        assertThat(result.success()).isTrue();
        assertThat(result.name()).isEqualTo("balance-summary");
    }

    @Test
    void shouldRejectUnknownRoutine() {
        RoutineDispatcher dispatcher = new RoutineDispatcher(List.of(new BalanceSummaryRoutine()));

        assertThatThrownBy(() -> dispatcher.execute("missing-routine", new RoutineContext(Map.of())))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("Unknown routine");
    }
}
