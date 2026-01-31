package com.sqlconsole.core.service;

import com.sqlconsole.core.model.dto.SqlResult;
import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

public class LargeResultTest {

    @Test
    public void testLargeResultGeneration() {
        // Arrange
        int rowCount = 100000;
        List<String> columns = List.of("id", "name", "value", "timestamp");
        List<Map<String, Object>> rows = new ArrayList<>(rowCount);

        long startTime = System.currentTimeMillis();

        // Act: Simulate generating 100,000 rows
        for (int i = 0; i < rowCount; i++) {
            Map<String, Object> row = new HashMap<>();
            row.put("id", i);
            row.put("name", "Row " + i);
            row.put("value", Math.random());
            row.put("timestamp", System.currentTimeMillis());
            rows.add(row);
        }

        SqlResult result = new SqlResult("SUCCESS", "COMMITTED", "Query executed", columns, rows);

        long duration = System.currentTimeMillis() - startTime;
        System.out.println("Generated " + rowCount + " rows in " + duration + "ms");

        // Assert
        assertNotNull(result);
        assertEquals(rowCount, result.rows().size());
        assertEquals(4, result.columns().size());
        assertEquals("SUCCESS", result.status());

        // Verify first and last row integrity
        assertEquals(0, result.rows().get(0).get("id"));
        assertEquals(rowCount - 1, result.rows().get(rowCount - 1).get("id"));
    }
}
