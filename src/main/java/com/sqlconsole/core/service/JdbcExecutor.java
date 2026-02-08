package com.sqlconsole.core.service;

import com.sqlconsole.core.model.dto.SqlResult;
import java.sql.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import org.springframework.stereotype.Component;

/** 負責單純的 JDBC 執行與結果集轉換。 讓 Service 層專注於流程控制，而非 JDBC API 細節。 */
@Component
public class JdbcExecutor {

  /**
   * 串流式查詢資料庫 (適用於大數據量匯出)
   *
   * @param conn 資料庫連線
   * @param sql SQL 指令
   * @param consumer ResultSet 消費者
   * @throws SQLException 若 SQL 執行失敗
   */
  public void streamQuery(Connection conn, String sql, Consumer<ResultSet> consumer)
      throws SQLException {
    String executableSql = sql.trim();
    if (executableSql.endsWith(";")) {
      executableSql = executableSql.substring(0, executableSql.length() - 1);
    }

    try (Statement stmt = conn.createStatement()) {
      stmt.setFetchSize(1000); // Enable streaming
      try (ResultSet rs = stmt.executeQuery(executableSql)) {
        consumer.accept(rs);
      }
    }
  }

  /**
   * 執行 SQL 指令 (無限制)
   *
   * @param conn 資料庫連線
   * @param sql SQL 指令
   * @return 執行結果
   * @throws SQLException 若 SQL 執行失敗
   */
  public SqlResult executeSql(Connection conn, String sql) throws SQLException {
    return executeSql(conn, sql, 0, 0);
  }

  /**
   * 執行 SQL 指令 (限制回傳筆數)
   *
   * @param conn 資料庫連線
   * @param sql SQL 指令
   * @param maxRows 最大回傳筆數
   * @return 執行結果
   * @throws SQLException 若 SQL 執行失敗
   */
  public SqlResult executeSql(Connection conn, String sql, int maxRows) throws SQLException {
    return executeSql(conn, sql, maxRows, 0);
  }

  /**
   * 執行 SQL 指令 (分頁)
   *
   * @param conn 資料庫連線
   * @param sql SQL 指令
   * @param limit 每頁筆數
   * @param offset 偏移量
   * @return 執行結果
   * @throws SQLException 若 SQL 執行失敗
   */
  public SqlResult executeSql(Connection conn, String sql, int limit, int offset)
      throws SQLException {
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
