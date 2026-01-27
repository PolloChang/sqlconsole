package work.pollochang.sqlconsole.scheduler;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;
import work.pollochang.sqlconsole.service.ExternalDriverService;
import work.pollochang.sqlconsole.service.DriverDirectoryWatcher;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Duration;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.awaitility.Awaitility.await;

@SpringBootTest
@TestPropertySource(properties = {
    "app.home=${java.io.tmpdir}/sqlconsole-watcher-test",
    "app.drivers.watcher.enabled=true",
    "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect"
})
class DriverDirectoryWatcherTest {

    @Autowired
    private DriverDirectoryWatcher watcher;

    @MockBean
    private ExternalDriverService externalDriverService;

    @TempDir
    Path tempDir; // Note: We actually need to point app.home to this, but @TempDir is runtime.
                  // In Spring Boot test, property replacement happens early.
                  // So we might need a workaround or just rely on the watcher creating the dir based on the property.

    @Test
    void testWatchService_ShouldHandlePartialWrites() throws Exception {
        // We need to access the actual directory the watcher is monitoring
        Path watchDir = watcher.getDriversLibDir();
        assertNotNull(watchDir);
        Files.createDirectories(watchDir);

        Path jarFile = watchDir.resolve("slow-upload.jar");

        // 1. Simulate partial write (create empty file)
        Files.writeString(jarFile, "part1");

        // Force scan immediately to register initial size
        watcher.scanDirectory();

        // Wait a bit - size should be tracked
        Thread.sleep(500);

        // 2. Append more data (simulating ongoing upload)
        Files.writeString(jarFile, "part2", StandardOpenOption.APPEND);

        // Force scan again - should detect size change
        watcher.scanDirectory();

        // Verify service has NOT been called yet (because file changed size)
        verify(externalDriverService, never()).registerDriver(any());

        // 3. Stop writing and wait for settling period (configured to 2000ms)
        // We need to advance time or wait. Since we use System.currentTimeMillis() in prod code,
        // we have to wait real time.

        // Scan again (still not enough time passed since last write, < 2s)
        Thread.sleep(1000);
        watcher.scanDirectory();
        verify(externalDriverService, never()).registerDriver(any());

        // Wait enough time (> 2s total from last write)
        Thread.sleep(2500);

        // Force final scan
        watcher.scanDirectory();

        // Now it should be called
        verify(externalDriverService, atLeastOnce()).registerDriver(any());
    }
}
