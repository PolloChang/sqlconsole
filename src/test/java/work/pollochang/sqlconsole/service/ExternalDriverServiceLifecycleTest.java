package work.pollochang.sqlconsole.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.SpringApplication;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;
import work.pollochang.sqlconsole.model.dto.DriverLoadRequest;
import work.pollochang.sqlconsole.model.entity.SysExternalDriver;
import work.pollochang.sqlconsole.repository.SysExternalDriverRepository;
import work.pollochang.sqlconsole.util.TestDriverUtils;

import java.nio.file.Path;
import java.sql.Driver;
import java.sql.DriverManager;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect"
})
class ExternalDriverServiceLifecycleTest {

    @Autowired
    private ExternalDriverService service;

    @Autowired
    private SysExternalDriverRepository repository;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @TempDir
    Path tempDir;

    @Test
    void testRegisterAndUnloadDriver() throws Exception {
        // 1. Setup: Create Dummy JAR
        Path jarPath = tempDir.resolve("lifecycle-driver.jar");
        String driverClassName = "work.pollochang.sqlconsole.drivers.test.LifecycleDriver";
        String urlPrefix = "jdbc:lifecycle:";
        TestDriverUtils.createDummyDriverJar(jarPath, "work.pollochang.sqlconsole.drivers.test", "LifecycleDriver", urlPrefix);

        // 2. Action: Register Driver
        DriverLoadRequest request = new DriverLoadRequest();
        request.setJarPath(jarPath.toAbsolutePath().toString());
        request.setDriverClassName(driverClassName);

        service.registerDriver(request);

        // 3. Verify: Driver is in DriverManager
        boolean driverFound = false;
        for (Driver d : Collections.list(DriverManager.getDrivers())) {
            try {
                if (d.acceptsURL(urlPrefix + "test")) {
                    driverFound = true;
                    break;
                }
            } catch (Exception e) {
                // ignore
            }
        }
        assertTrue(driverFound, "Driver should be registered in DriverManager");

        // 4. Verify: DB record exists and is active
        List<SysExternalDriver> drivers = repository.findAll();
        assertEquals(1, drivers.size());
        SysExternalDriver driverRecord = drivers.get(0);
        assertTrue(driverRecord.isActive());
        assertEquals(driverClassName, driverRecord.getDriverClass());

        // 5. Action: Unload Driver
        service.unloadDriver(driverRecord.getId());

        // 6. Verify: Driver is NOT in DriverManager
        driverFound = false;
        for (Driver d : Collections.list(DriverManager.getDrivers())) {
            try {
                if (d.acceptsURL(urlPrefix + "test")) {
                    driverFound = true;
                    break;
                }
            } catch (Exception e) {
                // ignore
            }
        }
        assertFalse(driverFound, "Driver should be deregistered from DriverManager");

        // 7. Verify: DB record is inactive
        SysExternalDriver updatedRecord = repository.findById(driverRecord.getId()).orElseThrow();
        assertFalse(updatedRecord.isActive());
    }

    @Test
    void testBootstrap_ShouldReloadActiveDriversOnStartup() throws Exception {
        // 1. Setup: Pre-populate DB with active driver
        Path jarPath = tempDir.resolve("bootstrap-driver.jar");
        String driverClassName = "work.pollochang.sqlconsole.drivers.test.BootstrapDriver";
        String urlPrefix = "jdbc:bootstrap:";
        TestDriverUtils.createDummyDriverJar(jarPath, "work.pollochang.sqlconsole.drivers.test", "BootstrapDriver", urlPrefix);

        SysExternalDriver entity = new SysExternalDriver();
        entity.setDriverName(driverClassName);
        entity.setDriverClass(driverClassName);
        entity.setJarPath(jarPath.toAbsolutePath().toString());
        entity.setActive(true);
        entity.setSha256(UUID.randomUUID().toString());
        repository.save(entity);

        // 2. Action: Trigger Bootstrap Event
        // In a real startup, Spring fires this. We simulate it.
        eventPublisher.publishEvent(new ApplicationReadyEvent(new SpringApplication(), null, null, null));

        // 3. Verify: Driver is loaded in DriverManager
        boolean driverFound = false;
        for (Driver d : Collections.list(DriverManager.getDrivers())) {
            try {
                if (d.acceptsURL(urlPrefix + "test")) {
                    driverFound = true;
                    break;
                }
            } catch (Exception e) {
                // ignore
            }
        }
        assertTrue(driverFound, "Driver should be loaded after bootstrap event");

        // Cleanup (since Service state persists in memory during test context)
        service.unloadDriver(entity.getId());
    }

    @Test
    void testDuplicateRegistration_ShouldPreventConflict() throws Exception {
        // 1. Setup: Create Dummy JAR
        Path jarPath = tempDir.resolve("duplicate-driver.jar");
        String driverClassName = "work.pollochang.sqlconsole.drivers.test.DuplicateDriver";
        TestDriverUtils.createDummyDriverJar(jarPath, "work.pollochang.sqlconsole.drivers.test", "DuplicateDriver", "jdbc:duplicate:");

        // 2. Action: Register First Time
        DriverLoadRequest request = new DriverLoadRequest();
        request.setJarPath(jarPath.toAbsolutePath().toString());
        request.setDriverClassName(driverClassName);
        service.registerDriver(request);

        // 3. Action: Register Second Time (Same JAR)
        // Should throw exception because SHA-256 will match (once implemented)
        // or purely based on active status.
        Exception exception = assertThrows(RuntimeException.class, () -> {
            service.registerDriver(request);
        });

        // The message should indicate duplicate
        assertTrue(exception.getMessage().toLowerCase().contains("duplicate") ||
                   exception.getMessage().toLowerCase().contains("already"));
    }
}
