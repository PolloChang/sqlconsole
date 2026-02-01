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

/** MariaDB implementation of VirtualDbaProvider. */
@Slf4j
@Component
public class MariaDbDbaProvider implements VirtualDbaProvider {

  @Override
  public String getProviderType() {
    return "MARIADB";
  }

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

  @Override
  public List<DbaSuggestion> getSuggestions(Connection conn, String sql) {
    List<DbaSuggestion> suggestions = new ArrayList<>();
    suggestions.add(new DbaSuggestion("INFO", "Use 'SHOW WARNINGS' to see optimizer notes."));
    return suggestions;
  }
}
