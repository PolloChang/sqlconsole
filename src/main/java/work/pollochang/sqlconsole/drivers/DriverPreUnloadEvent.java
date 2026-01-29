package work.pollochang.sqlconsole.drivers;

import org.springframework.context.ApplicationEvent;

public class DriverPreUnloadEvent extends ApplicationEvent {
    private final Long driverId;

    public DriverPreUnloadEvent(Object source, Long driverId) {
        super(source);
        this.driverId = driverId;
    }

    public Long getDriverId() {
        return driverId;
    }
}
