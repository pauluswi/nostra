package com.pswied.core.nostra.routine;

import java.util.Map;

public record RoutineResult(String name, boolean success, String outcome, Map<String, Object> data) {
}
