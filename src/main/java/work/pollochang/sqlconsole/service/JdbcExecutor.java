package work.pollochang.sqlconsole.service;

import org.springframework.stereotype.Component;
import work.pollochang.sqlconsole.model.dto.SqlResult;

import java.sql.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 負責單純的 JDBC 執行與結果集轉換。
 * 讓 Service 層專注於流程控制，而非 JDBC API 細節。
 */
@Component
public class JdbcExecutor {

    public SqlResult executeSql(Connection conn, String sql) throws SQLException {
        String status = "SUCCESS";
        String msg;
        List<String> columns = new ArrayList<>();
        List<Map<String, Object>> rows = new ArrayList<>();

        // 去除結尾分號
        String executableSql = sql.trim();
        if (executableSql.endsWith(";")) {
            executableSql = executableSql.substring(0, executableSql.length() - 1);
        }

        try (Statement stmt = conn.createStatement()) {
            boolean hasResultSet = stmt.execute(executableSql);
            if (hasResultSet) {
                try (ResultSet rs = stmt.getResultSet()) {
                    ResultSetMetaData meta = rs.getMetaData();
                    int colCount = meta.getColumnCount();
                    for (int i = 1; i <= colCount; i++) columns.add(meta.getColumnLabel(i));

                    while (rs.next()) {
                        Map<String, Object> row = new LinkedHashMap<>();
                        for (String col : columns) row.put(col, rs.getObject(col));
                        rows.add(row);
                    }
                    msg = "Query returned " + rows.size() + " rows.";
                }
            } else {
                msg = "Affected rows: " + stmt.getUpdateCount();
            }
        }
        return new SqlResult(status, null, msg, columns, rows);
    }
}