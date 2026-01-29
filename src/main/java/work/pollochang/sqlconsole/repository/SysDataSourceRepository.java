package work.pollochang.sqlconsole.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import work.pollochang.sqlconsole.model.entity.SysDataSource;

public interface SysDataSourceRepository extends JpaRepository<SysDataSource, Long> {
}
