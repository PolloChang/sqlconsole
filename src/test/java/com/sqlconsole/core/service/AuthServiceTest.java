package com.sqlconsole.core.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import com.sqlconsole.core.model.entity.User;
import com.sqlconsole.core.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

  @InjectMocks private AuthService authService;

  @Mock private UserRepository userRepo;

  @Test
  @DisplayName("載入使用者 - 成功")
  void testLoadUserByUsername_Success() {
    // Arrange
    String username = "admin";
    User mockUser = new User();
    mockUser.setUsername(username);
    mockUser.setPassword("encodedPwd");
    mockUser.setRole("ROLE_ADMIN"); // 注意你的程式碼邏輯有 replace("ROLE_", "")

    when(userRepo.findByUsername(username)).thenReturn(Optional.of(mockUser));

    // Act
    UserDetails userDetails = authService.loadUserByUsername(username);

    // Assert
    assertNotNull(userDetails);
    assertEquals(username, userDetails.getUsername());
    assertEquals("encodedPwd", userDetails.getPassword());

    // 驗證 Role 是否正確處理 (Spring Security Builder 會自動加上 ROLE_ 前綴，但你的程式邏輯是移除它再塞進去)
    // 這裡檢查 Authorities 是否包含我們預期的權限
    assertTrue(
        userDetails.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")));
  }

  @Test
  @DisplayName("載入使用者 - 失敗 (使用者不存在)")
  void testLoadUserByUsername_NotFound() {
    // Arrange
    String username = "ghost";
    when(userRepo.findByUsername(username)).thenReturn(Optional.empty());

    // Act & Assert
    assertThrows(
        UsernameNotFoundException.class,
        () -> {
          authService.loadUserByUsername(username);
        });
  }
}
