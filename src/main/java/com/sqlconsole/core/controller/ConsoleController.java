package com.sqlconsole.core.controller;

import com.sqlconsole.core.model.dto.SqlResult;
import com.sqlconsole.core.repository.DbConfigRepository;
import com.sqlconsole.core.repository.UserRepository;
import com.sqlconsole.core.service.AuditService;
import com.sqlconsole.core.service.DbConfigService;
import com.sqlconsole.core.service.SqlExecutorService;
import jakarta.servlet.http.HttpSession;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;


/**
 * Controller for SQL Console.
 */
@Controller
public class ConsoleController {

  @Autowired private DbConfigRepository dbConfigRepo;
  @Autowired private DbConfigService dbConfigService;
  @Autowired private AuditService auditService; // ✅ 新增：改用介面
  @Autowired private SqlExecutorService sqlService;
  @Autowired private UserRepository userRepo;

  /**
   * 根路徑重導向至 Console 頁面
   *
   * @return 重導向路徑
   */
  @GetMapping("/")
  public String index() {
    return "redirect:/console";
  }

  /**
   * 提供 SQL Console 主頁面
   *
   * @param model UI 模型
   * @param auth 認證資訊
   * @return 視圖名稱 "console"
   */
  @GetMapping("/console")
  public String consolePage(Model model, Authentication auth) {
    model.addAttribute("dbs", dbConfigService.getAllConfigs());
    model.addAttribute("username", auth.getName());

    String role = auth.getAuthorities().stream().findFirst().get().getAuthority();
    model.addAttribute("role", role);

    if (role.equals("ROLE_AUDITOR")) {
      model.addAttribute("tasks", auditService.getPendingTasks());
    }
    return "console";
  }

  /**
   * 執行 SQL 指令
   *
   * @param dbId 資料庫 ID
   * @param sql SQL 指令
   * @param page 頁碼 (預設 1)
   * @param size 每頁筆數 (預設 100)
   * @param auth 認證資訊
   * @param session HTTP Session
   * @return SQL 執行結果
   */
  @PostMapping("/api/execute")
  @ResponseBody
  public SqlResult execute(
      @RequestParam Long dbId,
      @RequestParam String sql,
      @RequestParam(defaultValue = "1") int page,
      @RequestParam(defaultValue = "100") int size,
      Authentication auth,
      HttpSession session) {
    String role = auth.getAuthorities().stream().findFirst().get().getAuthority();
    return sqlService.processRequest(dbId, sql, auth.getName(), role, session, page, size);
  }

  /**
   * 審核並執行工單
   *
   * @param taskId 工單 ID
   * @param auth 認證資訊
   * @param session HTTP Session
   * @return 執行結果
   */
  @PostMapping("/api/approve")
  @ResponseBody
  public SqlResult approve(@RequestParam Long taskId, Authentication auth, HttpSession session) {
    String role = auth.getAuthorities().stream().findFirst().get().getAuthority();
    if (!role.equals("ROLE_AUDITOR")) {
      return new SqlResult("ERROR", "COMMITTED", "無權限", null, null);
    }
    // 因為具體的「撈工單 -> 執行」邏輯現在在 Premium 專案裡
    return auditService.executeApprovedTask(taskId, auth.getName());
  }

  /**
   * 獲取資料庫表格綱要
   *
   * @param dbId 資料庫 ID
   * @param session HTTP Session
   * @param auth 認證資訊
   * @return 表格與欄位的對應 Map
   */
  @GetMapping("/api/schema")
  @ResponseBody
  public Map<String, List<String>> getSchema(
      @RequestParam Long dbId, HttpSession session, Authentication auth) {
    return sqlService.getTableSchema(dbId, session, auth);
  }
}
