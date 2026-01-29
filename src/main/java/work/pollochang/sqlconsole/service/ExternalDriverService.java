package work.pollochang.sqlconsole.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import work.pollochang.sqlconsole.drivers.DriverPreUnloadEvent;
import work.pollochang.sqlconsole.drivers.DriverShim;
import work.pollochang.sqlconsole.drivers.ExternalDriverClassLoader;
import work.pollochang.sqlconsole.model.dto.DriverLoadRequest;
import work.pollochang.sqlconsole.model.entity.SysExternalDriver;
import work.pollochang.sqlconsole.repository.SysExternalDriverRepository;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExternalDriverService {

    private final SysExternalDriverRepository repository;
    private final ApplicationEventPublisher eventPublisher;
    private final Map<Long, DriverContext> activeDrivers = new ConcurrentHashMap<>();

    private record DriverContext(ExternalDriverClassLoader classLoader, DriverShim shim) {}

    public Driver getDriverInstance(Long driverId) {
        DriverContext context = activeDrivers.get(driverId);
        if (context == null) {
            throw new IllegalArgumentException("Driver not loaded or not found: " + driverId);
        }
        return context.shim;
    }

    @Transactional
    public void registerDriver(DriverLoadRequest request) {
        log.info("Registering driver from path: {}", request.getJarPath());

        DriverContext context = null;
        try {
            Path jarPath = Paths.get(request.getJarPath());
            if (!jarPath.toFile().exists()) {
                throw new IllegalArgumentException("Jar file not found: " + request.getJarPath());
            }

            // 1. Calculate SHA-256
            String sha256 = calculateSha256(jarPath);
            Optional<SysExternalDriver> existingOpt = repository.findBySha256(sha256);

            SysExternalDriver entity;
            if (existingOpt.isPresent()) {
                entity = existingOpt.get();
                if (entity.isActive()) {
                     throw new RuntimeException("Driver with same SHA-256 is already active: " + entity.getDriverName());
                }
                log.info("Reactivating existing driver: {}", entity.getDriverName());
                entity.setActive(true);
                // Update other fields if needed, e.g. path might have moved?
                entity.setJarPath(request.getJarPath());
                entity.setDriverClass(request.getDriverClassName());
            } else {
                entity = new SysExternalDriver();
                entity.setDriverName(request.getDriverClassName());
                entity.setDriverClass(request.getDriverClassName());
                entity.setJarPath(request.getJarPath());
                entity.setSha256(sha256);
                entity.setActive(true);
            }

            // Handle library paths
            if (request.getLibraryPaths() != null && !request.getLibraryPaths().isEmpty()) {
                entity.setLibPaths(String.join(",", request.getLibraryPaths()));
            }

            // 2. Create Context (Load Jar & Shim)
            // Note: We deliberately load AFTER check to avoid unnecessary loading
            context = createDriverContext(request.getJarPath(), request.getLibraryPaths(), request.getDriverClassName());

            // Auto-detect class name if not provided
            String resolvedClassName = context.shim.getWrappedDriver().getClass().getName();
            if (entity.getDriverClass() == null) {
                entity.setDriverName(resolvedClassName);
                entity.setDriverClass(resolvedClassName);
            }

            // 3. Register with DriverManager
            DriverManager.registerDriver(context.shim);
            log.info("Registered DriverShim for {}", resolvedClassName);

            // 4. Save to DB
            entity = repository.save(entity);

            // 5. Cache Context
            activeDrivers.put(entity.getId(), context);

        } catch (Exception e) {
            log.error("Failed to register driver", e);
            cleanupContext(context);
            throw new RuntimeException("Failed to register driver: " + e.getMessage(), e);
        }
    }

    public void loadExistingDriver(SysExternalDriver entity) throws Exception {
        log.info("Loading existing driver: {}", entity.getDriverName());
        if (activeDrivers.containsKey(entity.getId())) {
            log.info("Driver already loaded: {}", entity.getDriverName());
            return;
        }

        List<String> libPaths = null;
        if (entity.getLibPaths() != null && !entity.getLibPaths().isBlank()) {
            libPaths = List.of(entity.getLibPaths().split(","));
        }

        DriverContext context = createDriverContext(entity.getJarPath(), libPaths, entity.getDriverClass());
        DriverManager.registerDriver(context.shim);
        activeDrivers.put(entity.getId(), context);
    }

    private DriverContext createDriverContext(String jarPathStr, String className) throws Exception {
        return createDriverContext(jarPathStr, null, className);
    }

    private DriverContext createDriverContext(String jarPathStr, List<String> libPaths, String className) throws Exception {
        Path jarPath = Paths.get(jarPathStr);
        if (!jarPath.toFile().exists()) {
            throw new IllegalArgumentException("Jar file not found: " + jarPathStr);
        }

        List<URL> urlList = new java.util.ArrayList<>();
        urlList.add(jarPath.toUri().toURL());

        if (libPaths != null) {
            for (String libPathStr : libPaths) {
                Path libPath = Paths.get(libPathStr);
                if (libPath.toFile().exists()) {
                     urlList.add(libPath.toUri().toURL());
                } else {
                    log.warn("Dependency jar not found: {}", libPathStr);
                }
            }
        }

        URL[] urls = urlList.toArray(new URL[0]);
        ExternalDriverClassLoader classLoader = new ExternalDriverClassLoader(urls);

        Driver driver;
        try {
            if (className == null || className.isBlank()) {
                // Auto-discovery via ServiceLoader
                java.util.ServiceLoader<Driver> loader = java.util.ServiceLoader.load(Driver.class, classLoader);
                java.util.Iterator<Driver> it = loader.iterator();
                if (it.hasNext()) {
                    driver = it.next();
                    log.info("Auto-detected driver class: {}", driver.getClass().getName());
                } else {
                    throw new IllegalArgumentException("No JDBC Driver found in JAR and no class name provided.");
                }
            } else {
                Class<?> clazz = classLoader.loadClass(className);
                driver = (Driver) clazz.getDeclaredConstructor().newInstance();
            }
        } catch (Exception e) {
            classLoader.close();
            throw e;
        }

        DriverShim shim = new DriverShim(driver);
        return new DriverContext(classLoader, shim);
    }

    private void cleanupContext(DriverContext context) {
        if (context == null) return;
        if (context.shim != null) {
            try {
                DriverManager.deregisterDriver(context.shim);
            } catch (SQLException ex) {
                log.error("Failed to cleanup driver shim", ex);
            }
        }
        if (context.classLoader != null) {
            try {
                context.classLoader.close();
            } catch (IOException ex) {
                log.error("Failed to cleanup classloader", ex);
            }
        }
    }

    private String calculateSha256(Path path) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (InputStream fis = Files.newInputStream(path)) {
            byte[] buffer = new byte[8192];
            int n;
            while ((n = fis.read(buffer)) != -1) {
                digest.update(buffer, 0, n);
            }
        }
        StringBuilder sb = new StringBuilder();
        for (byte b : digest.digest()) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    @Transactional
    public void unloadDriver(Long id) {
        log.info("Unloading driver ID: {}", id);

        // Publish event to notify dependents (e.g. Connection Pools) to close
        eventPublisher.publishEvent(new DriverPreUnloadEvent(this, id));

        SysExternalDriver entity = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Driver not found: " + id));

        DriverContext context = activeDrivers.remove(id);
        if (context != null) {
            try {
                DriverManager.deregisterDriver(context.shim);
                log.info("Deregistered DriverShim for {}", entity.getDriverClass());

                context.classLoader.close();
                log.info("Closed ExternalDriverClassLoader");
            } catch (SQLException | IOException e) {
                log.error("Error unloading driver resources", e);
            }
        } else {
            log.warn("Driver context not found in memory for ID: {}. Maybe it was not loaded?", id);
        }

        entity.setActive(false);
        repository.save(entity);
    }

    public List<SysExternalDriver> listLoadedDrivers() {
        return repository.findByIsActiveTrue();
    }
}
