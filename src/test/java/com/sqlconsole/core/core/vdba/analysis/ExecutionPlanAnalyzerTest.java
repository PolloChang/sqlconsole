package com.sqlconsole.core.core.vdba.analysis;

import static org.junit.jupiter.api.Assertions.*;

import com.sqlconsole.core.core.vdba.DbaSuggestion;
import com.sqlconsole.core.core.vdba.UnifiedExecutionNode;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;

class ExecutionPlanAnalyzerTest {

  private final ExecutionPlanAnalyzer analyzer = new ExecutionPlanAnalyzer();

  @Test
  void testAnalyze_EnrichesBottleneck() {
    // Root: Cost 100
    // Child: Cost 80 (80%) -> Should be bottleneck
    UnifiedExecutionNode child =
        new UnifiedExecutionNode("Child", 80.0, 100.0, 10.0, List.of(), Collections.emptyMap());
    UnifiedExecutionNode root =
        new UnifiedExecutionNode(
            "Root", 100.0, 100.0, 20.0, List.of(child), Collections.emptyMap());

    AnalysisResult result = analyzer.analyze(root);
    UnifiedExecutionNode enrichedRoot = result.enrichedRoot();

    assertNotNull(enrichedRoot);
    assertEquals(
        2, enrichedRoot.tags().size()); // relativeCost, isBottleneck (maybe) or just relativeCost

    UnifiedExecutionNode enrichedChild = enrichedRoot.children().get(0);
    assertTrue((Double) enrichedChild.tags().get("relativeCost") > 50.0);
    assertTrue((Boolean) enrichedChild.tags().get("isBottleneck"));
  }

  @Test
  void testAnalyze_MissingIndex() {
    UnifiedExecutionNode node =
        new UnifiedExecutionNode(
            "Seq Scan on table", 50.0, 2000.0, 5.0, List.of(), Collections.emptyMap());

    AnalysisResult result = analyzer.analyze(node);

    List<DbaSuggestion> suggestions = result.suggestions();
    assertFalse(suggestions.isEmpty());
    assertTrue(suggestions.stream().anyMatch(s -> s.message().contains("Missing Index")));
  }
}
