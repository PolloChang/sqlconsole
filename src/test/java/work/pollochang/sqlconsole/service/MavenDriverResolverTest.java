package work.pollochang.sqlconsole.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import work.pollochang.sqlconsole.service.MavenDriverResolverService;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
    "app.home=${java.io.tmpdir}/sqlconsole-test"
})
class MavenDriverResolverTest {

    @Autowired
    private MavenDriverResolverService resolverService;

    @Test
    void testMavenResolution_ShouldDownloadAllDependencies() throws Exception {
        // Use a real, small, public artifact that has dependencies (or at least exists)
        // H2 is already in cache likely, but let's try something small.
        // "org.xerial:sqlite-jdbc:3.45.1.0" is good, single jar mostly.
        // Let's try something with transitive deps if possible, or just verify basic download.
        // using "com.google.guava:guava:33.0.0-jre" as it has failureaccess dep.
        String gav = "com.google.guava:guava:33.0.0-jre";

        List<Path> resolvedJars = resolverService.resolveArtifacts(gav);

        assertNotNull(resolvedJars);
        assertFalse(resolvedJars.isEmpty(), "Should resolve at least one JAR");

        // Check if files exist
        for (Path jar : resolvedJars) {
            assertTrue(Files.exists(jar), "Resolved JAR should exist on disk: " + jar);
            assertTrue(jar.toString().endsWith(".jar"), "File should be a JAR");
        }

        // Verify we got the main artifact
        boolean mainArtifactFound = resolvedJars.stream()
            .anyMatch(p -> p.getFileName().toString().contains("guava"));
        assertTrue(mainArtifactFound, "Main artifact should be in the list");
    }
}
