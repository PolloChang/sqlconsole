package work.pollochang.sqlconsole.service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import work.pollochang.sqlconsole.model.entity.User;
import work.pollochang.sqlconsole.repository.DbConfigRepository;
import work.pollochang.sqlconsole.repository.UserRepository;

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
