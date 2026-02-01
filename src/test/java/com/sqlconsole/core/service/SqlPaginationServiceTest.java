package com.sqlconsole.core.service;

import static org.junit.jupiter.api.Assertions.*;

import com.sqlconsole.core.model.enums.DbType;
import org.junit.jupiter.api.Test;

public class SqlPaginationServiceTest {

  private final SqlPaginationService service = new SqlPaginationService();

  @Test
  public void testMysqlPagination() {
    String sql = "SELECT * FROM users";
    String result = service.applyPagination(sql, DbType.MYSQL, 1, 10);
    System.out.println("MySQL Page 1: " + result);
    assertTrue(result.toUpperCase().contains("LIMIT 10"), "Result: " + result);
    assertFalse(result.toUpperCase().contains("OFFSET"), "Result: " + result);

    result = service.applyPagination(sql, DbType.MYSQL, 2, 10);
    System.out.println("MySQL Page 2: " + result);
    assertTrue(result.toUpperCase().contains("LIMIT 10"), "Result: " + result);
    assertTrue(result.toUpperCase().contains("OFFSET 10"), "Result: " + result);
  }

  @Test
  public void testPostgresPagination() {
    String sql = "SELECT * FROM users WHERE active = true";
    String result = service.applyPagination(sql, DbType.POSTGRESQL, 2, 20);
    System.out.println("Postgres Page 2: " + result);
    assertTrue(result.toUpperCase().contains("LIMIT 20"), "Result: " + result);
    assertTrue(result.toUpperCase().contains("OFFSET 20"), "Result: " + result);
    assertTrue(result.contains("WHERE active = true"), "Result: " + result);
  }

  @Test
  public void testOraclePagination() {
    String sql = "SELECT * FROM users ORDER BY id";
    String result = service.applyPagination(sql, DbType.ORACLE, 2, 10);
    System.out.println("Oracle Page 2: " + result);
    // Expect OFFSET 10 ROWS FETCH NEXT 10 ROWS ONLY
    assertTrue(result.toUpperCase().contains("OFFSET 10 ROWS"), "Result: " + result);
    // JSqlParser might use different wording for FETCH
    assertTrue(
        result.toUpperCase().contains("FETCH NEXT 10 ROWS ONLY")
            || result.toUpperCase().contains("FETCH FIRST 10 ROWS ONLY"),
        "Result: " + result);
  }

  @Test
  public void testOraclePagination_NoOrderBy() {
    String sql = "SELECT * FROM users";
    // Should return original SQL because of fallback due to exception
    String result = service.applyPagination(sql, DbType.ORACLE, 2, 10);
    assertEquals(sql, result);
  }

  @Test
  public void testSqlServerPagination() {
    String sql = "SELECT * FROM users ORDER BY id";
    String result = service.applyPagination(sql, DbType.MSSQL, 3, 5);
    System.out.println("MSSQL Page 3: " + result);
    // Expect OFFSET 10 ROWS FETCH NEXT 5 ROWS ONLY (page 3 -> offset 10)
    assertTrue(result.toUpperCase().contains("OFFSET 10 ROWS"), "Result: " + result);
    assertTrue(
        result.toUpperCase().contains("FETCH NEXT 5 ROWS ONLY")
            || result.toUpperCase().contains("FETCH FIRST 5 ROWS ONLY"),
        "Result: " + result);
  }

  @Test
  public void testNonSelectQuery() {
    String sql = "UPDATE users SET active = false";
    String result = service.applyPagination(sql, DbType.MYSQL, 1, 10);
    assertEquals(sql, result);
  }

  @Test
  public void testComplexQueryFallback() {
    // Union often represented as SetOperationList, not PlainSelect in SelectBody
    String sql = "SELECT * FROM t1 UNION SELECT * FROM t2";
    String result = service.applyPagination(sql, DbType.MYSQL, 1, 10);
    // Current simple implementation might fallback if it checks for PlainSelect
    // Or JSqlParser might parse it as SetOperationList
    // Our service checks `if (!(select.getSelectBody() instanceof PlainSelect))`
    assertEquals(sql, result);
  }
}
