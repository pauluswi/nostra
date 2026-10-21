package com.pswied.core.nostra.routine;

public interface SimulationRoutine {

    String name();

    RoutineResult execute(RoutineContext context);
}
