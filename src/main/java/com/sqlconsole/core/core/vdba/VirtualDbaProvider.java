package com.sqlconsole.core.core.vdba;

import java.sql.Connection;
import java.util.List;

/** Interface for Virtual DBA providers. */
public interface VirtualDbaProvider {
  /**
   * Returns the database type this provider supports (e.g., "POSTGRESQL").
   *
   * @return The supported database type.
   */
  String getProviderType();

  /**
   * Generates an execution plan for the given SQL.
   *
   * @param conn The database connection.
   * @param sql The SQL query.
   * @return The execution plan.
   */
  ExecutionPlan explain(Connection conn, String sql);

  /**
   * Generates optimization suggestions for the given SQL.
   *
   * @param conn The database connection.
   * @param sql The SQL query.
   * @return A list of suggestions.
   */
  List<DbaSuggestion> getSuggestions(Connection conn, String sql);
}
