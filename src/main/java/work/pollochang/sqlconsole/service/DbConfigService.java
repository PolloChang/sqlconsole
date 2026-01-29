package work.pollochang.sqlconsole.service;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;
import org.postgresql.util.DriverInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import work.pollochang.sqlconsole.model.dto.DriverLoadRequest;
import work.pollochang.sqlconsole.model.entity.DbConfig;
import work.pollochang.sqlconsole.model.entity.User;
import work.pollochang.sqlconsole.model.enums.DbType;
import work.pollochang.sqlconsole.repository.DbConfigRepository;
import work.pollochang.sqlconsole.repository.UserRepository;

@Service
@Slf4j
public class DbConfigService {

  //注入 ExternalDriverService
  @Autowired private ExternalDriverService externalDriverService;

  // 注入 Maven 解析服務
  @Autowired private MavenDriverResolverService mavenDriverResolverService;

  /**
   * 定義 DbType 與 Driver Class / Maven 的對照表
   */
  private static final Map<DbType, DriverInfo> DRIVER_INFO_MAP = Map.of(
          DbType.ORACLE, new DriverInfo(
                  "oracle.jdbc.OracleDriver",
                  "com.oracle.database.jdbc:ojdbc11:23.3.0.23.09"
          ),
          DbType.MYSQL, new DriverInfo(
                  "com.mysql.cj.jdbc.Driver",
                  "com.mysql:mysql-connector-j:8.3.0"
          ),
          DbType.POSTGRESQL, new DriverInfo(
                  "org.postgresql.Driver",
                  "org.postgresql:postgresql:42.7.2"
          )
  );

  private record DriverInfo(String className, String mavenCoords) {}

  @Autowired private DbConfigRepository dbConfigRepository;
  @Autowired private UserRepository userRepository;
  @Autowired private EncryptionService encryptionService;

  @Transactional(readOnly = true)
  public List<DbConfig> getAllConfigs() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    // Default to empty if no auth (shouldn't happen in secured endpoint)
    if (auth == null) {
      return List.of();
    }

    boolean isAdmin =
        auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals(User.ROLE_ADMIN));

    List<DbConfig> list;
    if (isAdmin) {
      list = dbConfigRepository.findAll();
    } else {
      String username = auth.getName();
      User user =
          userRepository
              .findByUsername(username)
              .orElseThrow(() -> new RuntimeException("User not found"));
      list = new ArrayList<>(user.getAccessibleDatabases());
    }

    // Return decrypted view for UI (but mask password)
    return list.stream()
        .map(
            c -> {
              DbConfig dto = new DbConfig();
              dto.setId(c.getId());
              dto.setName(c.getName());
              dto.setDbType(c.getDbType());
              dto.setJdbcUrl(c.getJdbcUrl());
              try {
                dto.setDbUser(encryptionService.decrypt(c.getDbUser()));
              } catch (Exception e) {
                dto.setDbUser("ERROR"); // Decryption failed
              }
              dto.setDbPassword("******"); // Mask password
              return dto;
            })
        .toList();
  }

  public DbConfig getConfigById(Long id) {
    return dbConfigRepository
        .findById(id)
        .orElseThrow(() -> new RuntimeException("Config not found: " + id));
  }

  @Transactional
  public DbConfig saveConfig(DbConfig config) {
    if (config.getId() != null) {
      DbConfig existing =
          dbConfigRepository
              .findById(config.getId())
              .orElseThrow(() -> new RuntimeException("Not Found"));

      existing.setName(config.getName());
      existing.setDbType(config.getDbType());
      existing.setJdbcUrl(config.getJdbcUrl());

      // Handle User: Always encrypt as UI sends plain text
      if (config.getDbUser() != null) {
        existing.setDbUser(encryptionService.encrypt(config.getDbUser()));
      }

      // Handle Password: Only update if changed (not masked)
      if (config.getDbPassword() != null
          && !config.getDbPassword().equals("******")
          && !config.getDbPassword().isEmpty()) {
        existing.setDbPassword(encryptionService.encrypt(config.getDbPassword()));
      }

      return dbConfigRepository.save(existing);
    } else {
      // New config
      if (config.getDbUser() != null) {
        config.setDbUser(encryptionService.encrypt(config.getDbUser()));
      }
      if (config.getDbPassword() != null) {
        config.setDbPassword(encryptionService.encrypt(config.getDbPassword()));
      }
      return dbConfigRepository.save(config);
    }
  }

  @Transactional
  public void deleteConfig(Long id) {
    dbConfigRepository.deleteById(id);
  }

  /** Creates a raw JDBC connection for the given config. Decrypts credentials before connecting. */
  public Connection createConnection(DbConfig config) throws SQLException {
    String decryptedUser = encryptionService.decrypt(config.getDbUser());
    String decryptedPassword = encryptionService.decrypt(config.getDbPassword());

    // 如果 Config 指定了 driverId，就直接向 ExternalDriverService 要驅動
    if (config.getDriverId() != null) {
      return createExplicitConnection(
              config.getDriverId(),
              config.getJdbcUrl(),
              decryptedUser,
              decryptedPassword
      );
    }

    return createConnection(
        config.getDbType(), config.getJdbcUrl(), decryptedUser, decryptedPassword);
  }

  /**
   * Helper to create connection with plain text credentials. Handles Oracle roles (e.g. "sys as
   * sysdba").
   */
  public Connection createConnection(DbType dbType, String url, String user, String password)
      throws SQLException {
    Properties props = new Properties();

    if (user != null) {
      String finalUser = user;
      if (dbType == DbType.ORACLE) {
        // Check for " as SYSDBA" etc.
        Pattern p =
            Pattern.compile("^(.*)\\s+as\\s+(sysdba|sysoper|sysasm)$", Pattern.CASE_INSENSITIVE);
        Matcher m = p.matcher(user);
        if (m.find()) {
          finalUser = m.group(1);
          String role = m.group(2).toUpperCase();
          props.put("internal_logon", role);
        }
      }
      props.put("user", finalUser);
    }

    if (password != null) {
      props.put("password", password);
    }

    return DriverManager.getConnection(url, props);
  }

  /**
   * Tests a connection using provided (plain text) parameters.
   * 智慧型測試連線：支援自動修復
   * */
  public String testConnection(DbType dbType, String url, String user, String password) {
    try {
      // 步驟 1: 嘗試找出既有的 Driver ID
      Long driverId = resolveDriverIdByType(dbType);

      Connection conn;
      try {
        if (driverId != null) {
          // 路線 A: 走隔離通道
          conn = createExplicitConnection(driverId, url, user, password);
        } else {
          // 路線 B: 走舊通道 (Legacy)
          conn = createConnectionLegacy(dbType, url, user, password);
        }

        try (conn) { return "SUCCESS"; }

      } catch (SQLException e) {
        // 步驟 2: 捕獲錯誤，判斷是否需要自動修復
        if (shouldAttemptAutoRecovery(e, driverId)) {
          log.info("Driver missing for {}, attempting auto-recovery...", dbType);

          // 觸發下載並註冊
          Long newDriverId = autoDownloadAndRegister(dbType);

          if (newDriverId != null) {
            // 步驟 3: 重試 (Retry)
            log.info("Auto-recovery successful. Retrying with Driver ID: {}", newDriverId);
            try (Connection retryConn = createExplicitConnection(newDriverId, url, user, password)) {
              return "SUCCESS";
            }
          }
        }
        throw e; // 如果無法修復，拋出原錯誤
      }
    } catch (Exception e) {
      log.error("Connection test failed: {}", e.getMessage());
      return "FAILED: " + e.getMessage();
    }
  }

  private String doTestConnection(Long driverId, DbType dbType, String url, String user, String password) throws SQLException {
    Connection conn;
    if (driverId != null) {
      conn = createExplicitConnection(driverId, url, user, password);
    } else {
      conn = createConnection(dbType, url, user, password);
    }
    try (conn) {
      return "SUCCESS";
    }
  }

  /**
   * 明確指定 Driver 的連線方法 (繞過 DriverManager 的自動媒合)
   * @param driverId
   * @param url
   * @param user
   * @param password
   * @return
   * @throws SQLException
   */
  private Connection createExplicitConnection(Long driverId, String url, String user, String password)
          throws SQLException {

    // 從您的 ExternalDriverService 取得該 ID 對應的 DriverShim
    Driver driver = externalDriverService.getDriverInstance(driverId);

    Properties props = new Properties();
    if (user != null) props.put("user", user);
    if (password != null) props.put("password", password);

    // 處理 Oracle 特殊屬性 (保留原本的邏輯)
    if (user != null && user.toLowerCase().contains(" as sysdba")) {
      // ... 您的 Oracle 處理邏輯 ...
    }

    // 直接呼叫 Driver.connect()
    // 這會直接使用該 Driver 的 ClassLoader 環境，避免 No suitable driver 錯誤
    Connection conn = driver.connect(url, props);

    if (conn == null) {
      throw new SQLException("The selected driver (" + driver.getClass().getName() +") does not accept the URL: " + url);
    }
    return conn;
  }


  // 輔助方法：根據 DbType 找 ID
  private Long resolveDriverIdByType(DbType dbType) {
    if (DRIVER_INFO_MAP.containsKey(dbType)) {
      String className = DRIVER_INFO_MAP.get(dbType).className;
      return externalDriverService.findActiveDriverIdByClass(className);
    }
    return null;
  }

  // 輔助方法：判斷是否為 "找不到 Driver" 的錯誤
  private boolean shouldAttemptAutoRecovery(SQLException e, Long currentDriverId) {
    // 如果已經指定了 DriverId 但還是錯，代表可能是連線資訊錯，而不是沒 Driver，不需重試
    // 但如果是 "No suitable driver" 錯誤，無論如何都可以試試
    return e.getMessage().contains("No suitable driver");
  }

  // 輔助方法：自動下載與註冊
  private Long autoDownloadAndRegister(DbType dbType) {
    DriverInfo info = DRIVER_INFO_MAP.get(dbType);
    if (info == null) {
      throw new RuntimeException("No auto-download configuration for " + dbType);
    }

    log.info("Resolving driver from Maven: {}", info.mavenCoords);
    var paths = mavenDriverResolverService.resolveArtifacts(info.mavenCoords);
    if (paths.isEmpty()) {
      throw new RuntimeException("Maven resolved no artifacts");
    }

    // 建構請求
    DriverLoadRequest request = new DriverLoadRequest();
    request.setJarPath(paths.get(0).toAbsolutePath().toString()); // 主 JAR
    request.setLibraryPaths(paths.stream().skip(1).map(p -> p.toAbsolutePath().toString()).toList()); // 依賴
    request.setDriverClassName(info.className); // 明確指定 Class Name

    return externalDriverService.registerDriver(request);
  }

  // 舊版連線 (DriverManager)
  private Connection createConnectionLegacy(DbType dbType, String url, String user, String password) throws SQLException {
    Properties props = new Properties();
    if (user != null) props.put("user", user);
    if (password != null) props.put("password", password);
    return DriverManager.getConnection(url, props);
  }
}
