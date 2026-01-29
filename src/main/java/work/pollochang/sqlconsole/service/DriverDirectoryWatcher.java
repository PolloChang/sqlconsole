package work.pollochang.sqlconsole.service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import work.pollochang.sqlconsole.model.dto.DriverLoadRequest;

import java.io.IOException;
import java.nio.file.*;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class DriverDirectoryWatcher {

    private final ExternalDriverService driverService;

    @Value("${app.home:./drivers}")
    private String appHome;

    @Getter
    private Path driversLibDir;

    // Track files: Path -> LastModifiedTime
    private final Map<Path, Long> pendingFiles = new ConcurrentHashMap<>();
    // Track file sizes: Path -> Size
    private final Map<Path, Long> fileSizes = new ConcurrentHashMap<>();

    // Configurable settling time (e.g., 2000 ms)
    private static final long SETTLING_DELAY_MS = 2000;

    @PostConstruct
    public void init() throws IOException {
        driversLibDir = Paths.get(appHome, "lib");
        Files.createDirectories(driversLibDir);
        log.info("Monitoring driver directory: {}", driversLibDir.toAbsolutePath());
    }

    // Polling approach is often more robust than WatchService for "settling" logic across OSs,
    // especially for network shares or slow uploads where CREATE/MODIFY events can be spammy.
    // We scan every 1 second.
    @Scheduled(fixedDelay = 1000)
    public void scanDirectory() {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(driversLibDir, "*.jar")) {
            for (Path entry : stream) {
                // Efficiency Fix: Skip processed files before filesystem access
                if (!isProcessed(entry)) {
                    processFile(entry);
                }
            }
        } catch (IOException e) {
            log.error("Error scanning driver directory", e);
        }
    }

    private void processFile(Path file) {
        try {
            long currentSize = Files.size(file);
            long currentTime = System.currentTimeMillis();

            // If it's a new file we haven't seen, or we are tracking it
            if (!fileSizes.containsKey(file)) {
                // First time seeing it
                fileSizes.put(file, currentSize);
                pendingFiles.put(file, currentTime);
                log.debug("Detected new file: {}, waiting for settle...", file.getFileName());
            } else {
                long lastSize = fileSizes.get(file);
                if (currentSize != lastSize) {
                    // Size changed, reset timer
                    fileSizes.put(file, currentSize);
                    pendingFiles.put(file, currentTime);
                    log.debug("File {} is growing...", file.getFileName());
                } else {
                    // Size stable
                    long firstSeen = pendingFiles.get(file);
                    if ((currentTime - firstSeen) > SETTLING_DELAY_MS) {
                        // It has settled!
                        // Check if already processed/registered?
                        // For simplicity in this requirement, we assume we try to register.
                        // Ideally we should track "processed" state to avoid re-registering loop.
                        // But ExternalDriverService handles duplicates via SHA256.

                        // We remove from tracking so we don't spam attempts,
                        // but we need a way to know "we are done with this file".
                        // Simple approach: activeDrivers check inside Service will fail if duplicate.
                        // So we should only call if we haven't successfully processed it recently.
                        // For this task, we'll try to register and if it fails (duplicate), we ignore.

                        // We remove it from pending/sizes so we stop checking it continuously?
                        // If we remove, next scan sees it as "new" and starts over.
                        // So we need a "processed" set.
                        if (!isProcessed(file)) {
                             tryToRegister(file);
                             markProcessed(file);
                        }
                    }
                }
            }
        } catch (IOException e) {
            log.error("Error checking file {}", file, e);
        }
    }

    // In-memory set of processed files to avoid loops.
    // In real prod, this might need to sync with DB or just rely on DB check.
    private final java.util.Set<Path> processedFiles = java.util.Collections.synchronizedSet(new java.util.HashSet<>());

    private boolean isProcessed(Path file) {
        return processedFiles.contains(file);
    }

    private void markProcessed(Path file) {
        processedFiles.add(file);
        // Clean up tracking maps to save memory
        fileSizes.remove(file);
        pendingFiles.remove(file);
    }

    private void tryToRegister(Path file) {
        log.info("File {} settled. Attempting registration.", file.getFileName());
        try {
            DriverLoadRequest request = new DriverLoadRequest();
            request.setJarPath(file.toAbsolutePath().toString());
            // Auto-detect is enabled by null className
            driverService.registerDriver(request);
            log.info("Successfully auto-registered: {}", file.getFileName());
        } catch (Exception e) {
            if (e.getMessage().contains("already active") || e.getMessage().contains("duplicate")) {
                log.info("Skipping duplicate driver: {}", file.getFileName());
            } else {
                log.error("Failed to auto-register driver: {}", file.getFileName(), e);
            }
        }
    }
}
