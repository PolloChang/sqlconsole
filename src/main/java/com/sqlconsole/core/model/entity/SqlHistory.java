package com.sqlconsole.core.model.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.Data;

/** Represents a record of executed SQL history. */
@Entity
@Table(name = "sql_history")
@Data
public class SqlHistory {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private String executorName;
  private String dbName;

  @Column(length = 2000)
  private String sqlContent;

  private String status;
  private LocalDateTime executeTime;

  /** Default constructor. */
  public SqlHistory() {}

  /**
   * Constructs a new SqlHistory record.
   *
   * @param executorName the name of the user who executed the SQL
   * @param dbName the name of the database
   * @param sqlContent the SQL content
   * @param status the execution status
   */
  public SqlHistory(String executorName, String dbName, String sqlContent, String status) {
    this.executorName = executorName;
    this.dbName = dbName;
    this.sqlContent = sqlContent;
    this.status = status;
    this.executeTime = LocalDateTime.now();
  }
}
