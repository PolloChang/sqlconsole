package work.pollochang.sqlconsole.service;

import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import work.pollochang.sqlconsole.model.entity.DbConfig;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * 管理 Session 中的 JDBC 連線
 */
@Service
public class DbSessionService {

    @Autowired
    private DbConfigService dbConfigService;

    public Connection getConnection(HttpSession session, DbConfig config) throws SQLException {
        String key = "CONN_" + config.getId();
        Connection conn = (Connection) session.getAttribute(key);

        if (conn == null || conn.isClosed()) {
            // Use DbConfigService to create connection (handles decryption and roles)
            conn = dbConfigService.createConnection(config);
            conn.setAutoCommit(false); // 啟用手動 TCL
            session.setAttribute(key, conn);
        }
        return conn;
    }

    public void closeConnection(HttpSession session, Long dbConfigId) {
        String key = "CONN_" + dbConfigId;
        Connection conn = (Connection) session.getAttribute(key);
        if (conn != null) {
            try { conn.close(); } catch (Exception e) {}
            session.removeAttribute(key);
        }
    }
}