package com.sqlconsole.core.model.dto;

import java.util.List;
import java.util.Map;

/**
 * Result of a SQL execution.
 *
 * @param status the execution status (SUCCESS, ERROR, PENDING)
 * @param txStatus the transaction status (COMMITTED, UNCOMMIT)
 * @param message the result message or error description
 * @param columns the list of column names
 * @param rows the list of rows (map of column name to value)
 */
public record SqlResult(
    String status,
    String txStatus,
    String message,
    List<String> columns,
    List<Map<String, Object>> rows) {}
