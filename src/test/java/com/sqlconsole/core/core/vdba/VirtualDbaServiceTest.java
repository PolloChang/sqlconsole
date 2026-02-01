package com.sqlconsole.core.core.vdba;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.sqlconsole.core.model.entity.DbConfig;
import com.sqlconsole.core.model.enums.DbType;
import com.sqlconsole.core.service.DbConfigService;
import java.sql.Connection;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class VirtualDbaServiceTest {

  @Mock private DbConfigService dbConfigService;
  @Mock private VirtualDbaProvider provider;
  @Mock private Connection connection;

  private VirtualDbaService service;

  @BeforeEach
  void setUp() {
    when(provider.getProviderType()).thenReturn("POSTGRESQL");
    service = new VirtualDbaService(dbConfigService, List.of(provider));
    service.init();
  }

  @Test
  void testDiagnose_Success() throws Exception {
    Long dbId = 1L;
    String sql = "SELECT 1";
    DbConfig config = new DbConfig();
    config.setDbType(DbType.POSTGRESQL);

    when(dbConfigService.getConfigById(dbId)).thenReturn(config);
    when(dbConfigService.createConnection(config)).thenReturn(connection);
    when(provider.explain(connection, sql)).thenReturn(new ExecutionPlan("{}", "JSON"));
    when(provider.getSuggestions(connection, sql)).thenReturn(List.of());

    VirtualDbaReport report = service.diagnose(dbId, sql);

    assertNotNull(report);
    assertEquals("{}", report.plan().rawJson());
    verify(provider).explain(connection, sql);
  }

  @Test
  void testDiagnose_Fallback() {
    Long dbId = 2L;
    DbConfig config = new DbConfig();
    config.setDbType(DbType.MYSQL); // Unsupported in this setup

    when(dbConfigService.getConfigById(dbId)).thenReturn(config);

    VirtualDbaReport report = service.diagnose(dbId, "SELECT 1");

    assertNotNull(report);
    assertTrue(report.suggestions().stream().anyMatch(s -> s.message().contains("not supported")));
  }
}
