package work.pollochang.sqlconsole.controller;

import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import work.pollochang.sqlconsole.model.entity.User;
import work.pollochang.sqlconsole.service.UserService;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class UserController {

  private final UserService userService;

  @GetMapping
  public List<User> getAllUsers() {
    return userService.getAllUsers();
  }

  @PostMapping
  public ResponseEntity<User> createUser(@RequestBody User user) {
    if (userService.existsByUsername(user.getUsername())) {
      return ResponseEntity.badRequest().build();
    }
    return ResponseEntity.ok(userService.createUser(user));
  }

  @GetMapping("/{id}")
  public ResponseEntity<User> getUser(@PathVariable Long id) {
    return ResponseEntity.ok(userService.getUserById(id));
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
    userService.deleteUser(id);
    return ResponseEntity.ok().build();
  }

  @PostMapping("/{id}/databases")
  public ResponseEntity<Void> assignDatabases(@PathVariable Long id, @RequestBody Set<Long> dbIds) {
    userService.assignDatabases(id, dbIds);
    return ResponseEntity.ok().build();
  }
}
