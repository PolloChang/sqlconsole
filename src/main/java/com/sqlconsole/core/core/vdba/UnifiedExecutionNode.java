package com.sqlconsole.core.core.vdba;

import java.util.List;
import java.util.Map;

/**
 * Normalized representation of a database execution plan node.
 *
 * @param operation The operation name (e.g., "Seq Scan", "Index Scan").
 * @param cost The estimated cost.
 * @param rows The estimated row count.
 * @param actualTime The actual execution time (if available).
 * @param children Child nodes.
 * @param tags Analysis metadata (e.g., "isBottleneck", "relativeCost").
 */
public record UnifiedExecutionNode(
    String operation,
    Double cost,
    Double rows,
    Double actualTime,
    List<UnifiedExecutionNode> children,
    Map<String, Object> tags) {}
