package ardaaydinkilinc.Cam_Sise.auth.service;

import ardaaydinkilinc.Cam_Sise.auth.domain.Role;
import ardaaydinkilinc.Cam_Sise.auth.domain.User;
import ardaaydinkilinc.Cam_Sise.auth.dto.LoginRequest;
import ardaaydinkilinc.Cam_Sise.auth.dto.LoginResponse;
import ardaaydinkilinc.Cam_Sise.auth.repository.UserRepository;
import ardaaydinkilinc.Cam_Sise.shared.exception.AuthenticationException;
import ardaaydinkilinc.Cam_Sise.shared.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService Tests")
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtUtil jwtUtil;
    @Mock private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private AuthService authService;

    private static final Long POOL_OPERATOR_ID = 1L;
    private static final String USERNAME = "staff01";
    private static final String RAW_PASSWORD = "sifre123";
    private static final String HASHED_PASSWORD = "$2a$10$hashedpassword";
    private static final String JWT_TOKEN = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.test";

    private User activeUser;

    @BeforeEach
    void setUp() {
        activeUser = User.register(POOL_OPERATOR_ID, USERNAME, HASHED_PASSWORD, "Ali Veli", Role.COMPANY_STAFF, null);
        activeUser.clearDomainEvents();
    }

    @Nested
    @DisplayName("login()")
    class Login {

        @Test
        @DisplayName("Geçerli kimlik bilgileriyle token döndürmeli")
        void shouldReturnTokenOnSuccessfulLogin() {
            when(userRepository.findByUsernameAndActiveTrue(USERNAME)).thenReturn(Optional.of(activeUser));
            when(passwordEncoder.matches(RAW_PASSWORD, HASHED_PASSWORD)).thenReturn(true);
            when(jwtUtil.generateToken(USERNAME, "COMPANY_STAFF", POOL_OPERATOR_ID)).thenReturn(JWT_TOKEN);

            LoginResponse response = authService.login(new LoginRequest(USERNAME, RAW_PASSWORD));

            assertThat(response.getToken()).isEqualTo(JWT_TOKEN);
            assertThat(response.getUsername()).isEqualTo(USERNAME);
            assertThat(response.getRole()).isEqualTo("COMPANY_STAFF");
        }

        @Test
        @DisplayName("Başarılı girişte UserLoggedIn eventi yayınlamalı")
        void shouldPublishUserLoggedInEvent() {
            when(userRepository.findByUsernameAndActiveTrue(USERNAME)).thenReturn(Optional.of(activeUser));
            when(passwordEncoder.matches(RAW_PASSWORD, HASHED_PASSWORD)).thenReturn(true);
            when(jwtUtil.generateToken(any(), any(), any())).thenReturn(JWT_TOKEN);

            authService.login(new LoginRequest(USERNAME, RAW_PASSWORD));

            verify(eventPublisher).publishEvent(any(Object.class));
        }

        @Test
        @DisplayName("Kullanıcı bulunamazsa AuthenticationException fırlatmalı")
        void shouldThrowWhenUserNotFound() {
            when(userRepository.findByUsernameAndActiveTrue(USERNAME)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.login(new LoginRequest(USERNAME, RAW_PASSWORD)))
                    .isInstanceOf(AuthenticationException.class);

            verify(passwordEncoder, never()).matches(any(), any());
        }

        @Test
        @DisplayName("Şifre yanlışsa AuthenticationException fırlatmalı")
        void shouldThrowWhenPasswordDoesNotMatch() {
            when(userRepository.findByUsernameAndActiveTrue(USERNAME)).thenReturn(Optional.of(activeUser));
            when(passwordEncoder.matches(RAW_PASSWORD, HASHED_PASSWORD)).thenReturn(false);

            assertThatThrownBy(() -> authService.login(new LoginRequest(USERNAME, RAW_PASSWORD)))
                    .isInstanceOf(AuthenticationException.class)
                    .hasMessageContaining("şifre");

            verify(jwtUtil, never()).generateToken(any(), any(), any());
        }
    }
}
