package com.sqlconsole.core.service;

import java.sql.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
import com.sqlconsole.core.model.dto.SqlResult;

/** 負責單純的 JDBC 執行與結果集轉換。 讓 Service 層專注於流程控制，而非 JDBC API 細節。 */
@Component
public class JdbcExecutor {

  public SqlResult executeSql(Connection conn, String sql) throws SQLException {
    return executeSql(conn, sql, 0, 0);
  }

  public SqlResult executeSql(Connection conn, String sql, int maxRows) throws SQLException {
    return executeSql(conn, sql, maxRows, 0);
  }

  public SqlResult executeSql(Connection conn, String sql, int limit, int offset) throws SQLException {
    String status = "SUCCESS";
    String msg;
    List<String> columns = new ArrayList<>();
    List<Map<String, Object>> rows = new ArrayList<>();

    // 去除結尾分號
    String executableSql = sql.trim();
    if (executableSql.endsWith(";")) {
      executableSql = executableSql.substring(0, executableSql.length() - 1);
    }

    try (Statement stmt = conn.createStatement()) {
      if (limit > 0) {
        // Optimize: setMaxRows to limit + offset to stop DB from fetching everything
        stmt.setMaxRows(limit + offset);
      }
      boolean hasResultSet = stmt.execute(executableSql);
      if (hasResultSet) {
        try (ResultSet rs = stmt.getResultSet()) {
          ResultSetMetaData meta = rs.getMetaData();
          int colCount = meta.getColumnCount();
          for (int i = 1; i <= colCount; i++) columns.add(meta.getColumnLabel(i));

          // Skip Offset logic
          if (offset > 0) {
            // Try absolute positioning first if supported
            try {
              if (rs.getType() != ResultSet.TYPE_FORWARD_ONLY) {
                rs.absolute(offset);
              } else {
                for (int i = 0; i < offset; i++) {
                  if (!rs.next()) break;
                }
              }
            } catch (SQLException e) {
              // Fallback to simple loop
              for (int i = 0; i < offset; i++) {
                if (!rs.next()) break;
              }
            }
          }

          // Fetch Limit rows
          int count = 0;
          while ((limit <= 0 || count < limit) && rs.next()) {
            Map<String, Object> row = new LinkedHashMap<>();
            for (String col : columns) row.put(col, rs.getObject(col));
            rows.add(row);
            count++;
          }
          msg = "Query returned " + rows.size() + " rows.";
        }
      } else {
        msg = "Affected rows: " + stmt.getUpdateCount();
      }
    }
    return new SqlResult(status, null, msg, columns, rows);
  }
}
