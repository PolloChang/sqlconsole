package com.sqlconsole.core.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import com.sqlconsole.core.model.entity.DbConfig;
import com.sqlconsole.core.model.entity.User;
import com.sqlconsole.core.repository.DbConfigRepository;
import com.sqlconsole.core.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class DbConfigServiceTest {

  @Mock private DbConfigRepository dbConfigRepository;
  @Mock private UserRepository userRepository;
  @Mock private EncryptionService encryptionService;

  @Mock private SecurityContext securityContext;
  @Mock private Authentication authentication;

  @InjectMocks private DbConfigService dbConfigService;

  @BeforeEach
  void setUp() {
    SecurityContextHolder.setContext(securityContext);
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void getAllConfigs_AsAdmin_ShouldReturnAll() {
    // Arrange
    when(securityContext.getAuthentication()).thenReturn(authentication);
    when(authentication.getAuthorities())
        .thenReturn((List) Collections.singletonList(new SimpleGrantedAuthority(User.ROLE_ADMIN)));

    DbConfig config1 = new DbConfig();
    config1.setId(1L);
    config1.setName("DB1");
    config1.setDbUser("encUser");

    when(dbConfigRepository.findAll()).thenReturn(List.of(config1));
    when(encryptionService.decrypt("encUser")).thenReturn("user");

    // Act
    List<DbConfig> result = dbConfigService.getAllConfigs();

    // Assert
    assertEquals(1, result.size());
    assertEquals("DB1", result.get(0).getName());
    verify(dbConfigRepository).findAll();
  }

  @Test
  void getAllConfigs_AsUser_ShouldReturnOnlyAssigned() {
    // Arrange
    String username = "testuser";
    when(securityContext.getAuthentication()).thenReturn(authentication);
    when(authentication.getName()).thenReturn(username);
    when(authentication.getAuthorities())
        .thenReturn((List) Collections.singletonList(new SimpleGrantedAuthority(User.ROLE_USER)));

    DbConfig config1 = new DbConfig();
    config1.setId(1L);
    config1.setName("DB1");
    config1.setDbUser("encUser");

    User user = new User();
    user.setUsername(username);
    user.setAccessibleDatabases(Set.of(config1));

    when(userRepository.findByUsername(username)).thenReturn(Optional.of(user));
    when(encryptionService.decrypt("encUser")).thenReturn("user");

    // Act
    List<DbConfig> result = dbConfigService.getAllConfigs();

    // Assert
    assertEquals(1, result.size());
    assertEquals("DB1", result.get(0).getName());
    verify(dbConfigRepository, never()).findAll();
    verify(userRepository).findByUsername(username);
  }
}
