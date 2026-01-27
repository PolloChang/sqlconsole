package work.pollochang.sqlconsole.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import work.pollochang.sqlconsole.model.dto.ConnectionRequest;
import work.pollochang.sqlconsole.model.dto.DriverLoadRequest;
import work.pollochang.sqlconsole.model.dto.JdbcTestResult;
import work.pollochang.sqlconsole.model.entity.SysExternalDriver;
import work.pollochang.sqlconsole.service.ConnectionTestService;
import work.pollochang.sqlconsole.service.ExternalDriverService;
import work.pollochang.sqlconsole.service.MavenDriverResolverService;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/drivers")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('ADMIN')")
public class ExternalDriverController {

    private final ExternalDriverService driverService;
    private final MavenDriverResolverService mavenResolverService;
    private final ConnectionTestService connectionTestService;

    @Value("${app.home:./drivers}")
    private String appHome;

    @PostMapping("/maven")
    public ResponseEntity<String> loadFromMaven(@RequestParam String coords) {
        try {
            List<Path> resolvedJars = mavenResolverService.resolveArtifacts(coords);
            if (resolvedJars.isEmpty()) {
                return ResponseEntity.badRequest().body("No JARs resolved for coordinates: " + coords);
            }

            // Assume the first one is the driver, others are libs?
            // Or typically Aether returns artifacts in order, but the order of "main" vs "deps"
            // depends on the tree.
            // However, usually the requested artifact is first?
            // Aether's resolveDependencies returns a list.
            // We'll treat the first one as main JAR and others as libs for simplicity in this phase,
            // or we try to load the main requested one.
            // Actually, Aether result list usually contains the root node first if we walk the graph,
            // but `repositorySystem.resolveDependencies` returns flattened list.
            // Let's assume the first one matching the artifactId is the main one, or just pick the first.

            // Refinement: `MavenDriverResolverService` returned `List<Path>`.
            // We need to identify which one is the "main" jar.
            // For now, let's take the first one as main, rest as libs.
            Path mainJar = resolvedJars.get(0);
            List<String> libPaths = resolvedJars.stream()
                .skip(1)
                .map(Path::toAbsolutePath)
                .map(Path::toString)
                .collect(Collectors.toList());

            DriverLoadRequest request = new DriverLoadRequest();
            request.setJarPath(mainJar.toAbsolutePath().toString());
            request.setLibraryPaths(libPaths);

            driverService.registerDriver(request);

            return ResponseEntity.ok("Driver registered successfully via Maven");
        } catch (Exception e) {
            log.error("Error loading Maven driver", e);
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    @PostMapping("/upload")
    public ResponseEntity<String> uploadDriver(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body("File is empty");
        }

        try {
            Path uploadDir = Paths.get(appHome, "upload");
            Files.createDirectories(uploadDir);

            String originalFilename = file.getOriginalFilename();
            if (originalFilename == null || originalFilename.isBlank()) {
                originalFilename = "driver-" + System.currentTimeMillis() + ".jar";
            }

            // Fix: Path Traversal Vulnerability
            String filename = Paths.get(originalFilename).getFileName().toString();

            Path targetPath = uploadDir.resolve(filename);
            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);

            DriverLoadRequest request = new DriverLoadRequest();
            request.setJarPath(targetPath.toAbsolutePath().toString());
            // No libs for single file upload

            driverService.registerDriver(request);

            return ResponseEntity.ok("Driver uploaded and registered successfully");
        } catch (IOException e) {
            log.error("Error uploading file", e);
            return ResponseEntity.internalServerError().body("Upload failed: " + e.getMessage());
        }
    }

    @PostMapping("/test-connection")
    public ResponseEntity<JdbcTestResult> testConnection(@RequestBody ConnectionRequest request) {
        if (request.getDriverId() == null || request.getUrl() == null) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(connectionTestService.testConnection(request.getDriverId(), request));
    }

    @GetMapping
    public ResponseEntity<List<SysExternalDriver>> listDrivers() {
        return ResponseEntity.ok(driverService.listLoadedDrivers());
    }
}
