package com.sqlconsole.core.service;

import com.sqlconsole.core.repository.UserRepository;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class AuthService implements UserDetailsService {

  private final UserRepository userRepo;

  /**
   * Constructs an AuthService.
   *
   * @param userRepo the user repository
   */
  public AuthService(UserRepository userRepo) {
    this.userRepo = userRepo;
  }

  /**
   * Loads a user by username for Spring Security.
   *
   * @param username the username to load
   * @return the user details
   * @throws UsernameNotFoundException if the user is not found
   */
  @Override
  public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
    var user =
        userRepo
            .findByUsername(username)
            .orElseThrow(() -> new UsernameNotFoundException("User not found"));

    return User.builder()
        .username(user.getUsername())
        .password(user.getPassword())
        .roles(
            user.getRole()
                .replace(
                    "ROLE_", "")) // Spring Security expects "ADMIN", not "ROLE_ADMIN" in builder
        .build();
  }
}
