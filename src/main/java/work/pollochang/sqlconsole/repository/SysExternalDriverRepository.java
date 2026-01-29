package work.pollochang.sqlconsole.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import work.pollochang.sqlconsole.model.entity.SysExternalDriver;

import java.util.List;
import java.util.Optional;

public interface SysExternalDriverRepository extends JpaRepository<SysExternalDriver, Long> {
    List<SysExternalDriver> findByIsActiveTrue();
    Optional<SysExternalDriver> findBySha256(String sha256);
}
