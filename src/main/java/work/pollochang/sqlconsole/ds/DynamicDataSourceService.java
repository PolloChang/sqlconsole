package work.pollochang.sqlconsole.ds;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.datasource.SimpleDriverDataSource;
import org.springframework.stereotype.Service;
import work.pollochang.sqlconsole.drivers.DriverPreUnloadEvent;
import work.pollochang.sqlconsole.model.entity.SysDataSource;
import work.pollochang.sqlconsole.repository.SysDataSourceRepository;
import work.pollochang.sqlconsole.service.ExternalDriverService;

import javax.sql.DataSource;
import java.sql.Driver;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class DynamicDataSourceService {

    private final SysDataSourceRepository repository;
    private final ExternalDriverService driverService;

    // Cache: DataSourceID -> HikariDataSource
    private final Map<Long, HikariDataSource> activePools = new ConcurrentHashMap<>();

    public DataSource getDataSource(Long dataSourceId) {
        if (activePools.containsKey(dataSourceId)) {
            return activePools.get(dataSourceId);
        }

        SysDataSource dsEntity = repository.findById(dataSourceId)
                .orElseThrow(() -> new IllegalArgumentException("DataSource not found: " + dataSourceId));

        log.info("Creating DataSource pool for: {}", dsEntity.getName());

        // 1. Get the Isolated Driver Instance
        Driver driver = driverService.getDriverInstance(dsEntity.getDriver().getId());

        // 2. Wrap in SimpleDriverDataSource to adapt to DataSource interface
        SimpleDriverDataSource driverDataSource = new SimpleDriverDataSource(driver, dsEntity.getUrl(), dsEntity.getUsername(), dsEntity.getPassword());

        // 3. Configure HikariCP
        HikariConfig config = new HikariConfig();
        config.setDataSource(driverDataSource);
        config.setPoolName("Pool-" + dsEntity.getName());
        config.setMaximumPoolSize(dsEntity.getMaxPoolSize() != null ? dsEntity.getMaxPoolSize() : 10);

        // Validation timeout adjustment if needed
        config.setValidationTimeout(5000);

        HikariDataSource hikariDataSource = new HikariDataSource(config);
        activePools.put(dataSourceId, hikariDataSource);

        return hikariDataSource;
    }

    @EventListener
    public void handleDriverUnload(DriverPreUnloadEvent event) {
        log.info("Received DriverPreUnloadEvent for Driver ID: {}", event.getDriverId());

        // Find all pools using this driver
        // In a real app, we'd query DB or map to find which DS uses which Driver.
        // Here we iterate active pools and check (optimistic approach) or query repo.

        // Better: iterate repo to find DS IDs using this driver, then close their pools.
        repository.findAll().stream()
            .filter(ds -> ds.getDriver() != null && ds.getDriver().getId().equals(event.getDriverId()))
            .forEach(ds -> {
                if (activePools.containsKey(ds.getId())) {
                    log.info("Closing pool for DataSource: {}", ds.getName());
                    HikariDataSource pool = activePools.remove(ds.getId());
                    if (pool != null) {
                        pool.close();
                    }
                }
            });
    }
}
