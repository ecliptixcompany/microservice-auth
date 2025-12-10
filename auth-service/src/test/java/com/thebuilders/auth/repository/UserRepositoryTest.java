package com.thebuilders.auth.repository;

import com.thebuilders.auth.entity.User;
import com.thebuilders.common.enums.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@DisplayName("UserRepository Tests")
class UserRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private UserRepository userRepository;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .email("test@example.com")
                .password("encodedPassword")
                .firstName("John")
                .lastName("Doe")
                .role(Role.USER)
                .isEmailVerified(false)
                .emailVerificationToken("verification-token-123")
                .isActive(true)
                .build();
    }

    @Test
    @DisplayName("Should find user by email")
    void shouldFindUserByEmail() {
        // Given
        entityManager.persistAndFlush(testUser);

        // When
        Optional<User> found = userRepository.findByEmail("test@example.com");

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getEmail()).isEqualTo("test@example.com");
        assertThat(found.get().getFirstName()).isEqualTo("John");
    }

    @Test
    @DisplayName("Should return empty when user not found by email")
    void shouldReturnEmptyWhenUserNotFoundByEmail() {
        // When
        Optional<User> found = userRepository.findByEmail("nonexistent@example.com");

        // Then
        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("Should check if email exists")
    void shouldCheckIfEmailExists() {
        // Given
        entityManager.persistAndFlush(testUser);

        // When/Then
        assertThat(userRepository.existsByEmail("test@example.com")).isTrue();
        assertThat(userRepository.existsByEmail("nonexistent@example.com")).isFalse();
    }

    @Test
    @DisplayName("Should find user by email verification token")
    void shouldFindUserByEmailVerificationToken() {
        // Given
        entityManager.persistAndFlush(testUser);

        // When
        Optional<User> found = userRepository.findByEmailVerificationToken("verification-token-123");

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getEmail()).isEqualTo("test@example.com");
    }

    @Test
    @DisplayName("Should find user by password reset token")
    void shouldFindUserByPasswordResetToken() {
        // Given
        testUser.setPasswordResetToken("reset-token-456");
        entityManager.persistAndFlush(testUser);

        // When
        Optional<User> found = userRepository.findByPasswordResetToken("reset-token-456");

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getEmail()).isEqualTo("test@example.com");
    }

    @Test
    @DisplayName("Should save user with all fields")
    void shouldSaveUserWithAllFields() {
        // Given
        User user = User.builder()
                .email("newuser@example.com")
                .password("password")
                .firstName("Jane")
                .lastName("Smith")
                .phoneNumber("+1234567890")
                .role(Role.USER)
                .isEmailVerified(false)
                .isActive(true)
                .build();

        // When
        User saved = userRepository.saveAndFlush(user);
        entityManager.clear(); // Clear persistence context to force reload
        User reloaded = userRepository.findById(saved.getId()).orElse(null);

        // Then
        assertThat(reloaded).isNotNull();
        assertThat(reloaded.getId()).isNotNull();
        assertThat(reloaded.getEmail()).isEqualTo("newuser@example.com");
        // Note: createdAt is set by Hibernate's @CreationTimestamp
        // In H2 with DataJpaTest, it may be set after flush
    }
}
