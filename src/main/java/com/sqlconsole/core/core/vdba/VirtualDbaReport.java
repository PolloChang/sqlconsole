package com.sqlconsole.core.core.vdba;

import java.util.List;

/**
 * The full report from the Virtual DBA diagnosis.
 *
 * @param plan The execution plan.
 * @param suggestions A list of optimization suggestions.
 * @param normalizedPlan The normalized execution plan tree.
 */
public record VirtualDbaReport(
    ExecutionPlan plan, List<DbaSuggestion> suggestions, UnifiedExecutionNode normalizedPlan) {}
