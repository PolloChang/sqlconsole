package com.sqlconsole.core.core.vdba;

import com.sqlconsole.core.core.vdba.analysis.AnalysisResult;
import com.sqlconsole.core.core.vdba.analysis.ExecutionPlanAnalyzer;
import com.sqlconsole.core.model.entity.DbConfig;
import com.sqlconsole.core.service.DbConfigService;
import jakarta.annotation.PostConstruct;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/** Service for Virtual DBA diagnostics. */
@Slf4j
@Service
@RequiredArgsConstructor
public class VirtualDbaService {

  private final DbConfigService dbConfigService;
  private final List<VirtualDbaProvider> providerList;
  private final ExecutionPlanAnalyzer analyzer;
  private Map<String, VirtualDbaProvider> providerMap;

  /** Initializes the provider map by mapping provider types to their instances. */
  @PostConstruct
  public void init() {
    providerMap =
        providerList.stream()
            .collect(Collectors.toMap(p -> p.getProviderType().toUpperCase(), Function.identity()));
  }

  /**
   * Diagnoses the given SQL query on the specified database.
   *
   * @param dbConfigId The ID of the database configuration.
   * @param sql The SQL query to analyze.
   * @return A report containing the execution plan and suggestions.
   */
  @Async("exportTaskExecutor")
  public CompletableFuture<VirtualDbaReport> diagnose(Long dbConfigId, String sql) {
    DbConfig config = dbConfigService.getConfigById(dbConfigId);
    if (config == null) {
      throw new IllegalArgumentException("Database configuration not found for ID: " + dbConfigId);
    }

    String dbType = String.valueOf(config.getDbType()).toUpperCase();
    VirtualDbaProvider provider = providerMap.get(dbType);

    if (provider == null) {
      return CompletableFuture.completedFuture(
          new VirtualDbaReport(
              new ExecutionPlan("{}", "JSON"),
              List.of(
                  new DbaSuggestion(
                      "INFO",
                      "Virtual DBA not supported for "
                          + dbType
                          + ". Upgrade to Premium for full support.")),
              null,
              Collections.emptyMap()));
    }

    try (Connection conn = dbConfigService.createConnection(config)) {
      // Ensure safety: Disable auto-commit to prevent DML execution from persisting
      if (conn.getAutoCommit()) {
        conn.setAutoCommit(false);
      }

      try {
        // 1. Get Plan
        ExecutionPlan plan = provider.explain(conn, sql);
        UnifiedExecutionNode normalizedPlan = provider.normalize(plan.rawJson());

        // 2. Analyze Plan (Heuristics)
        AnalysisResult analysisResult = analyzer.analyze(normalizedPlan);
        UnifiedExecutionNode enrichedPlan = analysisResult.enrichedRoot();
        List<DbaSuggestion> heuristicSuggestions = analysisResult.suggestions();

        // 3. Get Provider Specific Suggestions (Index Advisor)
        List<DbaSuggestion> providerSuggestions =
            provider.getSuggestions(
                conn, sql, enrichedPlan != null ? enrichedPlan : normalizedPlan);

        // 4. Combine Suggestions
        providerSuggestions.addAll(heuristicSuggestions);

        return CompletableFuture.completedFuture(
            new VirtualDbaReport(plan, providerSuggestions, enrichedPlan, Collections.emptyMap()));
      } finally {
        conn.rollback();
      }
    } catch (SQLException e) {
      log.error("Error diagnosing SQL for DB: {}", dbConfigId, e);
      return CompletableFuture.completedFuture(
          new VirtualDbaReport(
              new ExecutionPlan("{\"error\": \"" + e.getMessage() + "\"}", "JSON"),
              List.of(),
              null,
              Collections.emptyMap()));
    }
  }
}
