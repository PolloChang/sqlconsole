package work.pollochang.sqlconsole.model.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class JdbcTestResult {
    private boolean success;
    private String databaseProductName;
    private String databaseVersion;
    private String errorMessage;
}
