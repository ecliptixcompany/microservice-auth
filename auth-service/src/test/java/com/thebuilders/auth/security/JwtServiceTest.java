package com.thebuilders.auth.security;

import io.jsonwebtoken.ExpiredJwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
@DisplayName("JwtService Tests")
class JwtServiceTest {

    private JwtService jwtService;

    private static final String TEST_SECRET = "myTestSecretKeyThatIsAtLeast256BitsLongForHMACSHA256Algorithm";
    private static final String TEST_USER_ID = "123e4567-e89b-12d3-a456-426614174000";
    private static final String TEST_EMAIL = "test@example.com";
    private static final String TEST_ROLE = "USER";

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(TEST_SECRET);
        // Set expiration times using reflection or create a test constructor
        setFieldValue(jwtService, "accessTokenExpiration", 900000L); // 15 minutes
        setFieldValue(jwtService, "refreshTokenExpiration", 604800000L); // 7 days
    }

    @Test
    @DisplayName("Should generate valid access token")
    void shouldGenerateValidAccessToken() {
        // When
        String token = jwtService.generateAccessToken(TEST_USER_ID, TEST_EMAIL, TEST_ROLE);

        // Then
        assertThat(token).isNotNull();
        assertThat(token).isNotEmpty();
        assertThat(jwtService.isTokenValid(token)).isTrue();
    }

    @Test
    @DisplayName("Should generate valid refresh token")
    void shouldGenerateValidRefreshToken() {
        // When
        String token = jwtService.generateRefreshToken(TEST_USER_ID);

        // Then
        assertThat(token).isNotNull();
        assertThat(token).isNotEmpty();
        assertThat(jwtService.isTokenValid(token)).isTrue();
    }

    @Test
    @DisplayName("Should extract correct subject (userId) from token")
    void shouldExtractCorrectSubject() {
        // Given
        String token = jwtService.generateAccessToken(TEST_USER_ID, TEST_EMAIL, TEST_ROLE);

        // When
        String subject = jwtService.extractSubject(token);

        // Then
        assertThat(subject).isEqualTo(TEST_USER_ID);
    }

    @Test
    @DisplayName("Should extract correct email from token")
    void shouldExtractCorrectEmail() {
        // Given
        String token = jwtService.generateAccessToken(TEST_USER_ID, TEST_EMAIL, TEST_ROLE);

        // When
        String email = jwtService.extractEmail(token);

        // Then
        assertThat(email).isEqualTo(TEST_EMAIL);
    }

    @Test
    @DisplayName("Should extract correct role from token")
    void shouldExtractCorrectRole() {
        // Given
        String token = jwtService.generateAccessToken(TEST_USER_ID, TEST_EMAIL, TEST_ROLE);

        // When
        String role = jwtService.extractRole(token);

        // Then
        assertThat(role).isEqualTo(TEST_ROLE);
    }

    @Test
    @DisplayName("Should return false for invalid token")
    void shouldReturnFalseForInvalidToken() {
        // Given
        String invalidToken = "invalid.jwt.token";

        // When
        boolean isValid = jwtService.isTokenValid(invalidToken);

        // Then
        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("Should return false for expired token")
    void shouldReturnFalseForExpiredToken() {
        // Given - Create JWT service with very short expiration
        JwtService shortExpirationService = new JwtService(TEST_SECRET);
        setFieldValue(shortExpirationService, "accessTokenExpiration", 1L); // 1 millisecond
        setFieldValue(shortExpirationService, "refreshTokenExpiration", 1L);
        
        String token = shortExpirationService.generateAccessToken(TEST_USER_ID, TEST_EMAIL, TEST_ROLE);
        
        // Wait for token to expire
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // When
        boolean isValid = shortExpirationService.isTokenValid(token);

        // Then
        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("Should return false for tampered token")
    void shouldReturnFalseForTamperedToken() {
        // Given
        String token = jwtService.generateAccessToken(TEST_USER_ID, TEST_EMAIL, TEST_ROLE);
        String tamperedToken = token.substring(0, token.length() - 5) + "xxxxx";

        // When
        boolean isValid = jwtService.isTokenValid(tamperedToken);

        // Then
        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("Should return correct access token expiration")
    void shouldReturnCorrectAccessTokenExpiration() {
        // When
        long expiration = jwtService.getAccessTokenExpiration();

        // Then
        assertThat(expiration).isEqualTo(900000L);
    }

    @Test
    @DisplayName("Should extract valid expiration date from token")
    void shouldExtractValidExpirationDate() {
        // Given
        String token = jwtService.generateAccessToken(TEST_USER_ID, TEST_EMAIL, TEST_ROLE);

        // When
        java.util.Date expiration = jwtService.extractExpiration(token);

        // Then
        assertThat(expiration).isNotNull();
        assertThat(expiration).isAfter(new java.util.Date());
    }

    // Utility method to set private fields using reflection
    private void setFieldValue(Object object, String fieldName, Object value) {
        try {
            java.lang.reflect.Field field = object.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(object, value);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set field value", e);
        }
    }
}
