package work.pollochang.sqlconsole.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import work.pollochang.sqlconsole.drivers.DriverShim;
import work.pollochang.sqlconsole.model.dto.ConnectionRequest;
import work.pollochang.sqlconsole.model.dto.JdbcTestResult;
import work.pollochang.sqlconsole.model.entity.SysExternalDriver;
import work.pollochang.sqlconsole.repository.SysExternalDriverRepository;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.Driver;
import java.sql.DriverManager;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Service
@RequiredArgsConstructor
@Slf4j
public class ConnectionTestService {

    private final ExternalDriverService externalDriverService;
    private final SysExternalDriverRepository driverRepository; // Used to fetch metadata if needed directly or via service

    public JdbcTestResult testConnection(Long driverId, ConnectionRequest request) {
        log.info("Testing connection for driverId: {}, URL: {}", driverId, request.getUrl());

        Connection connection = null;
        try {
            // 1. Retrieve the driver
            // We need to access the active shim. ExternalDriverService manages the context.
            // Ideally ExternalDriverService should expose a way to get the Shim or Driver.
            // Since `activeDrivers` is private in `ExternalDriverService`, we might need to expose a getter
            // or we just trust DriverManager because the shim is registered?

            // Problem: DriverManager.getConnection(url) iterates all registered drivers.
            // If our Shim is registered, it will be tried.
            // But if we want to FORCE a specific driver ID, we must ensure that SPECIFIC shim picks it up
            // or we manually use the shim instance.

            // To ensure we use the isolated driver corresponding to driverId:
            // We need to fetch the Shim instance from ExternalDriverService.

            // Let's assume for now we rely on DriverManager resolution because Shim registers itself.
            // But to validate "driverId", we should check if it's active.
            SysExternalDriver driverEntity = driverRepository.findById(driverId)
                    .orElseThrow(() -> new IllegalArgumentException("Driver not found: " + driverId));

            if (!driverEntity.isActive()) {
                 throw new IllegalArgumentException("Driver is not active/loaded: " + driverId);
            }

            // 2. Connect with Timeout
            // DriverManager.setLoginTimeout is global, which is risky.
            // Better to use an Executor to bound the connect call.

            Properties props = new Properties();
            if (request.getUsername() != null) props.setProperty("user", request.getUsername());
            if (request.getPassword() != null) props.setProperty("password", request.getPassword());

            ExecutorService executor = Executors.newSingleThreadExecutor();
            Future<Connection> future = executor.submit(() -> DriverManager.getConnection(request.getUrl(), props));

            try {
                connection = future.get(5, TimeUnit.SECONDS); // 5s timeout
            } catch (TimeoutException e) {
                future.cancel(true);
                throw new java.sql.SQLTimeoutException("Connection timed out after 5 seconds");
            } catch (ExecutionException e) {
                Throwable cause = e.getCause();
                if (cause instanceof Exception) throw (Exception) cause;
                throw new RuntimeException(cause);
            } finally {
                executor.shutdownNow();
            }

            if (connection == null) {
                throw new java.sql.SQLException("No suitable driver found for URL: " + request.getUrl());
            }

            // 3. Validate
            boolean isValid = connection.isValid(5);
            if (!isValid) {
                throw new java.sql.SQLException("Connection is invalid (isValid returned false)");
            }

            DatabaseMetaData metaData = connection.getMetaData();
            String product = "Unknown";
            String version = "Unknown";
            try {
                product = metaData.getDatabaseProductName();
                version = metaData.getDatabaseProductVersion();
            } catch (Exception e) {
                log.warn("Failed to retrieve metadata", e);
            }

            return JdbcTestResult.builder()
                    .success(true)
                    .databaseProductName(product)
                    .databaseVersion(version)
                    .build();

        } catch (Exception e) {
            log.error("Connection test failed", e);
            return JdbcTestResult.builder()
                    .success(false)
                    .errorMessage(e.getMessage())
                    .build();
        } finally {
            if (connection != null) {
                try {
                    connection.close();
                } catch (Exception e) {
                    log.warn("Failed to close test connection", e);
                }
            }
        }
    }
}
