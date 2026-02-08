package com.sqlconsole.core.controller;

import com.sqlconsole.core.model.entity.User;
import com.sqlconsole.core.service.DbConfigService;
import com.sqlconsole.core.service.UserService;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

@RestController
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class UserController {

  private final UserService userService;
  private final DbConfigService dbConfigService;

  /**
   * Returns the user management view.
   *
   * @param model the UI model
   * @return the model and view for users page
   */
  @GetMapping("/admin/users")
  public ModelAndView viewUsers(Model model) {
    ModelAndView mav = new ModelAndView("users");
    mav.addObject("dbs", dbConfigService.getAllConfigs());
    return mav;
  }

  /**
   * Retrieves all users.
   *
   * @return the list of users
   */
  @GetMapping("/api/users")
  public List<User> getAllUsers() {
    return userService.getAllUsers();
  }

  /**
   * Creates a new user.
   *
   * @param user the user details
   * @return the created user or bad request if username exists
   */
  @PostMapping("/api/users")
  public ResponseEntity<User> createUser(@RequestBody User user) {
    if (userService.existsByUsername(user.getUsername())) {
      return ResponseEntity.badRequest().build();
    }
    return ResponseEntity.ok(userService.createUser(user));
  }

  /**
   * Updates an existing user.
   *
   * @param id the user ID
   * @param user the updated user details
   * @return the updated user or bad request on failure
   */
  @PutMapping("/api/users/{id}")
  public ResponseEntity<User> updateUser(@PathVariable Long id, @RequestBody User user) {
    try {
      return ResponseEntity.ok(userService.updateUser(id, user));
    } catch (Exception e) {
      return ResponseEntity.badRequest().build();
    }
  }

  /**
   * Retrieves a user by ID.
   *
   * @param id the user ID
   * @return the user details
   */
  @GetMapping("/api/users/{id}")
  public ResponseEntity<User> getUser(@PathVariable Long id) {
    return ResponseEntity.ok(userService.getUserById(id));
  }

  /**
   * Deletes a user by ID.
   *
   * @param id the user ID
   * @return response entity
   */
  @DeleteMapping("/api/users/{id}")
  public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
    userService.deleteUser(id);
    return ResponseEntity.ok().build();
  }

  /**
   * Assigns accessible databases to a user.
   *
   * @param id the user ID
   * @param dbIds the set of database IDs
   * @return response entity
   */
  @PostMapping("/api/users/{id}/databases")
  public ResponseEntity<Void> assignDatabases(@PathVariable Long id, @RequestBody Set<Long> dbIds) {
    userService.assignDatabases(id, dbIds);
    return ResponseEntity.ok().build();
  }
}
