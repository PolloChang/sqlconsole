package com.sqlconsole.core.controller;

import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import com.sqlconsole.core.model.entity.DbConfig;
import com.sqlconsole.core.model.enums.DbType;
import com.sqlconsole.core.service.DbConfigService;
import com.sqlconsole.core.service.EncryptionService;

@Controller
@PreAuthorize("hasRole('ADMIN')")
public class DbConfigController {

  @Autowired private DbConfigService dbConfigService;

  @Autowired private EncryptionService encryptionService;

  @GetMapping("/connections")
  public String connectionsPage(Model model) {
    return "connections";
  }

  @GetMapping("/api/connections")
  @ResponseBody
  public List<DbConfig> getConnections() {
    return dbConfigService.getAllConfigs();
  }

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
