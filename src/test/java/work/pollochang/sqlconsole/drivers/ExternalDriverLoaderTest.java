package work.pollochang.sqlconsole.drivers;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.io.TempDir;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import java.net.URL;

import work.pollochang.sqlconsole.drivers.ExternalDriverClassLoader;
import work.pollochang.sqlconsole.drivers.DriverShim;
import work.pollochang.sqlconsole.util.TestDriverUtils;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.sql.DriverManager;

public class ExternalDriverLoaderTest {

    @TempDir
    static Path tempDir;

    static Path jarPath;

    @BeforeAll
    static void setup() throws Exception {
        jarPath = tempDir.resolve("dummy-driver.jar");
        TestDriverUtils.createDummyDriverJar(jarPath);
    }

    @Test
    void shouldLoadDriverFromExternalJar() throws Exception {
        // Verify System ClassLoader cannot see the class
        assertThrows(ClassNotFoundException.class, () -> {
            Class.forName("work.pollochang.sqlconsole.drivers.test.DummyDriver");
        });

        // Initialize ExternalDriverClassLoader
        // We expect this class to exist in the main source
        URL[] urls = new URL[]{jarPath.toUri().toURL()};
        try (ExternalDriverClassLoader classLoader = new ExternalDriverClassLoader(urls)) {
            Class<?> driverClass = classLoader.loadClass("work.pollochang.sqlconsole.drivers.test.DummyDriver");
            assertNotNull(driverClass);
            assertEquals("work.pollochang.sqlconsole.drivers.test.DummyDriver", driverClass.getName());

            // Verify isolation: The loaded class's classloader should be our custom loader
            assertEquals(classLoader, driverClass.getClassLoader());
        }
    }

    @Test
    void shouldRegisterAndConnectViaShim() throws Exception {
        URL[] urls = new URL[]{jarPath.toUri().toURL()};
        try (ExternalDriverClassLoader classLoader = new ExternalDriverClassLoader(urls)) {
            // Load the driver class
            Class<?> driverClass = classLoader.loadClass("work.pollochang.sqlconsole.drivers.test.DummyDriver");
            java.sql.Driver driverInstance = (java.sql.Driver) driverClass.getDeclaredConstructor().newInstance();

            // Register via Shim
            DriverShim shim = new DriverShim(driverInstance);
            DriverManager.registerDriver(shim);

            try {
                // Test connection
                String url = "jdbc:dummy:test";
                // DriverManager.getConnection iterates over registered drivers.
                // The Shim should accept the URL and delegate to the wrapped driver.
                java.sql.Connection connection = DriverManager.getConnection(url);
                assertNotNull(connection);
                assertTrue(connection.isValid(1));
            } finally {
                // Cleanup
                DriverManager.deregisterDriver(shim);
            }
        }
    }

}
