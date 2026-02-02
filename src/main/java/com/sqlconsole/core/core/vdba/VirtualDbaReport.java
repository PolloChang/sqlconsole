package com.sqlconsole.core.core.vdba;

import java.util.List;
import java.util.Map;

/**
 * The full report from the Virtual DBA diagnosis.
 *
 * @param plan The execution plan.
 * @param suggestions A list of optimization suggestions.
 * @param normalizedPlan The normalized execution plan tree.
 * @param advancedMetrics Advanced metrics (e.g. Buffer Gets) for Premium features.
 */
public record VirtualDbaReport(
    ExecutionPlan plan,
    List<DbaSuggestion> suggestions,
    UnifiedExecutionNode normalizedPlan,
    Map<String, Object> advancedMetrics) {}
