package work.pollochang.sqlconsole.service;

import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import work.pollochang.sqlconsole.model.entity.DbConfig;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DbSessionServiceTest {

    @InjectMocks
    private DbSessionService dbSessionService;

    @Mock private HttpSession session;
    @Mock private Connection connection;
    @Mock private DbConfig dbConfig;

    @Test
    void testGetConnection_ExistingOpenConnection() throws SQLException {
        // Arrange
        when(dbConfig.getId()).thenReturn(1L);
        when(session.getAttribute("CONN_1")).thenReturn(connection);
        when(connection.isClosed()).thenReturn(false);

        // Act
        Connection result = dbSessionService.getConnection(session, dbConfig);

        // Assert
        assertSame(connection, result);
        verify(session, never()).setAttribute(anyString(), any());
    }

    @Test
    void testGetConnection_NewConnection() throws SQLException {
        // Arrange
        when(dbConfig.getId()).thenReturn(1L);
        when(dbConfig.getJdbcUrl()).thenReturn("jdbc:h2:mem:test");
        when(dbConfig.getDbUser()).thenReturn("sa");
        when(dbConfig.getDbPassword()).thenReturn("");
        when(session.getAttribute("CONN_1")).thenReturn(null); // 模擬無連線

        // 使用 MockStatic 模擬靜態方法 DriverManager
        try (MockedStatic<DriverManager> mockedDriver = mockStatic(DriverManager.class)) {
            mockedDriver.when(() -> DriverManager.getConnection(anyString(), anyString(), anyString()))
                    .thenReturn(connection);

            // Act
            Connection result = dbSessionService.getConnection(session, dbConfig);

            // Assert
            assertSame(connection, result);
            verify(connection).setAutoCommit(false); // 驗證有設為手動 Commit
            verify(session).setAttribute("CONN_1", connection);
        }
    }

    @Test
    void testCloseConnection_Exists() throws SQLException {
        // Arrange
        when(session.getAttribute("CONN_1")).thenReturn(connection);

        // Act
        dbSessionService.closeConnection(session, 1L);

        // Assert
        verify(connection).close();
        verify(session).removeAttribute("CONN_1");
    }

    @Test
    void testCloseConnection_NotExists() throws SQLException {
        // Arrange
        when(session.getAttribute("CONN_1")).thenReturn(null);

        // Act
        dbSessionService.closeConnection(session, 1L);

        // Assert
        verify(connection, never()).close();
    }

    @Test
    void testCloseConnection_Exception() throws SQLException {
        // 為了覆蓋 catch (Exception e) {}
        when(session.getAttribute("CONN_1")).thenReturn(connection);
        doThrow(new SQLException("Close error")).when(connection).close();

        // Act & Assert (Should not throw)
        assertDoesNotThrow(() -> dbSessionService.closeConnection(session, 1L));
        verify(session).removeAttribute("CONN_1");
    }
}