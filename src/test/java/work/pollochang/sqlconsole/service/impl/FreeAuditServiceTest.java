package work.pollochang.sqlconsole.service.impl;

import org.junit.jupiter.api.Test;
import work.pollochang.sqlconsole.model.dto.SqlResult;

import static org.junit.jupiter.api.Assertions.*;

class FreeAuditServiceTest {

    private final FreeAuditService freeAuditService = new FreeAuditService();

    @Test
    void testCheckAndAudit() {
        SqlResult result = freeAuditService.checkAndAudit("user", 1L, "DROP TABLE");
        assertNull(result, "Free version should assume pass (return null)");
    }

    @Test
    void testExecuteApprovedTask() {
        SqlResult result = freeAuditService.executeApprovedTask(1L, "auditor");
        assertEquals("ERROR", result.status());
        assertEquals("此功能僅限企業版使用", result.message());
    }

    @Test
    void testGetPendingTasks() {
        assertTrue(freeAuditService.getPendingTasks().isEmpty());
    }
}