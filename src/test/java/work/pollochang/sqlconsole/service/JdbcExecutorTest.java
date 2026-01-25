package work.pollochang.sqlconsole.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import work.pollochang.sqlconsole.model.dto.SqlResult;

import java.sql.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JdbcExecutorTest {

    @InjectMocks
    private JdbcExecutor jdbcExecutor;

    @Mock private Connection connection;
    @Mock private Statement statement;
    @Mock private ResultSet resultSet;
    @Mock private ResultSetMetaData metaData;

    @Test
    void testExecuteQuery_Success() throws SQLException {
        // Arrange
        String sql = "SELECT * FROM users;"; // 測試去除分號邏輯
        when(connection.createStatement()).thenReturn(statement);
        when(statement.execute("SELECT * FROM users")).thenReturn(true); // true 表示有 ResultSet
        when(statement.getResultSet()).thenReturn(resultSet);
        when(resultSet.getMetaData()).thenReturn(metaData);

        // 模擬兩欄位
        when(metaData.getColumnCount()).thenReturn(2);
        when(metaData.getColumnLabel(1)).thenReturn("id");
        when(metaData.getColumnLabel(2)).thenReturn("name");

        // 模擬兩筆資料
        when(resultSet.next()).thenReturn(true, true, false);
        when(resultSet.getObject("id")).thenReturn(1, 2);
        when(resultSet.getObject("name")).thenReturn("Alice", "Bob");

        // Act
        SqlResult result = jdbcExecutor.executeSql(connection, sql);

        // Assert
        assertEquals("SUCCESS", result.status());
        assertEquals("Query returned 2 rows.", result.message());
        assertEquals(2, result.columns().size());
        assertEquals(2, result.rows().size());
        assertEquals("Alice", result.rows().get(0).get("name"));
    }

    @Test
    void testExecuteUpdate_Success() throws SQLException {
        // Arrange
        String sql = "UPDATE users SET name='Alice'";
        when(connection.createStatement()).thenReturn(statement);
        when(statement.execute(sql)).thenReturn(false); // false 表示是 Update Count
        when(statement.getUpdateCount()).thenReturn(5);

        // Act
        SqlResult result = jdbcExecutor.executeSql(connection, sql);

        // Assert
        assertEquals("SUCCESS", result.status());
        assertEquals("Affected rows: 5", result.message());
        assertTrue(result.rows().isEmpty());
    }

    // 雖然 JdbcExecutor 內部沒有 catch block，但通常會測試它是否正確拋出異常給上層
    @Test
    void testExecuteSql_ThrowsException() throws SQLException {
        when(connection.createStatement()).thenThrow(new SQLException("DB Error"));

        assertThrows(SQLException.class, () -> {
            jdbcExecutor.executeSql(connection, "SELECT 1");
        });
    }
}