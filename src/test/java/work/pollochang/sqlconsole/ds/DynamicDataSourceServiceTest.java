package work.pollochang.sqlconsole.ds;

import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.context.ActiveProfiles;
import work.pollochang.sqlconsole.drivers.DriverPreUnloadEvent;
import work.pollochang.sqlconsole.drivers.TestDriverUtils;
import work.pollochang.sqlconsole.model.dto.DriverLoadRequest;
import work.pollochang.sqlconsole.model.entity.SysDataSource;
import work.pollochang.sqlconsole.model.entity.SysExternalDriver;
import work.pollochang.sqlconsole.repository.SysDataSourceRepository;
import work.pollochang.sqlconsole.service.ExternalDriverService;

import javax.sql.DataSource;
import java.nio.file.Path;
import java.sql.Connection;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class DynamicDataSourceServiceTest {

    @Autowired
    private ExternalDriverService driverService;

    @Autowired
    private DynamicDataSourceService dataSourceService;

    @Autowired
    private SysDataSourceRepository dataSourceRepository;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    private Long driverId;
    private Long dataSourceId;

    @BeforeEach
    void setUp() throws Exception {
        // 1. Create and Register Dummy Driver
        Path jarPath = TestDriverUtils.createDummyDriverJar("work.pollochang.sqlconsole.drivers.DummyDriverE");
        DriverLoadRequest request = new DriverLoadRequest();
        request.setJarPath(jarPath.toAbsolutePath().toString());
        request.setDriverClassName("work.pollochang.sqlconsole.drivers.DummyDriverE");

        driverService.registerDriver(request);
        SysExternalDriver driver = driverService.listLoadedDrivers().stream()
                .filter(d -> d.getDriverClass().equals("work.pollochang.sqlconsole.drivers.DummyDriverE"))
                .findFirst()
                .orElseThrow();
        driverId = driver.getId();

        // 2. Create SysDataSource
        SysDataSource ds = new SysDataSource();
        ds.setName("TestDS");
        ds.setUrl("jdbc:dummy:test");
        ds.setUsername("sa");
        ds.setPassword("pass");
        ds.setDriver(driver);
        ds.setMaxPoolSize(5);

        dataSourceId = dataSourceRepository.save(ds).getId();
    }

    @AfterEach
    void tearDown() {
        if (driverId != null) {
            try {
                driverService.unloadDriver(driverId);
            } catch (Exception e) {
                // Ignore if already unloaded
            }
        }
        dataSourceRepository.deleteAll();
    }

    @Test
    void shouldCreateDataSourceAndCloseOnUnload() throws Exception {
        // 1. Get DataSource
        DataSource ds = dataSourceService.getDataSource(dataSourceId);
        assertNotNull(ds);
        assertTrue(ds instanceof HikariDataSource);
        HikariDataSource hikariDS = (HikariDataSource) ds;

        // 2. Verify Connection (uses DummyDriver which returns Proxy)
        try (Connection conn = ds.getConnection()) {
            assertNotNull(conn);
            assertTrue(conn.isValid(1));
        }

        assertFalse(hikariDS.isClosed());

        // 3. Unload Driver -> Should trigger Event -> Close Pool
        driverService.unloadDriver(driverId);

        // 4. Verify Pool Closed
        assertTrue(hikariDS.isClosed());
    }
}
