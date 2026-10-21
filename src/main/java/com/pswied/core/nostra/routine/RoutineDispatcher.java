package com.pswied.core.nostra.routine;

import com.pswied.core.nostra.rules.BusinessRuleException;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class RoutineDispatcher {

    private final Map<String, SimulationRoutine> routines;

    public RoutineDispatcher(List<SimulationRoutine> routines) {
        this.routines = routines.stream()
            .collect(Collectors.toMap(SimulationRoutine::name, Function.identity()));
    }

    public RoutineResult execute(String routineName, RoutineContext context) {
        SimulationRoutine routine = routines.get(routineName);
        if (routine == null) {
            throw new BusinessRuleException("ROUTINE_NOT_FOUND", "Unknown routine: " + routineName);
        }
        return routine.execute(context);
    }
}
