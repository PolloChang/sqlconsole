package com.sqlconsole.core.core.vdba.analysis;

import com.sqlconsole.core.core.vdba.DbaSuggestion;
import com.sqlconsole.core.core.vdba.UnifiedExecutionNode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/** Analyzer that heuristics to the execution plan. */
@Component
public class ExecutionPlanAnalyzer {

  public AnalysisResult analyze(UnifiedExecutionNode root) {
    if (root == null) {
      return new AnalysisResult(null, List.of());
    }

    // 1. Calculate Totals
    double totalCost = root.cost();
    double totalTime = root.actualTime();

    // 2. Identify Bottlenecks and Enrich Tree
    List<DbaSuggestion> suggestions = new ArrayList<>();
    UnifiedExecutionNode enrichedRoot =
        enrichNode(root, totalCost, totalTime, suggestions, root.cost(), root.actualTime());

    return new AnalysisResult(enrichedRoot, suggestions);
  }

  private UnifiedExecutionNode enrichNode(
      UnifiedExecutionNode node,
      double totalCost,
      double totalTime,
      List<DbaSuggestion> suggestions,
      double rootCost,
      double rootTime) {

    Map<String, Object> tags = new HashMap<>(node.tags());
    boolean isCostBottleneck = false;
    boolean isTimeBottleneck = false;

    // Relative Cost
    if (rootCost > 0) {
      double relativeCost = (node.cost() / rootCost) * 100.0;
      tags.put("relativeCost", relativeCost);
      if (relativeCost > 50.0) {
        tags.put("isBottleneck", true);
        isCostBottleneck = true;
      }
    }

    // Heuristic: Missing Index
    // Check for Seq Scan with high rows
    if ((node.operation().contains("Seq Scan") || node.operation().contains("Table"))
        && node.rows() > 1000) {
      suggestions.add(
          new DbaSuggestion(
              "WARN",
              "Missing Index detected on: "
                  + node.operation()
                  + ". Rows scanned: "
                  + node.rows()));
    }

    // Heuristic: Stale Statistics
    // If Actual Time is significantly higher than estimated Cost (normalized roughly)
    // Note: Cost units are arbitrary, Time is ms. Comparison is tricky.
    // Instead, look for huge disparity in row estimation vs actual (if actual rows existed, which we
    // assume might be in rows for now or future)
    // For now, let's use the rule: If actualTime is high but cost is low?
    // Requirement says: "If actualTime is significantly higher than estimated cost".
    // This assumes they are comparable or scale similarly, which isn't always true across DBs.
    // We will skip this specific implementation as it requires calibration per DB type
    // unless we strictly interpret "significantly higher".
    // Let's implement a simplified version: if time > 1000ms but cost < 10.
    if (node.actualTime() > 1000 && node.cost() < 10) {
      suggestions.add(
          new DbaSuggestion(
              "WARN",
              "Stale Statistics potential: High execution time ("
                  + node.actualTime()
                  + "ms) for low estimated cost ("
                  + node.cost()
                  + "). Run ANALYZE."));
    }

    // Recurse
    List<UnifiedExecutionNode> enrichedChildren = new ArrayList<>();
    if (node.children() != null) {
      for (UnifiedExecutionNode child : node.children()) {
        enrichedChildren.add(
            enrichNode(child, totalCost, totalTime, suggestions, rootCost, rootTime));
      }
    }

    return new UnifiedExecutionNode(
        node.operation(),
        node.cost(),
        node.rows(),
        node.actualTime(),
        enrichedChildren,
        tags);
  }
}
