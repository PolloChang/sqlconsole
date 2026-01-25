package work.pollochang.sqlconsole.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import work.pollochang.sqlconsole.model.entity.User;
import work.pollochang.sqlconsole.repository.UserRepository;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @InjectMocks
    private AuthService authService;

    @Mock
    private UserRepository userRepo;

    @Test
    void testLoadUserByUsername_Success() {
        // Arrange
        User user = new User();
        user.setUsername("admin");
        user.setPassword("hashed_pw");
        user.setRole("ROLE_ADMIN");

        when(userRepo.findByUsername("admin")).thenReturn(Optional.of(user));

        // Act
        UserDetails userDetails = authService.loadUserByUsername("admin");

        // Assert
        assertEquals("admin", userDetails.getUsername());
        assertEquals("hashed_pw", userDetails.getPassword());
        // 驗證 ROLE_ 首碼是否被正確移除或處理 (Spring Security builder 會自動加回 ROLE_ 前綴)
        // 這裡 builder.roles("ADMIN") 會產生 Authority "ROLE_ADMIN"
        assertTrue(userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")));
    }

    @Test
    void testLoadUserByUsername_NotFound() {
        when(userRepo.findByUsername("unknown")).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class, () -> {
            authService.loadUserByUsername("unknown");
        });
    }

    @Test
    void testPasswordEncoderBean() {
        PasswordEncoder encoder = authService.passwordEncoder();
        assertNotNull(encoder);
    }
}