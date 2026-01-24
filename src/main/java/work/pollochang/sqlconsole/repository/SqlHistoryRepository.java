package work.pollochang.sqlconsole.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import work.pollochang.sqlconsole.model.entity.SqlHistory;

import java.util.List;

public interface SqlHistoryRepository extends JpaRepository<SqlHistory, Long> {}

