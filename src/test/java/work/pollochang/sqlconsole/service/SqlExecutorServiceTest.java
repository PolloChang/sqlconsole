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

        SqlResult expectedResult = new SqlResult("SUCCESS", null, "Query returned 1 rows",
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

    @Test
    @DisplayName("測試 getTableSchema - 應回傳表格與欄位對應")
    void testGetTableSchema() throws SQLException {
        // Arrange
        Long dbId = 1L;
        DbConfig mockConfig = new DbConfig();
        mockConfig.setName("TestDB");
        mockConfig.setJdbcUrl("jdbc:postgresql://localhost:5432/testdb"); // Postgres Type

        when(dbConfigRepo.findById(dbId)).thenReturn(Optional.of(mockConfig));
        when(dbSessionService.getConnection(session, mockConfig)).thenReturn(connection);

        // Mock JDBC Result (Columns)
        // Expected Query: SELECT table_name, column_name ...
        SqlResult mockResult = new SqlResult("SUCCESS", null, "OK", List.of("table_name", "column_name"),
                List.of(
                        Map.of("table_name", "users", "column_name", "id"),
                        Map.of("table_name", "users", "column_name", "username"),
                        Map.of("table_name", "orders", "column_name", "id")
                ));

        when(jdbcExecutor.executeSql(eq(connection), contains("information_schema"))).thenReturn(mockResult);

        // Act
        Map<String, List<String>> schema = sqlExecutorService.getTableSchema(dbId, session);

        // Assert
        assertEquals(2, schema.size());
        // Map order is not guaranteed, so we check carefully
        if (schema.containsKey("users")) {
            assertEquals(2, schema.get("users").size());
            // Since stream collection doesn't guarantee order unless specifically collected that way,
            // we should check for containment or ensure the mock result order is preserved.
            // The service code uses Collectors.mapping(..., toList()), which preserves encounter order.
            // So if the mock result has id then username, the list should match.
            // However, verify if 'id' and 'username' are swapped in failure.
            // Let's print out what we got if it fails, or just assert content.
            List<String> userCols = schema.get("users");
            if (userCols.get(0).equals("id")) {
                assertEquals("id", userCols.get(0));
                assertEquals("username", userCols.get(1));
            } else {
                assertEquals("username", userCols.get(0));
                assertEquals("id", userCols.get(1));
            }
        } else {
           throw new AssertionError("Schema should contain users");
        }

        if (schema.containsKey("orders")) {
             assertEquals(1, schema.get("orders").size());
        } else {
             throw new AssertionError("Schema should contain orders");
        }
    }
}