package work.pollochang.sqlconsole.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import work.pollochang.sqlconsole.config.SecurityConfig;
import work.pollochang.sqlconsole.service.ExternalDriverService;
import work.pollochang.sqlconsole.service.MavenDriverResolverService;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
    controllers = ExternalDriverController.class,
    excludeFilters = {
        @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = SecurityConfig.class)
    }
)
@AutoConfigureMockMvc(addFilters = false)
class ExternalDriverControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ExternalDriverService externalDriverService;

    @MockBean
    private MavenDriverResolverService mavenResolverService;

    @MockBean
    private work.pollochang.sqlconsole.service.ConnectionTestService connectionTestService;

    // Mocks required to satisfy the CommandLineRunner in SqlConsoleApplication
    @MockBean
    private work.pollochang.sqlconsole.repository.UserRepository userRepository;

    @MockBean
    private work.pollochang.sqlconsole.service.DbConfigService dbConfigService;

    @MockBean
    private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    @Test
    void testTestConnectionEndpoint() throws Exception {
        // Arrange
        work.pollochang.sqlconsole.model.dto.JdbcTestResult mockResult = work.pollochang.sqlconsole.model.dto.JdbcTestResult.builder()
                .success(true)
                .databaseProductName("MockDB")
                .databaseVersion("1.0")
                .build();

        when(connectionTestService.testConnection(any(), any())).thenReturn(mockResult);

        // Act & Assert
        mockMvc.perform(post("/api/drivers/test-connection")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"driverId\": 1, \"url\": \"jdbc:test\", \"username\": \"user\", \"password\": \"pass\"}"))
                .andExpect(status().isOk());

        verify(connectionTestService).testConnection(any(), any());
    }

    @Test
    void testMavenUpload_ShouldCallResolverAndService() throws Exception {
        String gav = "com.example:driver:1.0.0";
        List<Path> resolvedJars = Collections.singletonList(Paths.get("/tmp/driver.jar"));
        when(mavenResolverService.resolveArtifacts(gav)).thenReturn(resolvedJars);

        mockMvc.perform(post("/api/drivers/maven")
                .param("coords", gav))
                .andExpect(status().isOk());

        verify(mavenResolverService).resolveArtifacts(gav);
        verify(externalDriverService).registerDriver(any());
    }

    @Test
    void testFileUpload_ShouldSaveAndRegister() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "driver.jar", MediaType.APPLICATION_OCTET_STREAM_VALUE, "dummy content".getBytes());

        mockMvc.perform(multipart("/api/drivers/upload")
                .file(file))
                .andExpect(status().isOk());

        verify(externalDriverService).registerDriver(any());
    }

    @Test
    void testListDrivers() throws Exception {
        mockMvc.perform(get("/api/drivers"))
                .andExpect(status().isOk());

        verify(externalDriverService).listLoadedDrivers();
    }
}
