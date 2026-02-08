package com.sqlconsole.core.controller;

import com.sqlconsole.core.model.entity.DbConfig;
import com.sqlconsole.core.model.enums.DbType;
import com.sqlconsole.core.service.DbConfigService;
import com.sqlconsole.core.service.EncryptionService;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;


@Controller
@PreAuthorize("hasRole('ADMIN')")
/**
 * Controller for Database Configuration.
 */
public class DbConfigController {

  @Autowired private DbConfigService dbConfigService;

  @Autowired private EncryptionService encryptionService;

  /**
   * Serves the database connections page.
   *
   * @param model the UI model
   * @return the view name "connections"
   */
  @GetMapping("/connections")
  public String connectionsPage(Model model) {
    return "connections";
  }

  /**
   * Retrieves all database configurations.
   *
   * @return the list of database configurations
   */
  @GetMapping("/api/connections")
  @ResponseBody
  public List<DbConfig> getConnections() {
    return dbConfigService.getAllConfigs();
  }

  /**
   * Saves or updates a database configuration.
   *
   * @param config the configuration to save
   * @return the saved configuration (masked) or error message
   */
  @PostMapping("/api/connections")
  @ResponseBody
  public ResponseEntity<?> saveConnection(@RequestBody DbConfig config) {
    try {
      DbConfig saved = dbConfigService.saveConfig(config);
      // Mask password and decrypt user before returning to UI
      saved.setDbPassword("******");
      try {
        saved.setDbUser(encryptionService.decrypt(saved.getDbUser()));
      } catch (Exception e) {
        // Ignore decryption error, return as is
      }
      return ResponseEntity.ok(saved);
    } catch (Exception e) {
      return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
    }
  }

  /**
   * Deletes a database configuration by ID.
   *
   * @param id the configuration ID
   * @return success message or error message
   */
  @DeleteMapping("/api/connections/{id}")
  @ResponseBody
  public ResponseEntity<?> deleteConnection(@PathVariable Long id) {
    try {
      dbConfigService.deleteConfig(id);
      return ResponseEntity.ok(Map.of("message", "Deleted successfully"));
    } catch (Exception e) {
      return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
    }
  }

  /**
   * Tests a database connection.
   *
   * @param payload map containing dbType, jdbcUrl, dbUser, dbPassword
   * @return status and message
   */
  @PostMapping("/api/connections/test")
  @ResponseBody
  public ResponseEntity<?> testConnection(@RequestBody Map<String, String> payload) {
    try {
      String dbTypeStr = payload.get("dbType");
      String jdbcUrl = payload.get("jdbcUrl");
      String dbUser = payload.get("dbUser");
      String dbPassword = payload.get("dbPassword");

      if (dbTypeStr == null || jdbcUrl == null) {
        return ResponseEntity.badRequest().body(Map.of("message", "Missing parameters"));
      }

      DbType dbType = DbType.valueOf(dbTypeStr);
      String result = dbConfigService.testConnection(dbType, jdbcUrl, dbUser, dbPassword);

      if ("SUCCESS".equals(result)) {
        return ResponseEntity.ok(Map.of("status", "SUCCESS", "message", "Connection Successful"));
      } else {
        return ResponseEntity.ok(Map.of("status", "FAILED", "message", result));
      }
    } catch (Exception e) {
      return ResponseEntity.ok(Map.of("status", "ERROR", "message", e.getMessage()));
    }
  }
}
