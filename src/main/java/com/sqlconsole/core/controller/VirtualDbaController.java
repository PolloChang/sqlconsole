package com.sqlconsole.core.controller;

import com.sqlconsole.core.core.vdba.VirtualDbaReport;
import com.sqlconsole.core.core.vdba.VirtualDbaService;
import com.sqlconsole.core.service.DbConfigService;
import java.util.concurrent.CompletableFuture;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

/** [LicenseGate] */
@Controller
@RequiredArgsConstructor
/**
 * Controller for Virtual DBA.
 */
public class VirtualDbaController {

  private final VirtualDbaService virtualDbaService;
  private final DbConfigService dbConfigService;

  /**
   * Serves the Virtual DBA page.
   *
   * @param model the UI model
   * @param auth the authentication object
   * @return the view name "vdba"
   */
  @GetMapping("/vdba")
  public String vdbaPage(Model model, Authentication auth) {
    model.addAttribute("dbs", dbConfigService.getAllConfigs());
    model.addAttribute("username", auth.getName());
    return "vdba";
  }

  /**
   * Diagnoses a SQL query and returns a report.
   *
   * @param dbId the database ID
   * @param sql the SQL query to analyze
   * @return a completable future containing the Virtual DBA report
   * @throws AccessDeniedException if the user does not have access to the database
   */
  @PostMapping("/api/vdba/diagnose")
  @ResponseBody
  public CompletableFuture<VirtualDbaReport> diagnose(
      @RequestParam Long dbId, @RequestParam String sql) {
    // Access Control: Ensure user has access to the requested DB
    boolean hasAccess =
        dbConfigService.getAllConfigs().stream().anyMatch(db -> db.getId().equals(dbId));

    if (!hasAccess) {
      throw new AccessDeniedException("Access Denied to DB: " + dbId);
    }

    return virtualDbaService.diagnose(dbId, sql);
  }
}
