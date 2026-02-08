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

@Slf4j
@Component
/**
 * MariaDB implementation of Virtual DBA Provider.
 */
public class MariaDbDbaProvider implements VirtualDbaProvider {


  private final ObjectMapper objectMapper = new ObjectMapper();

  @Override
  public String getProviderType() {
    return "MARIADB";
  }

  /**
   * Generates an execution plan for MariaDB.
   *
   * @param conn the database connection
   * @param sql the SQL query
   * @return the execution plan
   */
  @Override
  public ExecutionPlan explain(Connection conn, String sql) {
    String explainSql = "EXPLAIN FORMAT=JSON " + sql;
    StringBuilder jsonResult = new StringBuilder();

    try (Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery(explainSql)) {

      while (rs.next()) {
        jsonResult.append(rs.getString(1));
      }
      return new ExecutionPlan(jsonResult.toString(), "JSON");

    } catch (Exception e) {
      log.error("MariaDB EXPLAIN failed", e);
      return new ExecutionPlan("{\"error\": \"" + e.getMessage() + "\"}", "JSON");
    }
  }

  /**
   * Generates optimization suggestions for MariaDB.
   *
   * @param conn the database connection
   * @param sql the SQL query
   * @param plan the execution plan
   * @return a list of suggestions
   */
  @Override
  public List<DbaSuggestion> getSuggestions(
      Connection conn, String sql, UnifiedExecutionNode plan) {
    List<DbaSuggestion> suggestions = new ArrayList<>();
    suggestions.add(new DbaSuggestion("INFO", "Use 'SHOW WARNINGS' to see optimizer notes."));
    return suggestions;
  }

  @Override
  public boolean supportsAdvancedDiagnostics() {
    return false;
  }

  /**
   * Normalizes the MariaDB JSON execution plan.
   *
   * @param rawJson the raw JSON plan
   * @return the normalized execution node
   */
  @Override
  public UnifiedExecutionNode normalize(String rawJson) {
    try {
      JsonNode root = objectMapper.readTree(rawJson);
      return convertMariaNode(root.path("query_block"));
    } catch (Exception e) {
      log.error("Failed to normalize MariaDB plan", e);
    }
    return null;
  }

  /**
   * Recursively converts a MariaDB JSON node to a UnifiedExecutionNode.
   *
   * @param node the JSON node
   * @return the unified execution node
   */
  private UnifiedExecutionNode convertMariaNode(JsonNode node) {
    if (node == null || node.isMissingNode()) return null;

    String operation = "SELECT";
    double cost = 0.0;
    double rows = 0.0;
    List<UnifiedExecutionNode> children = new ArrayList<>();

    if (node.has("table")) {
      JsonNode table = node.get("table");
      operation =
          "Table: "
              + table.path("table_name").asText()
              + " ("
              + table.path("access_type").asText()
              + ")";
      rows = table.path("rows").asDouble(0.0);
      if (table.has("cost_info")) {
        cost = table.get("cost_info").path("read_cost").asDouble(0.0); // Or eval_cost
      }
    } else if (node.has("nested_loop")) {
      operation = "NESTED LOOP";
      for (JsonNode child : node.get("nested_loop")) {
        children.add(convertMariaNode(child.get("table"))); // Simplification
      }
    } else {
      // Very basic handling for other structures
      operation = "UNKNOWN";
    }

    if (node.has("cost_info")) {
      cost = node.get("cost_info").path("query_cost").asDouble(0.0);
    }

    return new UnifiedExecutionNode(operation, cost, rows, 0.0, children, Collections.emptyMap());
  }
}
