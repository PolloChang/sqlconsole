package com.sqlconsole.core.core.vdba.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sqlconsole.core.core.vdba.DbaSuggestion;
import com.sqlconsole.core.core.vdba.ExecutionPlan;
import com.sqlconsole.core.core.vdba.UnifiedExecutionNode;
import com.sqlconsole.core.core.vdba.VirtualDbaProvider;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/** PostgreSQL implementation of VirtualDbaProvider. */
@Slf4j
@Component
public class PostgresqlDbaProvider implements VirtualDbaProvider {

  private final ObjectMapper objectMapper = new ObjectMapper();

  @Override
  public String getProviderType() {
    return "POSTGRESQL";
  }

  @Override
  public ExecutionPlan explain(Connection conn, String sql) {
    // Note: EXPLAIN ANALYZE executes the query!
    // Caller should ensure transaction rollback for DML.
    String explainSql = "EXPLAIN (ANALYZE, COSTS, VERBOSE, BUFFERS, FORMAT JSON) " + sql;
    StringBuilder jsonResult = new StringBuilder();

    try (Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery(explainSql)) {

      while (rs.next()) {
        jsonResult.append(rs.getString(1));
      }
      return new ExecutionPlan(jsonResult.toString(), "JSON");

    } catch (Exception e) {
      log.error("PostgreSQL EXPLAIN failed", e);
      // Return error as JSON so frontend can display it
      return new ExecutionPlan("{\"error\": \"" + e.getMessage() + "\"}", "JSON");
    }
  }

  @Override
  public List<DbaSuggestion> getSuggestions(Connection conn, String sql) {
    List<DbaSuggestion> suggestions = new ArrayList<>();
    suggestions.add(
        new DbaSuggestion("INFO", "Check for missing indexes using pg_stat_user_tables."));
    suggestions.add(new DbaSuggestion("TIP", "Run VACUUM ANALYZE if stats are outdated."));
    return suggestions;
  }

  @Override
  public boolean supportsAdvancedDiagnostics() {
    return false;
  }

  @Override
  public UnifiedExecutionNode normalize(String rawJson) {
    try {
      JsonNode rootArray = objectMapper.readTree(rawJson);
      if (rootArray.isArray() && rootArray.size() > 0) {
        JsonNode planNode = rootArray.get(0).get("Plan");
        return convertPgNode(planNode);
      }
    } catch (Exception e) {
      log.error("Failed to normalize PostgreSQL plan", e);
    }
    return null;
  }

  private UnifiedExecutionNode convertPgNode(JsonNode pgNode) {
    if (pgNode == null) return null;

    String operation = pgNode.path("Node Type").asText("Unknown");
    double cost = pgNode.path("Total Cost").asDouble(0.0);
    double rows = pgNode.path("Plan Rows").asDouble(0.0);
    double actualTime = pgNode.path("Actual Total Time").asDouble(0.0);

    List<UnifiedExecutionNode> children = new ArrayList<>();
    if (pgNode.has("Plans")) {
      for (JsonNode child : pgNode.get("Plans")) {
        children.add(convertPgNode(child));
      }
    }

    return new UnifiedExecutionNode(operation, cost, rows, actualTime, children);
  }
}
