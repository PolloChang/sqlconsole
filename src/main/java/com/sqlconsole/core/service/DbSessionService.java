package com.sqlconsole.core.service;

import com.sqlconsole.core.model.entity.DbConfig;
import jakarta.servlet.http.HttpSession;
import java.sql.Connection;
import java.sql.SQLException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


@Service
/**
 * Service for Database Sessions.
 * 管理 Session 中的 JDBC 連線
 */
public class DbSessionService {

  @Autowired private DbConfigService dbConfigService;

  /**
   * 獲取資料庫連線 (優先從 Session 中取得，若無則新建)
   *
   * @param session HTTP Session
   * @param config 資料庫設定
   * @return 資料庫連線
   * @throws SQLException 若連線建立失敗
   */
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

  /**
   * 關閉資料庫連線並從 Session 中移除
   *
   * @param session HTTP Session
   * @param dbConfigId 資料庫設定 ID
   */
  public void closeConnection(HttpSession session, Long dbConfigId) {
    String key = "CONN_" + dbConfigId;
    Connection conn = (Connection) session.getAttribute(key);
    if (conn != null) {
      try {
        conn.close();
      } catch (Exception e) {
      }
      session.removeAttribute(key);
    }
  }
}
