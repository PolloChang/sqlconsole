package com.sqlconsole.core.core.vdba;

/**
 * Represents the execution plan of a SQL query.
 *
 * @param rawJson The raw JSON output of the EXPLAIN command.
 * @param format The format of the plan (e.g., "JSON", "TEXT").
 */
public record ExecutionPlan(String rawJson, String format) {}
