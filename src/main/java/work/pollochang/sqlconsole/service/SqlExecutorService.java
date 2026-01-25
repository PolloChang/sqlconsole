package work.pollochang.sqlconsole.service;

import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import work.pollochang.sqlconsole.model.entity.DbConfig;
import work.pollochang.sqlconsole.model.entity.SqlHistory;
import work.pollochang.sqlconsole.repository.DbConfigRepository;
import work.pollochang.sqlconsole.repository.SqlHistoryRepository;
import work.pollochang.sqlconsole.model.dto.SqlResult;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * 處理 SQL 解析、執行與審核。
 */
@Slf4j
@Service
public class SqlExecutorService {

    @Autowired private AuditService auditService;
    @Autowired private DbConfigRepository dbConfigRepo;
    @Autowired private SqlHistoryRepository historyRepo;
    @Autowired private DbSessionService dbSessionService;
    @Autowired private JdbcExecutor jdbcExecutor; // ✅ 注入新的 Helper

    /**
     * 判斷是否為敏感操作
     * @param sql
     * @return
     */
    private boolean isRestricted(String sql) {
        String upper = sql.trim().toUpperCase();
        return upper.startsWith("INSERT") || upper.startsWith("UPDATE") ||
                upper.startsWith("DELETE") || upper.startsWith("CREATE") ||
                upper.startsWith("DROP") || upper.startsWith("ALTER") ||
                upper.startsWith("TRUNCATE") || upper.startsWith("GRANT");
    }

    public SqlResult processRequest(Long dbId, String sql, String username, String role, HttpSession session) {
        DbConfig config = dbConfigRepo.findById(dbId).orElseThrow(() -> new RuntimeException("DB Not Found"));
        String upperSql = sql.trim().toUpperCase();

        if (upperSql.equals("COMMIT")) return executeTcl(session, config, true);
        if (upperSql.equals("ROLLBACK")) return executeTcl(session, config, false);

        if (isRestricted(sql) && !role.equals("ROLE_AUDITOR")) {
            // 呼叫介面，付費版會攔截並回傳 PENDING，免費版回傳 null
            SqlResult auditResult = auditService.checkAndAudit(username, dbId, sql);
            if (auditResult != null) {
                return auditResult;
            }
        }

        return executeRawSql(session, config, sql, username, false);
    }

    /**
     * 執行 TCL 指令
     * @param session
     * @param config
     * @param commit
     * @return
     */
    private SqlResult executeTcl(HttpSession session, DbConfig config, boolean commit) {
        try {
            Connection conn = dbSessionService.getConnection(session, config);
            if (commit) conn.commit(); else conn.rollback();
            return new SqlResult("SUCCESS", "COMMITTED", commit ? "Commit Success" : "Rollback Success", null, null);
        } catch (SQLException e) {
            return new SqlResult("ERROR", "UNCOMMIT", e.getMessage(), null, null);
        }
    }

    /**
     * 核心執行邏輯
     * @param autoCommitAfterExec 是否在執行成功後自動 Commit (用於審核通過的工單)
     */
    public SqlResult executeRawSql(HttpSession session, DbConfig config, String sql, String executor, boolean autoCommitAfterExec) {
        String status = "SUCCESS";
        String msg;
        String txStatus = "UNCOMMIT";
        SqlResult result = null;
        Connection conn = null;

        try {
            conn = dbSessionService.getConnection(session, config);

            // ✅ 將繁瑣的 JDBC 操作委派給 JdbcExecutor
            result = jdbcExecutor.executeSql(conn, sql);
            msg = result.message();

            // 處理自動 Commit (針對審核通過的工單)
            if (autoCommitAfterExec && !conn.getAutoCommit()) {
                conn.commit();
                msg += " (Auto Committed by System)";
            }

            // Determine txStatus
            if (conn.getAutoCommit()) {
                txStatus = "COMMITTED";
            } else {
                txStatus = "UNCOMMIT";
            }

            // Rebuild result with txStatus
            result = new SqlResult(
                    result.status(),
                    txStatus,
                    msg,
                    result.columns(),
                    result.rows()
            );

        } catch (SQLException e) {
            status = "ERROR";
            msg = e.getMessage();
            txStatus = "UNCOMMIT";

            if (conn != null) {
                try {
                    if (!conn.getAutoCommit()) {
                        log.warn("⚠️ SQL Error, Rolling back...");
                        conn.rollback();
                        msg += " (Transaction rolled back)";
                        txStatus = "COMMITTED";
                    } else {
                        txStatus = "COMMITTED";
                    }
                } catch (SQLException ex) {
                    log.error("Rollback failed", ex);
                }
            }
            result = new SqlResult("ERROR", txStatus, msg, null, null);
        }

        historyRepo.save(new SqlHistory(executor, config.getName(), sql, status));
        return result;
    }
}