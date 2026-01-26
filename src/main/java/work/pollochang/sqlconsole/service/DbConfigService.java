package work.pollochang.sqlconsole.service;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import work.pollochang.sqlconsole.model.entity.DbConfig;
import work.pollochang.sqlconsole.model.enums.DbType;
import work.pollochang.sqlconsole.repository.DbConfigRepository;

@Service
@Slf4j
public class DbConfigService {

  @Autowired private DbConfigRepository dbConfigRepository;

  @Autowired private EncryptionService encryptionService;

  public List<DbConfig> getAllConfigs() {
    // Return decrypted view for UI (but mask password)
    List<DbConfig> list = dbConfigRepository.findAll();
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

  /** Tests a connection using provided (plain text) parameters. */
  public String testConnection(DbType dbType, String url, String user, String password) {
    try (Connection conn = createConnection(dbType, url, user, password)) {
      return "SUCCESS";
    } catch (SQLException e) {
      log.error("Connection test failed", e);
      return "FAILED: " + e.getMessage();
    }
  }
}
