package work.pollochang.sqlconsole.controller;

import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.ui.Model;
import work.pollochang.sqlconsole.model.dto.SqlResult;
import work.pollochang.sqlconsole.repository.DbConfigRepository;
import work.pollochang.sqlconsole.repository.UserRepository;
import work.pollochang.sqlconsole.service.AuditService;
import work.pollochang.sqlconsole.service.SqlExecutorService;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConsoleControllerTest {

    @InjectMocks private ConsoleController controller;

    @Mock private DbConfigRepository dbConfigRepo;
    @Mock private AuditService auditService;
    @Mock private SqlExecutorService sqlService;
    @Mock private UserRepository userRepo;

    @Mock private Model model;
    @Mock private Authentication auth;
    @Mock private HttpSession session;

    // Helper: 模擬 Authentication
    private void mockAuth(String name, String role) {
        when(auth.getName()).thenReturn(name);
        // 使用 doReturn 避免泛型轉換問題
        doReturn(List.of(new SimpleGrantedAuthority(role)))
                .when(auth).getAuthorities();
    }

    @Test
    void testIndex() {
        assertEquals("redirect:/console", controller.index());
    }

    @Test
    void testConsolePage_AsAuditor() {
        mockAuth("admin", "ROLE_AUDITOR");
        when(dbConfigRepo.findAll()).thenReturn(Collections.emptyList());
        when(auditService.getPendingTasks()).thenReturn(Collections.emptyList());

        String view = controller.consolePage(model, auth);

        assertEquals("console", view);
        verify(model).addAttribute("role", "ROLE_AUDITOR");
        verify(auditService).getPendingTasks(); // 只有 Auditor 會呼叫這個
    }

    @Test
    void testConsolePage_AsUser() {
        mockAuth("user", "ROLE_USER");
        when(dbConfigRepo.findAll()).thenReturn(Collections.emptyList());

        String view = controller.consolePage(model, auth);

        assertEquals("console", view);
        verify(model).addAttribute("role", "ROLE_USER");
        verify(auditService, never()).getPendingTasks(); // 一般人不會撈待審核任務
    }

    @Test
    void testExecute() {
        mockAuth("user", "ROLE_USER");
        SqlResult expected = new SqlResult("SUCCESS", "OK", null, null);
        when(sqlService.processRequest(eq(1L), eq("SELECT 1"), eq("user"), eq("ROLE_USER"), eq(session)))
                .thenReturn(expected);

        SqlResult result = controller.execute(1L, "SELECT 1", auth, session);

        assertEquals(expected, result);
    }

    @Test
    void testApprove_AsAuditor_Success() {
        mockAuth("admin", "ROLE_AUDITOR");
        SqlResult expected = new SqlResult("SUCCESS", "Task Executed", null, null);
        when(auditService.executeApprovedTask(100L, "admin")).thenReturn(expected);

        SqlResult result = controller.approve(100L, auth, session);

        assertEquals(expected, result);
    }

    @Test
    void testApprove_AsUser_Forbidden() {
//        mockAuth("user", "ROLE_USER");
        doReturn(List.of(new SimpleGrantedAuthority("ROLE_USER")))
                .when(auth).getAuthorities();

        SqlResult result = controller.approve(100L, auth, session);

        assertEquals("ERROR", result.status());
        assertEquals("無權限", result.message());
        verify(auditService, never()).executeApprovedTask(anyLong(), anyString());
    }
}