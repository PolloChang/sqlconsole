/*
 * Copyright (c) 2026 Pollo Chang. All rights reserved.
 * This software is proprietary and confidential.
 * Unauthorized copying, via any medium, is strictly prohibited.
 */
package com.sqlconsole.core.controller;

import com.sqlconsole.core.model.dto.AnalyzeRequest;
import com.sqlconsole.core.model.entity.DbConfig;
import com.sqlconsole.core.report.DbaReport;
import com.sqlconsole.core.repository.DbConfigRepository;
import com.sqlconsole.core.service.DbSessionService;
import com.sqlconsole.core.service.SqlExecutorService;
import jakarta.servlet.http.HttpSession;
import java.sql.Connection;
import java.sql.SQLException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/sql")
@RequiredArgsConstructor
/**
 * Controller for SQL execution.
 */
public class SqlController {


  private final SqlExecutorService sqlExecutorService;
  private final DbSessionService dbSessionService;
  private final DbConfigRepository dbConfigRepository;

  /**
   * Analyzes a SQL query and returns an execution plan.
   *
   * @param request the analysis request containing DB ID and SQL
   * @param session the HTTP session
   * @return the DBA report containing the execution plan
   * @throws SQLException if a database error occurs
   */
  @PostMapping("/analyze")
  public DbaReport analyze(@RequestBody AnalyzeRequest request, HttpSession session)
      throws SQLException {
    // Find DB Config
    DbConfig config =
        dbConfigRepository
            .findById(request.dbId())
            .orElseThrow(() -> new IllegalArgumentException("Invalid DB ID"));

    // Get Connection (reusing session logic)
    Connection conn = dbSessionService.getConnection(session, config);

    // Execute Analyze
    return sqlExecutorService.getExplainPlan(conn, config, request.sql());
  }
}
