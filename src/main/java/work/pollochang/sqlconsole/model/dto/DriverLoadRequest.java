package work.pollochang.sqlconsole.model.dto;

import lombok.Data;

import java.util.List;

@Data
public class DriverLoadRequest {
    private String jarPath; // Main jar or single jar
    private List<String> libraryPaths; // Optional dependencies
    private String driverClassName; // Optional, if null, try to detect
}
