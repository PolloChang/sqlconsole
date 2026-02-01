package com.sqlconsole.core.service;

import com.sqlconsole.core.model.dto.ExportJobStatus;
import com.sqlconsole.core.model.dto.ExportJobStatus.Status;
import java.io.File;
import java.io.FileOutputStream;
import java.sql.ResultSetMetaData;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class ExportService {

  private final SqlExecutorService sqlExecutorService;
  private final Map<String, ExportJobStatus> jobStore = new ConcurrentHashMap<>();
  private final Semaphore ioPermits = new Semaphore(20);

  @Autowired @Lazy private ExportService self;

  public String submitJob(Long dbId, String sql, String username) {
    String jobId = UUID.randomUUID().toString();
    ExportJobStatus job =
        ExportJobStatus.builder()
            .jobId(jobId)
            .status(Status.PENDING)
            .percentage(0)
            .createdTime(System.currentTimeMillis())
            .build();
    jobStore.put(jobId, job);

    // Call async method via self-proxy to ensure @Async works
    self.executeBackgroundExport(jobId, dbId, sql, username);

    return jobId;
  }

  @Async("exportTaskExecutor")
  public void executeBackgroundExport(String jobId, Long dbId, String sql, String username) {
    ExportJobStatus job = jobStore.get(jobId);
    if (job == null) return;

    long startTime = System.currentTimeMillis();
    log.info("Export started [JobId: {}, User: {}]", jobId, username);

    job.setStatus(Status.PROCESSING);

    File tempFile = null;
    SXSSFWorkbook workbook = null;

    try {
      ioPermits.acquire();

      workbook = new SXSSFWorkbook(100); // Keep 100 rows in memory
      Sheet sheet = workbook.createSheet("Export Data");

      // Pass user role as ROLE_USER to enforce permission checks
      sqlExecutorService.streamQuery(
          dbId,
          sql,
          username,
          "ROLE_USER",
          rs -> {
            try {
              ResultSetMetaData meta = rs.getMetaData();
              int colCount = meta.getColumnCount();

              // Header
              Row headerRow = sheet.createRow(0);
              for (int i = 1; i <= colCount; i++) {
                Cell cell = headerRow.createCell(i - 1);
                cell.setCellValue(meta.getColumnLabel(i));
              }

              int rowIndex = 1;
              while (rs.next()) {
                Row row = sheet.createRow(rowIndex++);
                for (int i = 1; i <= colCount; i++) {
                  Cell cell = row.createCell(i - 1);
                  Object val = rs.getObject(i);
                  if (val != null) {
                    cell.setCellValue(val.toString());
                  }
                }
              }
            } catch (Exception e) {
              throw new RuntimeException(e);
            }
          });

      // Write to temp file
      tempFile = File.createTempFile("export_" + jobId, ".xlsx");
      try (FileOutputStream out = new FileOutputStream(tempFile)) {
        workbook.write(out);
      }

      job.setFilePath(tempFile.getAbsolutePath());
      job.setStatus(Status.COMPLETED);
      job.setPercentage(100);

      long duration = System.currentTimeMillis() - startTime;
      log.info(
          "Export completed [JobId: {}, Duration: {}ms, SQL: {}]",
          jobId,
          duration,
          truncateSql(sql));

    } catch (Exception e) {
      log.error("Export failed [JobId: {}]", jobId, e);
      job.setStatus(Status.FAILED);
      job.setErrorMessage(e.getMessage());
      if (tempFile != null && tempFile.exists()) {
        tempFile.delete();
      }
    } finally {
      ioPermits.release();
      if (workbook != null) {
        workbook.dispose();
        try {
          workbook.close();
        } catch (Exception e) {
          log.warn("Failed to close workbook", e);
        }
      }
    }
  }

  @Scheduled(fixedRate = 3600000) // Every hour
  public void cleanUpOldJobs() {
    long cutoff = System.currentTimeMillis() - 86400000; // 24 hours
    jobStore.entrySet().removeIf(entry -> {
      ExportJobStatus job = entry.getValue();
      if (job.getCreatedTime() < cutoff) {
        if (job.getFilePath() != null) {
          File file = new File(job.getFilePath());
          if (file.exists()) {
            file.delete();
          }
        }
        return true;
      }
      return false;
    });
  }

  public ExportJobStatus getStatus(String jobId) {
    return jobStore.get(jobId);
  }

  private String truncateSql(String sql) {
    if (sql == null) return "";
    return sql.length() > 100 ? sql.substring(0, 100) + "..." : sql;
  }
}
