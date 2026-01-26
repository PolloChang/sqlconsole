package work.pollochang.sqlconsole.controller;

import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;
import work.pollochang.sqlconsole.model.entity.User;
import work.pollochang.sqlconsole.service.DbConfigService;
import work.pollochang.sqlconsole.service.UserService;

@RestController
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class UserController {

  private final UserService userService;
  private final DbConfigService dbConfigService;

  @GetMapping("/admin/users")
  public ModelAndView viewUsers(Model model) {
    ModelAndView mav = new ModelAndView("users");
    mav.addObject("dbs", dbConfigService.getAllConfigs());
    return mav;
  }

  @GetMapping("/api/users")
  public List<User> getAllUsers() {
    return userService.getAllUsers();
  }

  @PostMapping("/api/users")
  public ResponseEntity<User> createUser(@RequestBody User user) {
    if (userService.existsByUsername(user.getUsername())) {
      return ResponseEntity.badRequest().build();
    }
    return ResponseEntity.ok(userService.createUser(user));
  }

  @PutMapping("/api/users/{id}")
  public ResponseEntity<User> updateUser(@PathVariable Long id, @RequestBody User user) {
    try {
      return ResponseEntity.ok(userService.updateUser(id, user));
    } catch (Exception e) {
      return ResponseEntity.badRequest().build();
    }
  }

  @GetMapping("/api/users/{id}")
  public ResponseEntity<User> getUser(@PathVariable Long id) {
    return ResponseEntity.ok(userService.getUserById(id));
  }

  @DeleteMapping("/api/users/{id}")
  public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
    userService.deleteUser(id);
    return ResponseEntity.ok().build();
  }

  @PostMapping("/api/users/{id}/databases")
  public ResponseEntity<Void> assignDatabases(@PathVariable Long id, @RequestBody Set<Long> dbIds) {
    userService.assignDatabases(id, dbIds);
    return ResponseEntity.ok().build();
  }
}
