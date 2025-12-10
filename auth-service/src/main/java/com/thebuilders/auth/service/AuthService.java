package com.thebuilders.auth.service;

import com.thebuilders.auth.dto.*;
import com.thebuilders.auth.entity.RefreshToken;
import com.thebuilders.auth.entity.User;
import com.thebuilders.auth.exception.AuthException;
import com.thebuilders.auth.repository.RefreshTokenRepository;
import com.thebuilders.auth.repository.UserRepository;
import com.thebuilders.auth.security.JwtService;
import com.thebuilders.common.enums.Role;
import com.thebuilders.common.event.PasswordResetEvent;
import com.thebuilders.common.event.UserRegisteredEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final EventPublisherService eventPublisherService;
    private final TokenBlacklistService tokenBlacklistService;

    @Value("${jwt.refresh-token-expiration}")
    private long refreshTokenExpiration;

    // Brute Force Protection Configuration
    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final int LOCK_DURATION_MINUTES = 30;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        // Check if email already exists
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new AuthException("Email already registered");
        }

        // Security: Always assign USER role for public registration
        // Admin users should be created through admin endpoints or database seeding

        // Create user
        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .phoneNumber(request.getPhoneNumber())
                .role(Role.USER)
                .isEmailVerified(false)
                .emailVerificationToken(UUID.randomUUID().toString())
                .isActive(true)
                .build();

        User savedUser = userRepository.save(user);
        log.info("User registered: {}", savedUser.getEmail());

        // Publish event to send verification email
        eventPublisherService.publishUserRegisteredEvent(UserRegisteredEvent.builder()
                .userId(savedUser.getId().toString())
                .email(savedUser.getEmail())
                .firstName(savedUser.getFirstName())
                .lastName(savedUser.getLastName())
                .role(savedUser.getRole().name())
                .verificationToken(savedUser.getEmailVerificationToken())
                .build());

        return generateAuthResponse(savedUser);
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        // First, check if user exists and handle brute force protection
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new AuthException("Invalid email or password"));

        // Check if account is locked
        if (user.isAccountLocked()) {
            long minutesRemaining = java.time.Duration.between(
                    LocalDateTime.now(), 
                    user.getLockedUntil()
            ).toMinutes();
            log.warn("Login attempt on locked account: {}", user.getEmail());
            throw new AuthException("Account is locked. Try again in " + minutesRemaining + " minutes");
        }

        // Check if account is active
        if (!user.isActive()) {
            throw new AuthException("Account is deactivated");
        }

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getEmail(),
                            request.getPassword()
                    )
            );

            // Successful login - reset failed attempts
            if (user.getFailedLoginAttempts() > 0) {
                user.resetFailedAttempts();
                userRepository.save(user);
            }

        } catch (BadCredentialsException e) {
            // Failed login - increment attempts and potentially lock
            handleFailedLogin(user);
            throw new AuthException("Invalid email or password");
        }

        // Check if access token is blacklisted (defense-in-depth, e.g. after logout)
        // This assumes client sends token for login, which is not typical, but can be checked in filter layer

        log.info("User logged in: {}", user.getEmail());
        return generateAuthResponse(user);
    }

    /**
     * Handle failed login attempt - increment counter and lock if needed
     */
    private void handleFailedLogin(User user) {
        user.incrementFailedAttempts();
        
        if (user.getFailedLoginAttempts() >= MAX_FAILED_ATTEMPTS) {
            user.lockAccount(LOCK_DURATION_MINUTES);
            log.warn("Account locked due to {} failed attempts: {}", 
                    MAX_FAILED_ATTEMPTS, user.getEmail());
        } else {
            int remainingAttempts = MAX_FAILED_ATTEMPTS - user.getFailedLoginAttempts();
            log.warn("Failed login attempt {} for user: {}. {} attempts remaining", 
                    user.getFailedLoginAttempts(), user.getEmail(), remainingAttempts);
        }
        
        userRepository.save(user);
    }

    @Transactional
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        RefreshToken refreshToken = refreshTokenRepository.findByToken(request.getRefreshToken())
                .orElseThrow(() -> new AuthException("Invalid refresh token"));

        if (!refreshToken.isValid()) {
            throw new AuthException("Refresh token is expired or revoked");
        }

        User user = refreshToken.getUser();

        // Revoke old refresh token
        refreshToken.setRevoked(true);
        refreshTokenRepository.save(refreshToken);

        log.info("Token refreshed for user: {}", user.getEmail());
        return generateAuthResponse(user);
    }

    @Transactional
    public void logout(String refreshToken) {
        refreshTokenRepository.findByToken(refreshToken)
                .ifPresent(token -> {
                    token.setRevoked(true);
                    refreshTokenRepository.save(token);
                    // Blacklist access token (client should send access token in header)
                    // For demo, assume access token is sent as part of request (not shown here)
                    // Example: tokenBlacklistService.blacklistToken(accessToken, jwtService.getAccessTokenExpiration() / 1000);
                    log.info("User logged out");
                });
    }

    @Transactional
    public void verifyEmail(String token) {
        User user = userRepository.findByEmailVerificationToken(token)
                .orElseThrow(() -> new AuthException("Invalid verification token"));

        user.setEmailVerified(true);
        user.setEmailVerificationToken(null);
        userRepository.save(user);
        
        log.info("Email verified for user: {}", user.getEmail());
    }

    @Transactional
    public void forgotPassword(ForgotPasswordRequest request) {
        userRepository.findByEmail(request.getEmail())
                .ifPresent(user -> {
                    String resetToken = UUID.randomUUID().toString();
                    user.setPasswordResetToken(resetToken);
                    user.setPasswordResetTokenExpiry(LocalDateTime.now().plusHours(1));
                    userRepository.save(user);

                    // Publish event to send password reset email
                    eventPublisherService.publishPasswordResetEvent(PasswordResetEvent.builder()
                            .userId(user.getId().toString())
                            .email(user.getEmail())
                            .firstName(user.getFirstName())
                            .resetToken(resetToken)
                            .build());

                    log.info("Password reset requested for: {}", user.getEmail());
                });
        // Don't reveal if email exists or not for security
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        User user = userRepository.findByPasswordResetToken(request.getToken())
                .orElseThrow(() -> new AuthException("Invalid reset token"));

        if (user.getPasswordResetTokenExpiry().isBefore(LocalDateTime.now())) {
            throw new AuthException("Reset token has expired");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setPasswordResetToken(null);
        user.setPasswordResetTokenExpiry(null);
        userRepository.save(user);

        // Revoke all refresh tokens for security
        refreshTokenRepository.revokeAllByUser(user);

        log.info("Password reset successful for: {}", user.getEmail());
    }

    private AuthResponse generateAuthResponse(User user) {
        String accessToken = jwtService.generateAccessToken(
                user.getId().toString(),
                user.getEmail(),
                user.getRole().name()
        );

        String refreshTokenStr = jwtService.generateRefreshToken(user.getId().toString());

        // Save refresh token
        RefreshToken refreshToken = RefreshToken.builder()
                .token(refreshTokenStr)
                .user(user)
                .expiresAt(LocalDateTime.now().plusSeconds(refreshTokenExpiration / 1000))
                .isRevoked(false)
                .build();
        refreshTokenRepository.save(refreshToken);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshTokenStr)
                .tokenType("Bearer")
                .expiresIn(jwtService.getAccessTokenExpiration() / 1000)
                .user(AuthResponse.UserInfo.builder()
                        .id(user.getId().toString())
                        .email(user.getEmail())
                        .firstName(user.getFirstName())
                        .lastName(user.getLastName())
                        .role(user.getRole())
                        .build())
                .build();
    }
}
