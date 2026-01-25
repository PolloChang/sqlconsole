package work.pollochang.sqlconsole.service;

import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import work.pollochang.sqlconsole.model.dto.SqlResult;
import work.pollochang.sqlconsole.model.entity.DbConfig;
import work.pollochang.sqlconsole.model.entity.SqlHistory;
import work.pollochang.sqlconsole.repository.DbConfigRepository;
import work.pollochang.sqlconsole.repository.SqlHistoryRepository;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SqlExecutorServiceTest {

    @InjectMocks private SqlExecutorService sqlExecutorService;

    @Mock private AuditService auditService;
    @Mock private DbConfigRepository dbConfigRepo;
    @Mock private SqlHistoryRepository historyRepo;
    @Mock private DbSessionService dbSessionService;
    @Mock private JdbcExecutor jdbcExecutor;

    @Mock private HttpSession session;
    @Mock private DbConfig dbConfig;
    @Mock private Connection connection;

    // --- 測試基本流程與 TCL ---

    @Test
    void testProcessRequest_DbNotFound() {
        when(dbConfigRepo.findById(1L)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () ->
                sqlExecutorService.processRequest(1L, "SELECT 1", "user", "ROLE_USER", session));
    }

    @Test
    void testProcessRequest_Commit() throws SQLException {
        when(dbConfigRepo.findById(1L)).thenReturn(Optional.of(dbConfig));
        when(dbSessionService.getConnection(session, dbConfig)).thenReturn(connection);

        SqlResult result = sqlExecutorService.processRequest(1L, "COMMIT", "user", "ROLE_USER", session);

        assertEquals("SUCCESS", result.status());
        verify(connection).commit();
    }

    @Test
    void testProcessRequest_Rollback_Error() throws SQLException {
        // 測試 TCL 執行發生異常
        when(dbConfigRepo.findById(1L)).thenReturn(Optional.of(dbConfig));
        when(dbSessionService.getConnection(session, dbConfig)).thenReturn(connection);
        doThrow(new SQLException("DB Error")).when(connection).rollback();

        SqlResult result = sqlExecutorService.processRequest(1L, "ROLLBACK", "user", "ROLE_USER", session);

        assertEquals("ERROR", result.status());
    }

    // --- 測試稽核與敏感操作 ---

    @Test
    void testProcessRequest_RestrictedSql_NeedAudit() throws SQLException { // ✅ 這裡補上 throws SQLException
        when(dbConfigRepo.findById(1L)).thenReturn(Optional.of(dbConfig));
        // 模擬 AuditService 攔截並回傳 PENDING
        when(auditService.checkAndAudit(anyString(), anyLong(), anyString()))
                .thenReturn(new SqlResult("PENDING", "Wait for audit", null, null));

        SqlResult result = sqlExecutorService.processRequest(1L, "DROP TABLE users", "user", "ROLE_USER", session);

        assertEquals("PENDING", result.status());
        verify(jdbcExecutor, never()).executeSql(any(), any()); // 確保沒執行 SQL
    }

    @Test
    void testProcessRequest_RestrictedSql_AuditorBypass() throws SQLException {
        // Auditor 執行敏感指令不需稽核
        when(dbConfigRepo.findById(1L)).thenReturn(Optional.of(dbConfig));
        when(dbSessionService.getConnection(session, dbConfig)).thenReturn(connection);
        when(jdbcExecutor.executeSql(any(), anyString()))
                .thenReturn(new SqlResult("SUCCESS", "Dropped", null, null));

        SqlResult result = sqlExecutorService.processRequest(1L, "DROP TABLE users", "admin", "ROLE_AUDITOR", session);

        assertEquals("SUCCESS", result.status());
        verify(auditService, never()).checkAndAudit(any(), any(), any());
    }

    // --- 測試 executeRawSql 核心邏輯 ---

    @Test
    void testExecuteRawSql_Success_WithAutoCommit() throws SQLException {
        // 測試執行成功且觸發自動 Commit (模擬審核通過後的執行)
        String sql = "INSERT INTO ...";
        when(dbSessionService.getConnection(session, dbConfig)).thenReturn(connection);
        when(connection.getAutoCommit()).thenReturn(false); // 手動模式

        SqlResult mockResult = new SqlResult("SUCCESS", "Inserted", null, null);
        when(jdbcExecutor.executeSql(connection, sql)).thenReturn(mockResult);

        // Act (autoCommitAfterExec = true)
        SqlResult result = sqlExecutorService.executeRawSql(session, dbConfig, sql, "user", true);

        // Assert
        verify(connection).commit(); // 驗證有自動 commit
        assertTrue(result.message().contains("Auto Committed"));
        verify(historyRepo).save(any(SqlHistory.class));
    }

    @Test
    void testExecuteRawSql_Exception_WithRollback() throws SQLException {
        // 測試執行 SQL 失敗，觸發 Rollback
        String sql = "BAD SQL";
        when(dbSessionService.getConnection(session, dbConfig)).thenReturn(connection);
        when(connection.getAutoCommit()).thenReturn(false);

        // 模擬 JDBC 拋錯
        when(jdbcExecutor.executeSql(connection, sql)).thenThrow(new SQLException("Syntax Error"));

        // Act
        SqlResult result = sqlExecutorService.executeRawSql(session, dbConfig, sql, "user", false);

        // Assert
        assertEquals("ERROR", result.status());
        assertTrue(result.message().contains("Syntax Error"));
        assertTrue(result.message().contains("Transaction rolled back"));
        verify(connection).rollback(); // 驗證有 rollback
        verify(historyRepo).save(any(SqlHistory.class));
    }

    @Test
    void testExecuteRawSql_Exception_RollbackFailed() throws SQLException {
        // 覆蓋 catch(SQLException ex) inside catch block (Rollback 也失敗)
        String sql = "BAD SQL";
        when(dbSessionService.getConnection(session, dbConfig)).thenReturn(connection);
        when(connection.getAutoCommit()).thenReturn(false);

        when(jdbcExecutor.executeSql(connection, sql)).thenThrow(new SQLException("Syntax Error"));
        doThrow(new SQLException("Rollback Fail")).when(connection).rollback(); // Rollback 也爆

        SqlResult result = sqlExecutorService.executeRawSql(session, dbConfig, sql, "user", false);

        assertEquals("ERROR", result.status());
        // 確保程式沒有 Crash，且有紀錄 History
        verify(historyRepo).save(any(SqlHistory.class));
    }
}