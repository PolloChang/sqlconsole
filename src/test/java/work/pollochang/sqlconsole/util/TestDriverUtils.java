package work.pollochang.sqlconsole.util;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

public class TestDriverUtils {

    public static void createDummyDriverJar(Path jarPath) throws IOException {
        createDummyDriverJar(jarPath, "work.pollochang.sqlconsole.drivers.test", "DummyDriver", "jdbc:dummy:");
    }

    public static void createDummyDriverJar(Path jarPath, String packageName, String className, String urlPrefix) throws IOException {
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
                "public class " + className + " implements Driver {\n" +
                "    public " + className + "() {}\n" +
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
                "        return url != null && url.startsWith(\"" + urlPrefix + "\");\n" +
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

        Path tempDir = jarPath.getParent();
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
