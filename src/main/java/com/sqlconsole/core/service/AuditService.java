package com.sqlconsole.core.service;

import com.sqlconsole.core.model.dto.SqlResult;
import java.util.List;

public interface AuditService {
  /**
   * 檢查 SQL 是否需要審核
   *
   * @param username 使用者名稱
   * @param dbId 資料庫 ID
   * @param sql SQL 指令
   * @return null 代表不需要審核 (直接通過)，回傳 SqlResult 代表被攔截 (PENDING)
   */
  SqlResult checkAndAudit(String username, Long dbId, String sql);

  /**
   * 執行被批准的工單 (給 Controller 呼叫)
   *
   * @param taskId 工單 ID
   * @param auditorName 審核者名稱
   * @return 執行結果
   */
  SqlResult executeApprovedTask(Long taskId, String auditorName);

  /**
   * 獲取待審核工單列表
   *
   * @return 工單列表
   */
  List<?> getPendingTasks();
}
