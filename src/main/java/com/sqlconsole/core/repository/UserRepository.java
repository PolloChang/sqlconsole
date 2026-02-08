package com.sqlconsole.core.repository;

import com.sqlconsole.core.model.entity.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Repository for managing User entities. */
public interface UserRepository extends JpaRepository<User, Long> {

  /**
   * Finds a user by their username.
   *
   * @param username the username to search for
   * @return an Optional containing the user if found, or empty
   */
  Optional<User> findByUsername(String username);

  /**
   * Checks if a user exists with the given username.
   *
   * @param username the username to check
   * @return true if a user exists, false otherwise
   */
  boolean existsByUsername(String username);
}
