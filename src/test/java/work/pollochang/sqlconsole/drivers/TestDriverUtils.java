package work.pollochang.sqlconsole.drivers;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.DriverPropertyInfo;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.Properties;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.logging.Logger;

public class TestDriverUtils {

    public static Path createDummyDriverJar(String fullClassName) throws IOException {
        String packageName = "";
        String simpleName = fullClassName;
        int lastDot = fullClassName.lastIndexOf('.');
        if (lastDot > 0) {
            packageName = fullClassName.substring(0, lastDot);
            simpleName = fullClassName.substring(lastDot + 1);
        }

        Path jarPath = Files.createTempFile("dummy-driver", ".jar");
        File jarFile = jarPath.toFile();
        jarFile.deleteOnExit();

        StringBuilder source = new StringBuilder();
        if (!packageName.isEmpty()) {
            source.append("package ").append(packageName).append(";\n");
        }
        source.append("import java.sql.*;\n")
              .append("import java.util.Properties;\n")
              .append("import java.util.logging.Logger;\n")
              .append("import java.lang.reflect.Proxy;\n")
              .append("import java.lang.reflect.InvocationHandler;\n")
              .append("import java.lang.reflect.Method;\n")
              .append("\n")
              .append("public class ").append(simpleName).append(" implements Driver {\n")
              .append("    static {\n")
              .append("        try {\n")
              .append("            DriverManager.registerDriver(new ").append(simpleName).append("());\n")
              .append("        } catch (SQLException e) {\n")
              .append("            throw new RuntimeException(e);\n")
              .append("        }\n")
              .append("    }\n")
              .append("\n")
              .append("    public Connection connect(String url, Properties info) throws SQLException {\n")
              .append("        return (Connection) Proxy.newProxyInstance(\n")
              .append("            Connection.class.getClassLoader(),\n")
              .append("            new Class[]{Connection.class},\n")
              .append("            new InvocationHandler() {\n")
              .append("                @Override\n")
              .append("                public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {\n")
              .append("                    String name = method.getName();\n")
              .append("                    if (name.equals(\"isValid\")) return true;\n")
              .append("                    if (name.equals(\"getAutoCommit\")) return true;\n")
              .append("                    if (name.equals(\"getNetworkTimeout\")) return 0;\n")
              .append("                    if (name.equals(\"isReadOnly\")) return false;\n")
              .append("                    if (name.equals(\"isClosed\")) return false;\n")
              .append("                    if (name.equals(\"getTransactionIsolation\")) return Connection.TRANSACTION_READ_COMMITTED;\n")
              .append("                    if (name.equals(\"getHoldability\")) return ResultSet.HOLD_CURSORS_OVER_COMMIT;\n")
              .append("                    if (name.equals(\"isWrapperFor\")) return false;\n")
              .append("                    if (name.equals(\"unwrap\")) throw new SQLException(\"Not a wrapper\");\n")
              .append("                    if (name.equals(\"close\")) return null;\n")
              .append("                    if (name.equals(\"getMetaData\")) {\n")
              .append("                       return (DatabaseMetaData) Proxy.newProxyInstance(\n")
              .append("                           DatabaseMetaData.class.getClassLoader(),\n")
              .append("                           new Class[]{DatabaseMetaData.class},\n")
              .append("                           new InvocationHandler() {\n")
              .append("                               public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {\n")
              .append("                                   if (method.getName().equals(\"getJDBCMajorVersion\")) return 4;\n")
              .append("                                   if (method.getName().equals(\"isWrapperFor\")) return false;\n")
              .append("                                   if (method.getName().equals(\"unwrap\")) throw new SQLException(\"Not a wrapper\");\n")
              .append("                                   return null;\n")
              .append("                               }\n")
              .append("                           }\n")
              .append("                       );\n")
              .append("                    }\n")
              .append("                    return null;\n")
              .append("                }\n")
              .append("            }\n")
              .append("        );\n")
              .append("    }\n")
              .append("\n")
              .append("    public boolean acceptsURL(String url) throws SQLException {\n")
              .append("        return url.startsWith(\"jdbc:dummy:\");\n")
              .append("    }\n")
              .append("\n")
              .append("    public DriverPropertyInfo[] getPropertyInfo(String url, Properties info) throws SQLException {\n")
              .append("        return new DriverPropertyInfo[0];\n")
              .append("    }\n")
              .append("\n")
              .append("    public int getMajorVersion() {\n")
              .append("        return 1;\n")
              .append("    }\n")
              .append("\n")
              .append("    public int getMinorVersion() {\n")
              .append("        return 0;\n")
              .append("    }\n")
              .append("\n")
              .append("    public boolean jdbcCompliant() {\n")
              .append("        return true;\n")
              .append("    }\n")
              .append("\n")
              .append("    public Logger getParentLogger() throws SQLFeatureNotSupportedException {\n")
              .append("        throw new SQLFeatureNotSupportedException();\n")
              .append("    }\n")
              .append("}");

        // Create a temporary directory for source files
        Path sourceDir = Files.createTempDirectory("driver-source");
        sourceDir.toFile().deleteOnExit();

        File sourceFile = new File(sourceDir.toFile(), simpleName + ".java");
        Files.write(sourceFile.toPath(), source.toString().getBytes(StandardCharsets.UTF_8));

        Path classesDir = Files.createTempDirectory("classes");
        classesDir.toFile().deleteOnExit();

        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        ByteArrayOutputStream err = new ByteArrayOutputStream();
        int result = compiler.run(null, null, err, "-d", classesDir.toFile().getAbsolutePath(), sourceFile.getAbsolutePath());
        if (result != 0) {
            throw new RuntimeException("Compilation failed:\n" + err.toString());
        }

        // Package into JAR
        try (FileOutputStream fos = new FileOutputStream(jarFile);
             JarOutputStream jos = new JarOutputStream(fos)) {

            Files.walkFileTree(classesDir, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    if (file.toString().endsWith(".class")) {
                        Path relPath = classesDir.relativize(file);
                        // Normalize path to use / for ZIP
                        String entryName = relPath.toString().replace(File.separatorChar, '/');
                        JarEntry entry = new JarEntry(entryName);
                        jos.putNextEntry(entry);
                        Files.copy(file, jos);
                        jos.closeEntry();
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        }

        return jarPath;
    }
}
