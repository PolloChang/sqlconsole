package work.pollochang.sqlconsole.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import work.pollochang.sqlconsole.model.entity.DbConfig;

import java.util.List;

public interface DbConfigRepository extends JpaRepository<DbConfig, Long> {}
