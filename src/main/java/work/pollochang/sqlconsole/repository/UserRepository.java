package work.pollochang.sqlconsole.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import work.pollochang.sqlconsole.model.entity.User;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
}

