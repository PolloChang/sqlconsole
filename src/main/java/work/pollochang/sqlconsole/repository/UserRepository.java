package work.pollochang.sqlconsole.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import work.pollochang.sqlconsole.model.entity.User;

public interface UserRepository extends JpaRepository<User, Long> {
  Optional<User> findByUsername(String username);
}
