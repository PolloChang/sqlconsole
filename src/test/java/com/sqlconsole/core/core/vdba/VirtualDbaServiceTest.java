package com.sqlconsole.core.core.vdba;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.sqlconsole.core.core.vdba.analysis.AnalysisResult;
import com.sqlconsole.core.core.vdba.analysis.ExecutionPlanAnalyzer;
import com.sqlconsole.core.model.entity.DbConfig;
import com.sqlconsole.core.model.enums.DbType;
import com.sqlconsole.core.service.DbConfigService;
import java.sql.Connection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class VirtualDbaServiceTest {

  @Mock private DbConfigService dbConfigService;
  @Mock private VirtualDbaProvider provider;
  @Mock private ExecutionPlanAnalyzer analyzer;
  @Mock private Connection connection;

  private VirtualDbaService service;

  @BeforeEach
  void setUp() {
    when(provider.getProviderType()).thenReturn("POSTGRESQL");
    service = new VirtualDbaService(dbConfigService, List.of(provider), analyzer);
    service.init();
  }

  @Test
  void testDiagnose_Success() throws Exception {
    Long dbId = 1L;
    String sql = "SELECT 1";
    DbConfig config = new DbConfig();
    config.setDbType(DbType.POSTGRESQL);

    UnifiedExecutionNode normalizedPlan =
        new UnifiedExecutionNode("Root", 0.0, 0.0, 0.0, List.of(), Collections.emptyMap());

    when(dbConfigService.getConfigById(dbId)).thenReturn(config);
    when(dbConfigService.createConnection(config)).thenReturn(connection);
    when(provider.explain(connection, sql)).thenReturn(new ExecutionPlan("{}", "JSON"));
    when(provider.normalize(anyString())).thenReturn(normalizedPlan);
    when(analyzer.analyze(normalizedPlan))
        .thenReturn(new AnalysisResult(normalizedPlan, List.of()));
    when(provider.getSuggestions(eq(connection), eq(sql), any(UnifiedExecutionNode.class)))
        .thenReturn(mock(List.class)); // Return a mock list to avoid modifying immutable list issues if any

    CompletableFuture<VirtualDbaReport> future = service.diagnose(dbId, sql);
    VirtualDbaReport report = future.get();

    assertNotNull(report);
    assertEquals("{}", report.plan().rawJson());
    verify(provider).explain(connection, sql);
    verify(analyzer).analyze(normalizedPlan);
    // Verify rollback called
    verify(connection).rollback();
  }

  @Test
  void testDiagnose_Fallback() throws Exception {
    Long dbId = 2L;
    DbConfig config = new DbConfig();
    config.setDbType(DbType.MYSQL); // Unsupported in this setup

    when(dbConfigService.getConfigById(dbId)).thenReturn(config);

    CompletableFuture<VirtualDbaReport> future = service.diagnose(dbId, "SELECT 1");
    VirtualDbaReport report = future.get();

    assertNotNull(report);
    assertTrue(report.suggestions().stream().anyMatch(s -> s.message().contains("not supported")));
  }
}
