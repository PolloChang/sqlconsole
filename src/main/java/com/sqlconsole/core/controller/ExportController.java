package com.sqlconsole.core.controller;

import com.sqlconsole.core.model.dto.AnalyzeRequest;
import com.sqlconsole.core.model.dto.ExportJobStatus;
import com.sqlconsole.core.service.ExportService;
import java.io.File;
import java.security.Principal;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/export")
@RequiredArgsConstructor
public class ExportController {

  private final ExportService exportService;

  @PostMapping("/execute")
  public ResponseEntity<String> execute(@RequestBody AnalyzeRequest request, Principal principal) {
    String jobId = exportService.submitJob(request.dbId(), request.sql(), principal.getName());
    return ResponseEntity.ok(jobId);
  }

  @GetMapping("/status/{jobId}")
  public ResponseEntity<ExportJobStatus> getStatus(@PathVariable String jobId) {
    ExportJobStatus status = exportService.getStatus(jobId);
    if (status == null) {
      return ResponseEntity.notFound().build();
    }
    return ResponseEntity.ok(status);
  }

  @GetMapping("/download/{jobId}")
  public ResponseEntity<Resource> download(@PathVariable String jobId) {
    ExportJobStatus job = exportService.getStatus(jobId);
    if (job == null || job.getStatus() != ExportJobStatus.Status.COMPLETED) {
      return ResponseEntity.notFound().build();
    }

    File file = new File(job.getFilePath());
    if (!file.exists()) {
      return ResponseEntity.notFound().build();
    }

    Resource resource = new FileSystemResource(file);
    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"export.xlsx\"")
        .contentType(
            MediaType.parseMediaType(
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
        .body(resource);
  }
}
