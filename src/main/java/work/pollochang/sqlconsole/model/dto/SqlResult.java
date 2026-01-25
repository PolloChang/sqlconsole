package work.pollochang.sqlconsole.model.dto;

import java.util.List;
import java.util.Map;

public record SqlResult(
        String status,   // SUCCESS, ERROR, PENDING
        String txStatus, // COMMITTED, UNCOMMIT
        String message,
        List<String> columns,
        List<Map<String, Object>> rows
) {}
