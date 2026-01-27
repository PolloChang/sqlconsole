package work.pollochang.sqlconsole.model.dto;

import lombok.Data;

@Data
public class DriverLoadRequest {
    private String jarPath;
    private String driverClassName; // Optional, if null, try to detect
}
