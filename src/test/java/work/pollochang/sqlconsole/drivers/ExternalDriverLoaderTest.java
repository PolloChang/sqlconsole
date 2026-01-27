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
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.stream.Stream;
import java.net.URL;

import work.pollochang.sqlconsole.drivers.ExternalDriverClassLoader;
import work.pollochang.sqlconsole.drivers.DriverShim;
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
        createDummyDriverJar(jarPath);
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

    private static void createDummyDriverJar(Path jarPath) throws IOException {
        String packageName = "work.pollochang.sqlconsole.drivers.test";
        String className = "DummyDriver";
        String fullClassName = packageName + "." + className;
        String source = "package " + packageName + ";\n" +
                "\n" +
                "import java.sql.Connection;\n" +
                "import java.sql.Driver;\n" +
                "import java.sql.DriverPropertyInfo;\n" +
                "import java.sql.SQLException;\n" +
                "import java.sql.SQLFeatureNotSupportedException;\n" +
                "import java.util.Properties;\n" +
                "import java.util.logging.Logger;\n" +
                "import java.lang.reflect.Proxy;\n" +
                "\n" +
                "public class DummyDriver implements Driver {\n" +
                "    public DummyDriver() {}\n" +
                "\n" +
                "    @Override\n" +
                "    public Connection connect(String url, Properties info) throws SQLException {\n" +
                "        if (!acceptsURL(url)) return null;\n" +
                "        return (Connection) Proxy.newProxyInstance(\n" +
                "            getClass().getClassLoader(),\n" +
                "            new Class<?>[]{Connection.class},\n" +
                "            (proxy, method, args) -> {\n" +
                "                if (\"isValid\".equals(method.getName())) return true;\n" +
                "                if (\"close\".equals(method.getName())) return null;\n" +
                "                return null;\n" +
                "            }\n" +
                "        );\n" +
                "    }\n" +
                "\n" +
                "    @Override\n" +
                "    public boolean acceptsURL(String url) throws SQLException {\n" +
                "        return url != null && url.startsWith(\"jdbc:dummy:\");\n" +
                "    }\n" +
                "\n" +
                "    @Override\n" +
                "    public DriverPropertyInfo[] getPropertyInfo(String url, Properties info) throws SQLException {\n" +
                "        return new DriverPropertyInfo[0];\n" +
                "    }\n" +
                "\n" +
                "    @Override\n" +
                "    public int getMajorVersion() { return 1; }\n" +
                "\n" +
                "    @Override\n" +
                "    public int getMinorVersion() { return 0; }\n" +
                "\n" +
                "    @Override\n" +
                "    public boolean jdbcCompliant() { return false; }\n" +
                "\n" +
                "    @Override\n" +
                "    public Logger getParentLogger() throws SQLFeatureNotSupportedException {\n" +
                "        throw new SQLFeatureNotSupportedException();\n" +
                "    }\n" +
                "}";

        Path sourcePath = tempDir.resolve(className + ".java");
        Files.writeString(sourcePath, source);

        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        int result = compiler.run(null, null, null, sourcePath.toString());
        if (result != 0) {
            throw new RuntimeException("Compilation failed");
        }

        Path classPath = tempDir.resolve(className + ".class");

        try (JarOutputStream jos = new JarOutputStream(new FileOutputStream(jarPath.toFile()))) {
            JarEntry entry = new JarEntry(packageName.replace('.', '/') + "/" + className + ".class");
            jos.putNextEntry(entry);
            Files.copy(classPath, jos);
            jos.closeEntry();
        }
    }
}
