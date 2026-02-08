package com.sqlconsole.core.service.impl;

import com.sqlconsole.core.model.dto.SqlResult;
import com.sqlconsole.core.service.AuditService;
import java.util.List;
import org.springframework.stereotype.Service;


@Service
/**
 * Free implementation of AuditService (no-op).
 */
public class FreeAuditService implements AuditService {

  /**
   * 檢查並審核 SQL 指令
   *
   * @param username 使用者名稱
   * @param dbId 資料庫 ID
   * @param sql SQL 指令
   * @return 審核結果，免費版永遠回傳 null (代表 Pass，直接執行)
   */
  @Override
  public SqlResult checkAndAudit(String username, Long dbId, String sql) {
    // 免費版沒有審核功能，永遠回傳 null (代表 Pass，直接執行)
    return null;
  }

  /**
   * 執行已核准的審核工單
   *
   * @param taskId 工單 ID
   * @param auditorName 審核者名稱
   * @return 執行結果
   */
  @Override
  public SqlResult executeApprovedTask(Long taskId, String auditorName) {
    return new SqlResult("ERROR", "COMMITTED", "此功能僅限企業版使用", null, null);
  }

  /**
   * 獲取待審核工單列表
   *
   * @return 工單列表
   */
  @Override
  public List<?> getPendingTasks() {
    // 免費版沒有審核功能，回傳空清單
    return java.util.Collections.emptyList();
  }
}
