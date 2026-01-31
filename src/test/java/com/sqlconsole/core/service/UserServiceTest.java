package com.sqlconsole.core.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import com.sqlconsole.core.model.entity.DbConfig;
import com.sqlconsole.core.model.entity.User;
import com.sqlconsole.core.repository.DbConfigRepository;
import com.sqlconsole.core.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

  @Mock private UserRepository userRepository;
  @Mock private DbConfigRepository dbConfigRepository;
  @Mock private PasswordEncoder passwordEncoder;

  @InjectMocks private UserService userService;

  @Test
  @DisplayName("createUser: Should successfully hash password and save user")
  void testCreateUser() {
    // Arrange
    User newUser = new User();
    newUser.setUsername("testuser");
    newUser.setPassword("rawpassword");
    newUser.setRole(User.ROLE_USER);

    when(passwordEncoder.encode("rawpassword")).thenReturn("hashedpassword");
    when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

    // Act
    User savedUser = userService.createUser(newUser);

    // Assert
    assertEquals("testuser", savedUser.getUsername());
    assertEquals("hashedpassword", savedUser.getPassword());
    verify(userRepository).save(any(User.class));
    verify(passwordEncoder).encode("rawpassword");
  }

  @Test
  @DisplayName("assignDatabases: Should correctly map user IDs to DbConfig IDs")
  void testAssignDatabases() {
    // Arrange
    Long userId = 1L;
    Set<Long> dbIds = Set.of(10L, 20L);

    User user = new User();
    user.setId(userId);
    user.setUsername("user1");

    DbConfig db1 = new DbConfig();
    db1.setId(10L);
    DbConfig db2 = new DbConfig();
    db2.setId(20L);

    when(userRepository.findById(userId)).thenReturn(Optional.of(user));
    when(dbConfigRepository.findAllById(dbIds)).thenReturn(java.util.List.of(db1, db2));
    when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

    // Act
    userService.assignDatabases(userId, dbIds);

    // Assert
    assertEquals(2, user.getAccessibleDatabases().size());
    assertTrue(user.getAccessibleDatabases().contains(db1));
    assertTrue(user.getAccessibleDatabases().contains(db2));
    verify(userRepository).save(user);
  }

  @Test
  @DisplayName("existsByUsername: Should detect duplicate usernames")
  void testExistsByUsername() {
    // Arrange
    String username = "existingUser";
    when(userRepository.existsByUsername(username)).thenReturn(true);

    // Act
    boolean exists = userService.existsByUsername(username);

    // Assert
    assertTrue(exists);
    verify(userRepository).existsByUsername(username);
  }
}
