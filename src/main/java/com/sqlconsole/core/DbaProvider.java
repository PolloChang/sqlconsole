/*
 * Copyright (c) 2026 Pollo Chang. All rights reserved.
 * This software is proprietary and confidential.
 * Unauthorized copying, via any medium, is strictly prohibited.
 */
package com.sqlconsole.core;

import java.sql.Connection;
import java.util.List;
import java.util.Map;

/**
 * 虛擬 DBA 供應商介面 (Requirement 13)
 * 負責執行資料庫診斷、分析執行計畫與提供優化建議。
 */
public interface DbaProvider {

    /**
     * 獲取 SQL 的執行計畫 (Execution Plan)
     * @param connection 資料庫連線
     * @param sql 原始 SQL 指令
     * @return 標準化後的診斷報告
     */
    DbaReport getExecutionPlan(Connection connection, String sql);

    /**
     * 獲取資料庫即時健康指標 (Diagnostics)
     * @param connection 資料庫連線
     * @return 包含指標名稱與數值的列表
     */
    List<Map<String, Object>> getLiveDiagnostics(Connection connection);

    /**
     * 判定此 Provider 是否支援當前的資料庫類型
     * @param dbType 資料庫類型標籤 (如 "POSTGRESQL", "ORACLE")
     * @return true 若支援
     */
    boolean supports(String dbType);
}