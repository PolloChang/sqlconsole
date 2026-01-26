package work.pollochang.sqlconsole.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import work.pollochang.sqlconsole.model.dto.SqlResult;
import work.pollochang.sqlconsole.repository.DbConfigRepository;
import work.pollochang.sqlconsole.repository.UserRepository;
import work.pollochang.sqlconsole.service.AuditService;
import work.pollochang.sqlconsole.service.AuthService;
import work.pollochang.sqlconsole.service.DbConfigService;
import work.pollochang.sqlconsole.service.SqlExecutorService;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ConsoleController.class)
class ConsoleControllerTest {

    @Autowired
    private MockMvc mockMvc;

    // ✅ 這裡必須補齊 ConsoleController 所需的所有依賴
    @MockitoBean private DbConfigRepository dbConfigRepo;
    @MockitoBean private AuditService auditService;
    @MockitoBean private SqlExecutorService sqlService;
    @MockitoBean private UserRepository userRepo;
    @MockitoBean private AuthService authService;
    //因為 AuthService 被 Mock 了，導致 PasswordEncoder 消失，必須手動補回來
    @MockitoBean private PasswordEncoder passwordEncoder;
    @MockitoBean private DbConfigService dbConfigService;

    @Test
    @WithMockUser(username = "admin", roles = "AUDITOR")
    @DisplayName("進入 Console 頁面 - 稽核員應看到任務列表")
    void testConsolePage_AsAuditor() throws Exception {
        // 這裡不需要特別 Mock 回傳值，因為 Model 屬性是 null 也不會導致頁面崩潰 (除非 thymeleaf 有強力檢查)
        // 為了保險，可以加一點 Mock
        // when(auditService.getPendingTasks()).thenReturn(List.of());

        mockMvc.perform(get("/console"))
                .andExpect(status().isOk())
                .andExpect(view().name("console"))
                .andExpect(model().attributeExists("dbs"))
                .andExpect(model().attributeExists("username"))
                .andExpect(model().attributeExists("tasks")); // Auditor 特有
    }

    @Test
    @WithMockUser(username = "user", roles = "USER")
    @DisplayName("執行 SQL API - 成功")
    void testExecuteApi() throws Exception {
        // Arrange: 模擬 Service 回傳成功結果
        SqlResult mockResult = new SqlResult("SUCCESS", "UNCOMMIT", "OK", null, null);
        when(sqlService.processRequest(any(), any(), any(), any(), any()))
                .thenReturn(mockResult);

        // Act & Assert
        mockMvc.perform(post("/api/execute")
                        .param("dbId", "1")
                        .param("sql", "SELECT 1")
                        .with(csrf())) // POST 需要 CSRF Token
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"));
    }

    @Test
    @WithMockUser(username = "user", roles = "USER")
    @DisplayName("審核 API - 一般使用者應無權限")
    void testApproveApi_AsUser_ShouldFail() throws Exception {
        mockMvc.perform(post("/api/approve")
                        .param("taskId", "1")
                        .with(csrf()))
                .andExpect(status().isOk()) // 因為 Controller 有 try-catch 或回傳 JSON 錯誤物件
                .andExpect(jsonPath("$.status").value("ERROR"))
                .andExpect(jsonPath("$.message").value("無權限"));
    }
}