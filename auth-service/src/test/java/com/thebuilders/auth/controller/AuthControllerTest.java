package com.thebuilders.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.thebuilders.auth.dto.*;
import com.thebuilders.auth.exception.AuthException;
import com.thebuilders.auth.exception.GlobalExceptionHandler;
import com.thebuilders.auth.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthController Tests")
class AuthControllerTest {

    private MockMvc mockMvc;

    @Mock
    private AuthService authService;

    @InjectMocks
    private AuthController authController;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        mockMvc = MockMvcBuilders.standaloneSetup(authController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Nested
    @DisplayName("Register Endpoint Tests")
    class RegisterEndpointTests {

        @Test
        @DisplayName("Should register user successfully")
        void shouldRegisterUserSuccessfully() throws Exception {
            // Given
            RegisterRequest request = RegisterRequest.builder()
                    .email("test@example.com")
                    .password("password123")
                    .firstName("John")
                    .lastName("Doe")
                    .build();

            AuthResponse response = AuthResponse.builder()
                    .accessToken("accessToken")
                    .refreshToken("refreshToken")
                    .expiresIn(900000L)
                    .build();

            when(authService.register(any(RegisterRequest.class))).thenReturn(response);

            // When/Then
            mockMvc.perform(post("/api/v1/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Registration successful"))
                    .andExpect(jsonPath("$.data.accessToken").value("accessToken"));
        }

        @Test
        @DisplayName("Should return 400 for invalid email format")
        void shouldReturn400ForInvalidEmailFormat() throws Exception {
            // Given
            RegisterRequest request = RegisterRequest.builder()
                    .email("invalid-email")
                    .password("password123")
                    .firstName("John")
                    .lastName("Doe")
                    .build();

            // When/Then
            mockMvc.perform(post("/api/v1/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 400 for short password")
        void shouldReturn400ForShortPassword() throws Exception {
            // Given
            RegisterRequest request = RegisterRequest.builder()
                    .email("test@example.com")
                    .password("short")
                    .firstName("John")
                    .lastName("Doe")
                    .build();

            // When/Then
            mockMvc.perform(post("/api/v1/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 400 for missing required fields")
        void shouldReturn400ForMissingRequiredFields() throws Exception {
            // Given
            RegisterRequest request = RegisterRequest.builder()
                    .email("test@example.com")
                    .build();

            // When/Then
            mockMvc.perform(post("/api/v1/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 400 when email already exists")
        void shouldReturn400WhenEmailAlreadyExists() throws Exception {
            // Given
            RegisterRequest request = RegisterRequest.builder()
                    .email("existing@example.com")
                    .password("password123")
                    .firstName("John")
                    .lastName("Doe")
                    .build();

            when(authService.register(any(RegisterRequest.class)))
                    .thenThrow(new AuthException("Email already registered"));

            // When/Then
            mockMvc.perform(post("/api/v1/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("Email already registered"));
        }
    }

    @Nested
    @DisplayName("Login Endpoint Tests")
    class LoginEndpointTests {

        @Test
        @DisplayName("Should login user successfully")
        void shouldLoginUserSuccessfully() throws Exception {
            // Given
            LoginRequest request = LoginRequest.builder()
                    .email("test@example.com")
                    .password("password123")
                    .build();

            AuthResponse response = AuthResponse.builder()
                    .accessToken("accessToken")
                    .refreshToken("refreshToken")
                    .expiresIn(900000L)
                    .build();

            when(authService.login(any(LoginRequest.class))).thenReturn(response);

            // When/Then
            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Login successful"))
                    .andExpect(jsonPath("$.data.accessToken").value("accessToken"));
        }

        @Test
        @DisplayName("Should return 400 for invalid credentials")
        void shouldReturn400ForInvalidCredentials() throws Exception {
            // Given
            LoginRequest request = LoginRequest.builder()
                    .email("test@example.com")
                    .password("wrongpassword")
                    .build();

            when(authService.login(any(LoginRequest.class)))
                    .thenThrow(new AuthException("Invalid email or password"));

            // When/Then
            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("Invalid email or password"));
        }
    }

    @Nested
    @DisplayName("Refresh Token Endpoint Tests")
    class RefreshTokenEndpointTests {

        @Test
        @DisplayName("Should refresh token successfully")
        void shouldRefreshTokenSuccessfully() throws Exception {
            // Given
            RefreshTokenRequest request = RefreshTokenRequest.builder()
                    .refreshToken("validRefreshToken")
                    .build();

            AuthResponse response = AuthResponse.builder()
                    .accessToken("newAccessToken")
                    .refreshToken("newRefreshToken")
                    .expiresIn(900000L)
                    .build();

            when(authService.refreshToken(any(RefreshTokenRequest.class))).thenReturn(response);

            // When/Then
            mockMvc.perform(post("/api/v1/auth/refresh")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.accessToken").value("newAccessToken"));
        }

        @Test
        @DisplayName("Should return 400 for invalid refresh token")
        void shouldReturn400ForInvalidRefreshToken() throws Exception {
            // Given
            RefreshTokenRequest request = RefreshTokenRequest.builder()
                    .refreshToken("invalidToken")
                    .build();

            when(authService.refreshToken(any(RefreshTokenRequest.class)))
                    .thenThrow(new AuthException("Invalid refresh token"));

            // When/Then
            mockMvc.perform(post("/api/v1/auth/refresh")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false));
        }
    }

    @Nested
    @DisplayName("Logout Endpoint Tests")
    class LogoutEndpointTests {

        @Test
        @DisplayName("Should logout successfully")
        void shouldLogoutSuccessfully() throws Exception {
            // Given
            RefreshTokenRequest request = RefreshTokenRequest.builder()
                    .refreshToken("validRefreshToken")
                    .build();

            doNothing().when(authService).logout(anyString());

            // When/Then
            mockMvc.perform(post("/api/v1/auth/logout")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Logged out successfully"));
        }
    }

    @Nested
    @DisplayName("Email Verification Endpoint Tests")
    class EmailVerificationEndpointTests {

        @Test
        @DisplayName("Should verify email successfully")
        void shouldVerifyEmailSuccessfully() throws Exception {
            // Given
            doNothing().when(authService).verifyEmail(anyString());

            // When/Then
            mockMvc.perform(get("/api/v1/auth/verify-email")
                            .param("token", "valid-verification-token"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Email verified successfully"));
        }

        @Test
        @DisplayName("Should return 400 for invalid verification token")
        void shouldReturn400ForInvalidVerificationToken() throws Exception {
            // Given
            doThrow(new AuthException("Invalid verification token"))
                    .when(authService).verifyEmail(anyString());

            // When/Then
            mockMvc.perform(get("/api/v1/auth/verify-email")
                            .param("token", "invalid-token"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false));
        }
    }
}
