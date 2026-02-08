/*
 * Copyright (c) 2026 Pollo Chang. All rights reserved.
 * This software is proprietary and confidential.
 * Unauthorized copying, via any medium, is strictly prohibited.
 */
package com.sqlconsole.core.impl;

import com.sqlconsole.core.report.DbaProvider;
import com.sqlconsole.core.report.DbaReport;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class PostgresDbaProvider implements DbaProvider {

  /**
   * 獲取 SQL 的執行計畫 (使用 EXPLAIN ANALYZE)
   *
   * @param connection 資料庫連線
   * @param sql 原始 SQL 指令
   * @return 標準化後的診斷報告
   */
  @Override
  public DbaReport getExecutionPlan(Connection connection, String sql) {
    StringBuilder plan = new StringBuilder();
    // OpenSource 僅限使用 EXPLAIN ANALYZE (PG 語法)
    String explainSql = "EXPLAIN (ANALYZE, BUFFERS, FORMAT TEXT) " + sql;

    try (Statement stmt = connection.createStatement();
        ResultSet rs = stmt.executeQuery(explainSql)) {

      while (rs.next()) {
        plan.append(rs.getString(1)).append("\n");
      }

      return new DbaReport(
          plan.toString(), List.of("基礎建議：請檢查是否漏掉 Index "), 0 // 暫不實作耗時解析
          );
    } catch (Exception e) {
      log.error("Failed to fetch Postgres Execution Plan", e);
      return new DbaReport("Error: " + e.getMessage(), List.of(), -1);
    }
  }

  /**
   * 獲取資料庫即時健康指標 (僅提供基礎狀態)
   *
   * @param connection 資料庫連線
   * @return 包含指標名稱與數值的列表
   */
  @Override
  public List<Map<String, Object>> getLiveDiagnostics(Connection connection) {
    // OS 版本僅提供 pg_stat_activity 的基礎計數
    return List.of(Map.of("Status", "Tier 1: Basic Diagnostics Only"));
  }

  /**
   * 判定是否支援 PostgreSQL
   *
   * @param dbType 資料庫類型標籤
   * @return true 若為 PostgreSQL
   */
  @Override
  public boolean supports(String dbType) {
    return "POSTGRESQL".equalsIgnoreCase(dbType);
  }
}
