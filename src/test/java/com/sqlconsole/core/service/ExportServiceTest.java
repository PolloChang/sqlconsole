package com.sqlconsole.core.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.sqlconsole.core.model.dto.ExportJobStatus;
import java.io.File;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.util.Map;
import java.util.function.Consumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ExportServiceTest {

  @Mock private SqlExecutorService sqlExecutorService;

  @InjectMocks private ExportService exportService;

  @BeforeEach
  void setUp() {
    ReflectionTestUtils.setField(exportService, "self", exportService);
  }

  @Test
  void testExecuteBackgroundExport_Success() throws Exception {
    String jobId = "test-job-id";
    Long dbId = 1L;
    String sql = "SELECT * FROM test";
    String username = "user";

    // Setup Job in store
    ExportJobStatus job =
        ExportJobStatus.builder().jobId(jobId).status(ExportJobStatus.Status.PENDING).build();
    Map<String, ExportJobStatus> jobStore =
        (Map<String, ExportJobStatus>) ReflectionTestUtils.getField(exportService, "jobStore");
    jobStore.put(jobId, job);

    // Mock ResultSet
    ResultSet rs = mock(ResultSet.class);
    ResultSetMetaData meta = mock(ResultSetMetaData.class);
    when(rs.getMetaData()).thenReturn(meta);
    when(meta.getColumnCount()).thenReturn(2);
    when(meta.getColumnLabel(1)).thenReturn("Col1");
    when(meta.getColumnLabel(2)).thenReturn("Col2");

    // Simulate 2 rows
    when(rs.next()).thenReturn(true, true, false);
    when(rs.getObject(1)).thenReturn("Val1", "Val3");
    when(rs.getObject(2)).thenReturn("Val2", "Val4");

    // Mock streamQuery to execute consumer
    doAnswer(
            invocation -> {
              Consumer<ResultSet> consumer = invocation.getArgument(4);
              consumer.accept(rs);
              return null;
            })
        .when(sqlExecutorService)
        .streamQuery(eq(dbId), eq(sql), eq(username), eq("ROLE_USER"), any());

    // Execute
    exportService.executeBackgroundExport(jobId, dbId, sql, username);

    // Verify
    ExportJobStatus status = exportService.getStatus(jobId);
    assertEquals(ExportJobStatus.Status.COMPLETED, status.getStatus());
    assertEquals(100, status.getPercentage());
    assertNotNull(status.getFilePath());

    File file = new File(status.getFilePath());
    assertTrue(file.exists());
    assertTrue(file.length() > 0);

    // Cleanup
    file.delete();
  }
}
