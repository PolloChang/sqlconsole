package com.sqlconsole.core.core.vdba;

import com.sqlconsole.core.model.entity.DbConfig;
import com.sqlconsole.core.service.DbConfigService;
import jakarta.annotation.PostConstruct;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/** Service for Virtual DBA diagnostics. */
@Slf4j
@Service
@RequiredArgsConstructor
public class VirtualDbaService {

  private final DbConfigService dbConfigService;
  private final List<VirtualDbaProvider> providerList;
  private Map<String, VirtualDbaProvider> providerMap;

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
  public VirtualDbaReport diagnose(Long dbConfigId, String sql) {
    DbConfig config = dbConfigService.getConfigById(dbConfigId);
    if (config == null) {
      throw new IllegalArgumentException("Database configuration not found for ID: " + dbConfigId);
    }

    String dbType = String.valueOf(config.getDbType()).toUpperCase();
    VirtualDbaProvider provider = providerMap.get(dbType);

    if (provider == null) {
      return new VirtualDbaReport(
          new ExecutionPlan("{}", "JSON"),
          List.of(
              new DbaSuggestion(
                  "INFO",
                  "Virtual DBA not supported for "
                      + dbType
                      + ". Upgrade to Premium for full support.")));
    }

    try (Connection conn = dbConfigService.createConnection(config)) {
      // Ensure safety: Disable auto-commit to prevent DML execution from persisting
      // This is critical for providers like PostgreSQL where EXPLAIN ANALYZE executes the query.
      if (conn.getAutoCommit()) {
        conn.setAutoCommit(false);
      }

      try {
        ExecutionPlan plan = provider.explain(conn, sql);
        List<DbaSuggestion> suggestions = provider.getSuggestions(conn, sql);
        return new VirtualDbaReport(plan, suggestions);
      } finally {
        // Always rollback, regardless of success or failure
        conn.rollback();
      }
    } catch (SQLException e) {
      log.error("Error diagnosing SQL for DB: {}", dbConfigId, e);
      return new VirtualDbaReport(
          new ExecutionPlan("{\"error\": \"" + e.getMessage() + "\"}", "JSON"), List.of());
    }
  }
}
