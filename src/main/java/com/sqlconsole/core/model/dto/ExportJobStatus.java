package com.sqlconsole.core.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


/**
 * Represents the status of a data export job.
 */
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
  private long createdTime;

  /**
   * The possible states of an export job.
   */
  public enum Status {
    /** Job is queued but not started. */
    PENDING,
    /** Job is currently running. */
    PROCESSING,
    /** Job completed successfully. */
    COMPLETED,
    /** Job failed. */
    FAILED
  }
}
