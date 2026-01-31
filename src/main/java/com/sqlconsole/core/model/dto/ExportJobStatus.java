package com.sqlconsole.core.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExportJobStatus {
  private String jobId;
  private Status status;
  private int percentage;
  private String filePath;
  private String errorMessage;

  public enum Status {
    PENDING,
    PROCESSING,
    COMPLETED,
    FAILED
  }
}
