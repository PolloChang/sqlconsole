package com.sqlconsole.core.core.vdba.impl;

import com.sqlconsole.core.core.vdba.DbaSuggestion;
import com.sqlconsole.core.core.vdba.ExecutionPlan;
import com.sqlconsole.core.core.vdba.VirtualDbaProvider;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/** PostgreSQL implementation of VirtualDbaProvider. */
@Slf4j
@Component
public class PostgresqlDbaProvider implements VirtualDbaProvider {

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
}
