package work.pollochang.sqlconsole.service;

import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import work.pollochang.sqlconsole.model.dto.SqlResult;
import work.pollochang.sqlconsole.model.entity.DbConfig;
import work.pollochang.sqlconsole.repository.DbConfigRepository;
import work.pollochang.sqlconsole.repository.SqlHistoryRepository;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@Slf4j
@ExtendWith(MockitoExtension.class)
class SqlExecutorServiceTest {

    @InjectMocks private SqlExecutorService sqlExecutorService;

    @Mock private AuditService auditService;
    @Mock private DbConfigRepository dbConfigRepo;
    @Mock private SqlHistoryRepository historyRepo;
    @Mock private DbSessionService dbSessionService;
    @Mock private JdbcExecutor jdbcExecutor;

    @Mock private HttpSession session;
    @Mock private Connection connection;

    @Test
    @DisplayName("測試 SELECT 查詢 - 使用 JdbcExecutor Mock")
    void testProcessRequest_Select() throws SQLException {
        // Arrange
        Long dbId = 1L;
        String sql = "SELECT * FROM users";
        DbConfig mockConfig = new DbConfig();
        mockConfig.setName("TestDB");

        when(dbConfigRepo.findById(dbId)).thenReturn(Optional.of(mockConfig));
        when(dbSessionService.getConnection(session, mockConfig)).thenReturn(connection);

        SqlResult expectedResult = new SqlResult("SUCCESS", "Query returned 1 rows",
                List.of("id"),
                List.of(Map.of("id", 100)));

        when(jdbcExecutor.executeSql(connection, sql)).thenReturn(expectedResult);

        // Act
        SqlResult result = sqlExecutorService.processRequest(dbId, sql, "user1", "ROLE_USER", session);
        log.error("Test Result Status: " + result.status());
        log.error("Test Result Message: " + result.message());

        // Assert
        // 🔴 修正點：移除 get 前綴
        assertEquals("SUCCESS", result.status());
        assertEquals(1, result.rows().size());
        assertEquals(100, result.rows().get(0).get("id"));

        verify(jdbcExecutor).executeSql(connection, sql);
        verify(historyRepo).save(any());
    }

    @Test
    @DisplayName("測試 SQL 執行錯誤 - 應處理 Exception 並 Rollback")
    void testProcessRequest_ErrorHandling() throws SQLException {
        // Arrange
        Long dbId = 1L;
        String sql = "BAD SQL";
        DbConfig mockConfig = new DbConfig();

        when(dbConfigRepo.findById(dbId)).thenReturn(Optional.of(mockConfig));
        when(dbSessionService.getConnection(session, mockConfig)).thenReturn(connection);

        when(jdbcExecutor.executeSql(connection, sql)).thenThrow(new SQLException("Syntax Error"));
        when(connection.getAutoCommit()).thenReturn(false);

        // Act
        SqlResult result = sqlExecutorService.processRequest(dbId, sql, "user1", "ROLE_USER", session);
        log.error("Actual Error Message: " + result.message());

        // Debug: 如果測試失敗，把 result 印出來看看是什麼

        // Assert
        // 🔴 修正點：移除 get 前綴
        if (result.message().contains("Syntax Error")) {
            // 通過
        } else {
            // 失敗時顯示清楚的訊息
            assertEquals("Syntax Error", result.message());
        }


        verify(connection).rollback();
    }
}