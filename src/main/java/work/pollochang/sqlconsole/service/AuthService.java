package work.pollochang.sqlconsole.service;

import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import work.pollochang.sqlconsole.repository.UserRepository;

@Service
public class AuthService implements UserDetailsService {

  private final UserRepository userRepo;

  public AuthService(UserRepository userRepo) {
    this.userRepo = userRepo;
  }

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
