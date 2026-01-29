package work.pollochang.sqlconsole.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;
import work.pollochang.sqlconsole.service.ExternalDriverService;

@Component
@RequiredArgsConstructor
@Slf4j
public class JDBCDriverBootstrapListener implements ApplicationListener<ApplicationReadyEvent> {

    private final ExternalDriverService service;

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        log.info("Bootstrapping external JDBC drivers...");
        service.listLoadedDrivers().forEach(driver -> {
            try {
                service.loadExistingDriver(driver);
                log.info("Loaded driver: {}", driver.getDriverName());
            } catch (Exception e) {
                log.error("Failed to load driver: " + driver.getDriverName(), e);
                // Do not throw, continue loading others
            }
        });
    }
}
