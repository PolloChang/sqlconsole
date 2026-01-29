package work.pollochang.sqlconsole.service;

import jakarta.servlet.http.HttpSession;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import work.pollochang.sqlconsole.core.DbaProvider;
import work.pollochang.sqlconsole.core.DbaReport;
import work.pollochang.sqlconsole.model.dto.SqlResult;
import work.pollochang.sqlconsole.model.entity.DbConfig;
import work.pollochang.sqlconsole.model.entity.SqlHistory;
import work.pollochang.sqlconsole.model.entity.User;
import work.pollochang.sqlconsole.repository.DbConfigRepository;
import work.pollochang.sqlconsole.repository.SqlHistoryRepository;
import work.pollochang.sqlconsole.repository.UserRepository;

/** 處理 SQL 解析、執行與審核。 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SqlExecutorService {

  private final AuditService auditService;
  private final DbConfigRepository dbConfigRepo;
  private final SqlHistoryRepository historyRepo;
  private final DbSessionService dbSessionService;
  private final JdbcExecutor jdbcExecutor; // ✅ 注入新的 Helper
  private final UserRepository userRepository;

  // 自動收集所有 Provider (包含 OS 版與未來 Premium 版)
  private final List<DbaProvider> dbaProviders;

  /**
   * 獲取資料庫執行計畫 (Requirement 13)
   */
  public DbaReport getExplainPlan(Connection conn, DbConfig config, String sql) {
    // 根據 DbType 動態尋找適用的 Provider
    Optional<DbaProvider> provider = dbaProviders.stream()
            .filter(p -> p.supports(String.valueOf(config.getDbType())))
            .findFirst();

    if (provider.isPresent()) {
      log.debug("Using DBA Provider: {} for DB Type: {}",
              provider.get().getClass().getSimpleName(), config.getDbType());
      return provider.get().getExecutionPlan(conn, sql);
    }

    log.warn("No DBA Provider found for DB Type: {}", config.getDbType());
    return new DbaReport("No DBA Provider found for " + config.getDbType(), List.of(), -1);
  }

  private void validateAccess(Long dbId, String username, String role) {
    if (User.ROLE_ADMIN.equals(role)) {
      return;
    }
    User user =
        userRepository
            .findByUsername(username)
            .orElseThrow(() -> new RuntimeException("User not found"));

    boolean hasAccess =
        user.getAccessibleDatabases().stream().anyMatch(db -> db.getId().equals(dbId));

    if (!hasAccess) {
      throw new AccessDeniedException("Access Denied to DB: " + dbId);
    }
  }

  public Map<String, List<String>> getTableSchema(
      Long dbId, HttpSession session, Authentication auth) {
    String role =
        auth.getAuthorities().stream()
            .findFirst()
            .map(GrantedAuthority::getAuthority)
            .orElse(User.ROLE_USER);

    validateAccess(dbId, auth.getName(), role);

    DbConfig config =
        dbConfigRepo.findById(dbId).orElseThrow(() -> new RuntimeException("DB Not Found"));
    String url = config.getJdbcUrl().toLowerCase();
    String sql = "";

    // Determine SQL based on DB Type
    if (url.contains(":oracle:")) {
      sql = "SELECT table_name, column_name FROM user_tab_columns ORDER BY table_name, column_id";
    } else if (url.contains(":postgresql:")) {
      sql =
          "SELECT table_name, column_name FROM information_schema.columns WHERE table_schema NOT IN ('information_schema', 'pg_catalog') ORDER BY table_name, ordinal_position";
    } else if (url.contains(":db2:")) {
      sql =
          "SELECT tabname AS table_name, colname AS column_name FROM syscat.columns WHERE tabschema = CURRENT SCHEMA ORDER BY tabname, colno";
    } else if (url.contains(":sqlserver:")) {
      sql =
          "SELECT table_name, column_name FROM information_schema.columns WHERE table_schema = SCHEMA_NAME() ORDER BY table_name, ordinal_position";
    } else if (url.contains(":mysql:")) {
      sql =
          "SELECT table_name, column_name FROM information_schema.columns WHERE table_schema = DATABASE() ORDER BY table_name, ordinal_position";
    } else if (url.contains(":h2:")) {
      sql =
          "SELECT table_name, column_name FROM information_schema.columns WHERE table_schema = 'PUBLIC' ORDER BY table_name, ordinal_position";
    } else {
      return Collections.emptyMap();
    }

    try {
      Connection conn = dbSessionService.getConnection(session, config);
      SqlResult result = jdbcExecutor.executeSql(conn, sql);

      if ("SUCCESS".equals(result.status()) && result.rows() != null) {
        // Group by Table Name
        return result.rows().stream()
            .collect(
                Collectors.groupingBy(
                    row ->
                        row.values().stream()
                            .findFirst()
                            .orElse("UNKNOWN")
                            .toString(), // First column is Table
                    Collectors.mapping(
                        row ->
                            row.values().stream()
                                .skip(1)
                                .findFirst()
                                .orElse("UNKNOWN")
                                .toString(), // Second column is Column
                        Collectors.toList())));
      }
    } catch (SQLException e) {
      log.error("Failed to fetch schema", e);
    }
    return Collections.emptyMap();
  }

  /**
   * 判斷是否為敏感操作
   *
   * @param sql
   * @return
   */
  private boolean isRestricted(String sql) {
    String upper = sql.trim().toUpperCase();
    return upper.startsWith("INSERT")
        || upper.startsWith("UPDATE")
        || upper.startsWith("DELETE")
        || upper.startsWith("CREATE")
        || upper.startsWith("DROP")
        || upper.startsWith("ALTER")
        || upper.startsWith("TRUNCATE")
        || upper.startsWith("GRANT");
  }

  public SqlResult processRequest(
      Long dbId, String sql, String username, String role, HttpSession session) {
    validateAccess(dbId, username, role);

    DbConfig config =
        dbConfigRepo.findById(dbId).orElseThrow(() -> new RuntimeException("DB Not Found"));
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
   *
   * @param session
   * @param config
   * @param commit
   * @return
   */
  private SqlResult executeTcl(HttpSession session, DbConfig config, boolean commit) {
    try {
      Connection conn = dbSessionService.getConnection(session, config);
      if (commit) conn.commit();
      else conn.rollback();
      return new SqlResult(
          "SUCCESS", "COMMITTED", commit ? "Commit Success" : "Rollback Success", null, null);
    } catch (SQLException e) {
      return new SqlResult("ERROR", "UNCOMMIT", e.getMessage(), null, null);
    }
  }

  /**
   * 核心執行邏輯
   *
   * @param autoCommitAfterExec 是否在執行成功後自動 Commit (用於審核通過的工單)
   */
  public SqlResult executeRawSql(
      HttpSession session,
      DbConfig config,
      String sql,
      String executor,
      boolean autoCommitAfterExec) {
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
      result = new SqlResult(result.status(), txStatus, msg, result.columns(), result.rows());

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
