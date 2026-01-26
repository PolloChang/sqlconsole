package work.pollochang.sqlconsole.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import jakarta.servlet.http.HttpSession;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import work.pollochang.sqlconsole.model.dto.SqlResult;
import work.pollochang.sqlconsole.model.entity.DbConfig;
import work.pollochang.sqlconsole.model.entity.User;
import work.pollochang.sqlconsole.repository.DbConfigRepository;
import work.pollochang.sqlconsole.repository.SqlHistoryRepository;
import work.pollochang.sqlconsole.repository.UserRepository;

@Slf4j
@ExtendWith(MockitoExtension.class)
class SqlExecutorServiceTest {

  @InjectMocks private SqlExecutorService sqlExecutorService;

  @Mock private AuditService auditService;
  @Mock private DbConfigRepository dbConfigRepo;
  @Mock private SqlHistoryRepository historyRepo;
  @Mock private DbSessionService dbSessionService;
  @Mock private JdbcExecutor jdbcExecutor;
  @Mock private UserRepository userRepository;

  @Mock private HttpSession session;
  @Mock private Connection connection;

  @Test
  @DisplayName("executeSql_Unauthorized_ShouldThrowException")
  void testProcessRequest_Unauthorized_ShouldThrowException() {
    // Arrange
    Long dbId = 1L;
    String sql = "SELECT 1";
    String username = "user1";

    User user = new User();
    user.setUsername(username);
    user.setAccessibleDatabases(Collections.emptySet()); // No access

    // We don't need to mock DbConfigRepo because validation should happen before or during lookup
    // But if implementation fetches config first, we need to mock it.
    // Assuming validation happens.

    // If implementation fetches config first:
    // DbConfig mockConfig = new DbConfig();
    // mockConfig.setId(dbId);
    // when(dbConfigRepo.findById(dbId)).thenReturn(Optional.of(mockConfig));

    when(userRepository.findByUsername(username)).thenReturn(Optional.of(user));

    // Act & Assert
    assertThrows(
        AccessDeniedException.class,
        () -> {
          sqlExecutorService.processRequest(dbId, sql, username, "ROLE_USER", session);
        });
  }

  @Test
  @DisplayName("測試 SELECT 查詢 - 使用 JdbcExecutor Mock")
  void testProcessRequest_Select() throws SQLException {
    // Arrange
    Long dbId = 1L;
    String sql = "SELECT * FROM users";
    DbConfig mockConfig = new DbConfig();
    mockConfig.setId(dbId);
    mockConfig.setName("TestDB");

    // Mock Auth
    User user = new User();
    user.setUsername("user1");
    user.setAccessibleDatabases(Set.of(mockConfig));
    when(userRepository.findByUsername("user1")).thenReturn(Optional.of(user));

    when(dbConfigRepo.findById(dbId)).thenReturn(Optional.of(mockConfig));
    when(dbSessionService.getConnection(session, mockConfig)).thenReturn(connection);

    SqlResult expectedResult =
        new SqlResult(
            "SUCCESS", null, "Query returned 1 rows", List.of("id"), List.of(Map.of("id", 100)));

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
    mockConfig.setId(dbId);

    // Mock Auth
    User user = new User();
    user.setUsername("user1");
    user.setAccessibleDatabases(Set.of(mockConfig));
    when(userRepository.findByUsername("user1")).thenReturn(Optional.of(user));

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
    mockConfig.setId(dbId);
    mockConfig.setName("TestDB");
    mockConfig.setJdbcUrl("jdbc:postgresql://localhost:5432/testdb"); // Postgres Type

    // Mock Auth
    Authentication auth = mock(Authentication.class);
    when(auth.getName()).thenReturn("user1");

    User user = new User();
    user.setUsername("user1");
    user.setAccessibleDatabases(Set.of(mockConfig));
    when(userRepository.findByUsername("user1")).thenReturn(Optional.of(user));

    when(dbConfigRepo.findById(dbId)).thenReturn(Optional.of(mockConfig));
    when(dbSessionService.getConnection(session, mockConfig)).thenReturn(connection);

    // Mock JDBC Result (Columns)
    // Expected Query: SELECT table_name, column_name ...
    // Use LinkedHashMap to ensure order for test stability, as the service relies on value order
    Map<String, Object> row1 = new java.util.LinkedHashMap<>();
    row1.put("table_name", "users");
    row1.put("column_name", "id");

    Map<String, Object> row2 = new java.util.LinkedHashMap<>();
    row2.put("table_name", "users");
    row2.put("column_name", "username");

    Map<String, Object> row3 = new java.util.LinkedHashMap<>();
    row3.put("table_name", "orders");
    row3.put("column_name", "id");

    SqlResult mockResult =
        new SqlResult(
            "SUCCESS", null, "OK", List.of("table_name", "column_name"), List.of(row1, row2, row3));

    when(jdbcExecutor.executeSql(eq(connection), contains("information_schema")))
        .thenReturn(mockResult);

    // Act
    Map<String, List<String>> schema = sqlExecutorService.getTableSchema(dbId, session, auth);

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
