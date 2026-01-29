/*
 * Copyright (c) 2026 Pollo Chang. All rights reserved.
 * This software is proprietary and confidential.
 * Unauthorized copying, via any medium, is strictly prohibited.
 */
package work.pollochang.sqlconsole.core.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import work.pollochang.sqlconsole.core.DbaProvider;
import work.pollochang.sqlconsole.core.DbaReport;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class PostgresDbaProvider implements DbaProvider {

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
                    plan.toString(),
                    List.of("基礎建議：請檢查是否漏掉 Index "),
                    0 // 暫不實作耗時解析
            );
        } catch (Exception e) {
            log.error("Failed to fetch Postgres Execution Plan", e);
            return new DbaReport("Error: " + e.getMessage(), List.of(), -1);
        }
    }

    @Override
    public List<Map<String, Object>> getLiveDiagnostics(Connection connection) {
        // OS 版本僅提供 pg_stat_activity 的基礎計數
        return List.of(Map.of("Status", "Tier 1: Basic Diagnostics Only"));
    }

    @Override
    public boolean supports(String dbType) {
        return "POSTGRESQL".equalsIgnoreCase(dbType);
    }
}