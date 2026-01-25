package work.pollochang.sqlconsole.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import work.pollochang.sqlconsole.model.dto.SqlResult;
import work.pollochang.sqlconsole.repository.DbConfigRepository;
import work.pollochang.sqlconsole.repository.UserRepository;
import work.pollochang.sqlconsole.service.AuditService;
import work.pollochang.sqlconsole.service.SqlExecutorService;

import java.util.List;

@Controller
public class ConsoleController {

    @Autowired private DbConfigRepository dbConfigRepo;
    @Autowired private AuditService auditService; // ✅ 新增：改用介面
    @Autowired private SqlExecutorService sqlService;
    @Autowired private UserRepository userRepo;

    @GetMapping("/")
    public String index() {
        return "redirect:/console";
    }

    @GetMapping("/console")
    public String consolePage(Model model, Authentication auth) {
        model.addAttribute("dbs", dbConfigRepo.findAll());
        model.addAttribute("username", auth.getName());

        String role = auth.getAuthorities().stream().findFirst().get().getAuthority();
        model.addAttribute("role", role);

        if (role.equals("ROLE_AUDITOR")) {
            model.addAttribute("tasks", auditService.getPendingTasks());
        }
        return "console";
    }

    @PostMapping("/api/execute")
    @ResponseBody
    public SqlResult execute(@RequestParam Long dbId, @RequestParam String sql,
                             Authentication auth, HttpSession session) {
        String role = auth.getAuthorities().stream().findFirst().get().getAuthority();
        return sqlService.processRequest(dbId, sql, auth.getName(), role, session);
    }

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

    @GetMapping("/api/tables")
    @ResponseBody
    public List<String> getTables(@RequestParam Long dbId, HttpSession session) {
        return sqlService.getTableNames(dbId, session);
    }
}