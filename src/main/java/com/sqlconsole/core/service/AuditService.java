package com.sqlconsole.core.service;

import java.util.List;
import com.sqlconsole.core.model.dto.SqlResult;

public interface AuditService {
  /**
   * 檢查 SQL 是否需要審核
   *
   * @return null 代表不需要審核 (直接通過)，回傳 SqlResult 代表被攔截 (PENDING)
   */
  SqlResult checkAndAudit(String username, Long dbId, String sql);

  /** 執行被批准的工單 (給 Controller 呼叫) */
  SqlResult executeApprovedTask(Long taskId, String auditorName);

  List<?> getPendingTasks();
}
