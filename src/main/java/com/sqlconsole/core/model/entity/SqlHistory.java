package com.sqlconsole.core.model.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.Data;

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

  public SqlHistory() {}

  public SqlHistory(String executorName, String dbName, String sqlContent, String status) {
    this.executorName = executorName;
    this.dbName = dbName;
    this.sqlContent = sqlContent;
    this.status = status;
    this.executeTime = LocalDateTime.now();
  }
}
