package com.sqlconsole.core.service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.sqlconsole.core.model.entity.User;
import com.sqlconsole.core.repository.DbConfigRepository;
import com.sqlconsole.core.repository.UserRepository;

@Service
@RequiredArgsConstructor
public class UserService {

  private final UserRepository userRepository;
  private final DbConfigRepository dbConfigRepository;
  private final PasswordEncoder passwordEncoder;

  @Transactional
  public User createUser(User user) {
    user.setPassword(passwordEncoder.encode(user.getPassword()));
    return userRepository.save(user);
  }

  @Transactional
  public User updateUser(Long id, User userDetails) {
    User user =
        userRepository
            .findById(id)
            .orElseThrow(() -> new RuntimeException("User not found: " + id));

    // Update username if changed and check distinct
    if (!user.getUsername().equals(userDetails.getUsername())) {
      if (userRepository.existsByUsername(userDetails.getUsername())) {
        throw new RuntimeException("Username already exists");
      }
      user.setUsername(userDetails.getUsername());
    }

    // Update Role
    user.setRole(userDetails.getRole());

    // Update Password only if provided
    if (userDetails.getPassword() != null && !userDetails.getPassword().isEmpty()) {
      user.setPassword(passwordEncoder.encode(userDetails.getPassword()));
    }

    return userRepository.save(user);
  }

  @Transactional
  public void assignDatabases(Long userId, Set<Long> dbIds) {
    var user =
        userRepository
            .findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found: " + userId));

    var configs = dbConfigRepository.findAllById(dbIds);
    user.setAccessibleDatabases(new HashSet<>(configs));

    userRepository.save(user);
  }

  @Transactional(readOnly = true)
  public boolean existsByUsername(String username) {
    return userRepository.existsByUsername(username);
  }

  @Transactional(readOnly = true)
  public List<User> getAllUsers() {
    return userRepository.findAll();
  }

  @Transactional(readOnly = true)
  public User getUserById(Long id) {
    return userRepository.findById(id).orElseThrow(() -> new RuntimeException("User not found"));
  }

  @Transactional
  public void deleteUser(Long id) {
    userRepository.deleteById(id);
  }
}
