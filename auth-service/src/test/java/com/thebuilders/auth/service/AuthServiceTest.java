package com.thebuilders.auth.service;

import com.thebuilders.auth.dto.AuthResponse;
import com.thebuilders.auth.dto.LoginRequest;
import com.thebuilders.auth.dto.RegisterRequest;
import com.thebuilders.auth.dto.RefreshTokenRequest;
import com.thebuilders.auth.entity.RefreshToken;
import com.thebuilders.auth.entity.User;
import com.thebuilders.auth.exception.AuthException;
import com.thebuilders.auth.repository.RefreshTokenRepository;
import com.thebuilders.auth.repository.UserRepository;
import com.thebuilders.auth.security.JwtService;
import com.thebuilders.common.enums.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService Tests")
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private EventPublisherService eventPublisherService;

    @InjectMocks
    private AuthService authService;

    @Captor
    private ArgumentCaptor<User> userCaptor;

    private RegisterRequest registerRequest;
    private LoginRequest loginRequest;
    private User testUser;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authService, "refreshTokenExpiration", 604800000L);

        registerRequest = RegisterRequest.builder()
                .email("test@example.com")
                .password("password123")
                .firstName("John")
                .lastName("Doe")
                .phoneNumber("+1234567890")
                .build();

        loginRequest = LoginRequest.builder()
                .email("test@example.com")
                .password("password123")
                .build();

        testUser = User.builder()
                .id(UUID.randomUUID())
                .email("test@example.com")
                .password("encodedPassword")
                .firstName("John")
                .lastName("Doe")
                .role(Role.USER)
                .isEmailVerified(false)
                .isActive(true)
                .build();
    }

    @Nested
    @DisplayName("Register Tests")
    class RegisterTests {

        @Test
        @DisplayName("Should register new user successfully")
        void shouldRegisterNewUserSuccessfully() {
            // Given
            when(userRepository.existsByEmail(anyString())).thenReturn(false);
            when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
            when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
                User user = invocation.getArgument(0);
                user.setId(UUID.randomUUID());
                return user;
            });
            when(jwtService.generateAccessToken(anyString(), anyString(), anyString()))
                    .thenReturn("accessToken");
            when(jwtService.generateRefreshToken(anyString()))
                    .thenReturn("refreshToken");
            when(jwtService.getAccessTokenExpiration()).thenReturn(900000L);
            when(refreshTokenRepository.save(any(RefreshToken.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            // When
            AuthResponse response = authService.register(registerRequest);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getAccessToken()).isEqualTo("accessToken");
            assertThat(response.getRefreshToken()).isEqualTo("refreshToken");

            verify(userRepository).save(userCaptor.capture());
            User savedUser = userCaptor.getValue();
            assertThat(savedUser.getEmail()).isEqualTo(registerRequest.getEmail());
            assertThat(savedUser.getRole()).isEqualTo(Role.USER); // Always USER, not from request
            assertThat(savedUser.getEmailVerificationToken()).isNotNull();
        }

        @Test
        @DisplayName("Should throw exception when email already exists")
        void shouldThrowExceptionWhenEmailExists() {
            // Given
            when(userRepository.existsByEmail(anyString())).thenReturn(true);

            // When/Then
            assertThatThrownBy(() -> authService.register(registerRequest))
                    .isInstanceOf(AuthException.class)
                    .hasMessage("Email already registered");

            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should always assign USER role regardless of request")
        void shouldAlwaysAssignUserRole() {
            // Given - Even though request might try to set ADMIN (which is now impossible)
            when(userRepository.existsByEmail(anyString())).thenReturn(false);
            when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
            when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
                User user = invocation.getArgument(0);
                user.setId(UUID.randomUUID());
                return user;
            });
            when(jwtService.generateAccessToken(anyString(), anyString(), anyString()))
                    .thenReturn("accessToken");
            when(jwtService.generateRefreshToken(anyString()))
                    .thenReturn("refreshToken");
            when(jwtService.getAccessTokenExpiration()).thenReturn(900000L);
            when(refreshTokenRepository.save(any(RefreshToken.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            // When
            authService.register(registerRequest);

            // Then
            verify(userRepository).save(userCaptor.capture());
            User savedUser = userCaptor.getValue();
            assertThat(savedUser.getRole()).isEqualTo(Role.USER);
        }

        @Test
        @DisplayName("Should publish user registered event")
        void shouldPublishUserRegisteredEvent() {
            // Given
            when(userRepository.existsByEmail(anyString())).thenReturn(false);
            when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
            when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
                User user = invocation.getArgument(0);
                user.setId(UUID.randomUUID());
                return user;
            });
            when(jwtService.generateAccessToken(anyString(), anyString(), anyString()))
                    .thenReturn("accessToken");
            when(jwtService.generateRefreshToken(anyString()))
                    .thenReturn("refreshToken");
            when(jwtService.getAccessTokenExpiration()).thenReturn(900000L);
            when(refreshTokenRepository.save(any(RefreshToken.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            // When
            authService.register(registerRequest);

            // Then
            verify(eventPublisherService).publishUserRegisteredEvent(any());
        }
    }

    @Nested
    @DisplayName("Login Tests")
    class LoginTests {

        @Test
        @DisplayName("Should login user successfully")
        void shouldLoginUserSuccessfully() {
            // Given
            when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(testUser));
            when(jwtService.generateAccessToken(anyString(), anyString(), anyString()))
                    .thenReturn("accessToken");
            when(jwtService.generateRefreshToken(anyString()))
                    .thenReturn("refreshToken");
            when(jwtService.getAccessTokenExpiration()).thenReturn(900000L);
            when(refreshTokenRepository.save(any(RefreshToken.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            // When
            AuthResponse response = authService.login(loginRequest);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getAccessToken()).isEqualTo("accessToken");
            verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        }

        @Test
        @DisplayName("Should throw exception for invalid credentials")
        void shouldThrowExceptionForInvalidCredentials() {
            // Given
            when(authenticationManager.authenticate(any()))
                    .thenThrow(new BadCredentialsException("Bad credentials"));

            // When/Then
            assertThatThrownBy(() -> authService.login(loginRequest))
                    .isInstanceOf(AuthException.class)
                    .hasMessage("Invalid email or password");
        }

        @Test
        @DisplayName("Should throw exception for deactivated account")
        void shouldThrowExceptionForDeactivatedAccount() {
            // Given
            testUser.setActive(false);
            when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(testUser));

            // When/Then
            assertThatThrownBy(() -> authService.login(loginRequest))
                    .isInstanceOf(AuthException.class)
                    .hasMessage("Account is deactivated");
        }
    }

    @Nested
    @DisplayName("Refresh Token Tests")
    class RefreshTokenTests {

        @Test
        @DisplayName("Should refresh token successfully")
        void shouldRefreshTokenSuccessfully() {
            // Given
            RefreshToken refreshToken = RefreshToken.builder()
                    .id(UUID.randomUUID())
                    .token("validRefreshToken")
                    .user(testUser)
                    .expiresAt(LocalDateTime.now().plusDays(7))
                    .isRevoked(false)
                    .build();

            RefreshTokenRequest request = RefreshTokenRequest.builder()
                    .refreshToken("validRefreshToken")
                    .build();

            when(refreshTokenRepository.findByToken(anyString())).thenReturn(Optional.of(refreshToken));
            when(jwtService.generateAccessToken(anyString(), anyString(), anyString()))
                    .thenReturn("newAccessToken");
            when(jwtService.generateRefreshToken(anyString()))
                    .thenReturn("newRefreshToken");
            when(jwtService.getAccessTokenExpiration()).thenReturn(900000L);
            when(refreshTokenRepository.save(any(RefreshToken.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            // When
            AuthResponse response = authService.refreshToken(request);

            // Then
            assertThat(response.getAccessToken()).isEqualTo("newAccessToken");
            assertThat(response.getRefreshToken()).isEqualTo("newRefreshToken");
            assertThat(refreshToken.isRevoked()).isTrue(); // Old token should be revoked
        }

        @Test
        @DisplayName("Should throw exception for invalid refresh token")
        void shouldThrowExceptionForInvalidRefreshToken() {
            // Given
            RefreshTokenRequest request = RefreshTokenRequest.builder()
                    .refreshToken("invalidToken")
                    .build();

            when(refreshTokenRepository.findByToken(anyString())).thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> authService.refreshToken(request))
                    .isInstanceOf(AuthException.class)
                    .hasMessage("Invalid refresh token");
        }

        @Test
        @DisplayName("Should throw exception for expired refresh token")
        void shouldThrowExceptionForExpiredRefreshToken() {
            // Given
            RefreshToken expiredToken = RefreshToken.builder()
                    .id(UUID.randomUUID())
                    .token("expiredToken")
                    .user(testUser)
                    .expiresAt(LocalDateTime.now().minusDays(1)) // Expired
                    .isRevoked(false)
                    .build();

            RefreshTokenRequest request = RefreshTokenRequest.builder()
                    .refreshToken("expiredToken")
                    .build();

            when(refreshTokenRepository.findByToken(anyString())).thenReturn(Optional.of(expiredToken));

            // When/Then
            assertThatThrownBy(() -> authService.refreshToken(request))
                    .isInstanceOf(AuthException.class)
                    .hasMessage("Refresh token is expired or revoked");
        }
    }

    @Nested
    @DisplayName("Email Verification Tests")
    class EmailVerificationTests {

        @Test
        @DisplayName("Should verify email successfully")
        void shouldVerifyEmailSuccessfully() {
            // Given
            String verificationToken = "valid-verification-token";
            testUser.setEmailVerificationToken(verificationToken);
            testUser.setEmailVerified(false);

            when(userRepository.findByEmailVerificationToken(verificationToken))
                    .thenReturn(Optional.of(testUser));
            when(userRepository.save(any(User.class))).thenReturn(testUser);

            // When
            authService.verifyEmail(verificationToken);

            // Then
            verify(userRepository).save(userCaptor.capture());
            User savedUser = userCaptor.getValue();
            assertThat(savedUser.isEmailVerified()).isTrue();
            assertThat(savedUser.getEmailVerificationToken()).isNull();
        }

        @Test
        @DisplayName("Should throw exception for invalid verification token")
        void shouldThrowExceptionForInvalidVerificationToken() {
            // Given
            when(userRepository.findByEmailVerificationToken(anyString()))
                    .thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> authService.verifyEmail("invalid-token"))
                    .isInstanceOf(AuthException.class)
                    .hasMessage("Invalid verification token");
        }
    }

    @Nested
    @DisplayName("Password Reset Tests")
    class PasswordResetTests {

        @Test
        @DisplayName("Should handle forgot password for existing user")
        void shouldHandleForgotPasswordForExistingUser() {
            // Given
            when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(testUser));
            when(userRepository.save(any(User.class))).thenReturn(testUser);

            var request = new com.thebuilders.auth.dto.ForgotPasswordRequest();
            ReflectionTestUtils.setField(request, "email", "test@example.com");

            // When
            authService.forgotPassword(request);

            // Then
            verify(userRepository).save(userCaptor.capture());
            User savedUser = userCaptor.getValue();
            assertThat(savedUser.getPasswordResetToken()).isNotNull();
            assertThat(savedUser.getPasswordResetTokenExpiry()).isNotNull();
            verify(eventPublisherService).publishPasswordResetEvent(any());
        }

        @Test
        @DisplayName("Should silently handle forgot password for non-existing user")
        void shouldSilentlyHandleForgotPasswordForNonExistingUser() {
            // Given - This is intentional security behavior
            when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());

            var request = new com.thebuilders.auth.dto.ForgotPasswordRequest();
            ReflectionTestUtils.setField(request, "email", "nonexistent@example.com");

            // When
            authService.forgotPassword(request);

            // Then - No exception, no save, no event (silent failure for security)
            verify(userRepository, never()).save(any());
            verify(eventPublisherService, never()).publishPasswordResetEvent(any());
        }
    }

    @Nested
    @DisplayName("Logout Tests")
    class LogoutTests {

        @Test
        @DisplayName("Should logout and revoke refresh token")
        void shouldLogoutAndRevokeRefreshToken() {
            // Given
            RefreshToken refreshToken = RefreshToken.builder()
                    .id(UUID.randomUUID())
                    .token("validRefreshToken")
                    .user(testUser)
                    .isRevoked(false)
                    .build();

            when(refreshTokenRepository.findByToken("validRefreshToken"))
                    .thenReturn(Optional.of(refreshToken));
            when(refreshTokenRepository.save(any(RefreshToken.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            // When
            authService.logout("validRefreshToken");

            // Then
            verify(refreshTokenRepository).save(any(RefreshToken.class));
            assertThat(refreshToken.isRevoked()).isTrue();
        }
    }
}
