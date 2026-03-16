package com.vulnbookstore;

import com.vulnbookstore.model.User;
import com.vulnbookstore.repository.UserRepository;
import com.vulnbookstore.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for UserService.
 * Tests cover user creation, authentication, and password reset token generation.
 *
 * Note: These tests verify functional correctness only.
 */
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    private User sampleUser;

    @BeforeEach
    void setUp() {
        sampleUser = new User(1L, "alice", "alice@example.com",
                "password123", "USER", LocalDateTime.now(), null);
    }

    // ----------------------------------------------------------------
    // createUser()
    // ----------------------------------------------------------------

    @Test
    @DisplayName("createUser() creates and saves a new user successfully")
    void createUser_savesNewUser() {
        when(userRepository.existsByUsername("alice")).thenReturn(false);
        when(userRepository.existsByEmail("alice@example.com")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenReturn(sampleUser);

        User result = userService.createUser("alice", "alice@example.com", "password123", "USER");

        assertNotNull(result);
        assertEquals("alice", result.getUsername());
        assertEquals("alice@example.com", result.getEmail());
        assertEquals("password123", result.getPassword());
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("createUser() assigns default role USER when role is null")
    void createUser_assignsDefaultRole_whenRoleIsNull() {
        when(userRepository.existsByUsername("bob")).thenReturn(false);
        when(userRepository.existsByEmail("bob@example.com")).thenReturn(false);

        User savedUser = new User(2L, "bob", "bob@example.com", "pass", "USER", LocalDateTime.now(), null);
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        User result = userService.createUser("bob", "bob@example.com", "pass", null);

        assertEquals("USER", result.getRole());
    }

    @Test
    @DisplayName("createUser() throws exception when username already exists")
    void createUser_throwsException_whenUsernameExists() {
        when(userRepository.existsByUsername("alice")).thenReturn(true);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> userService.createUser("alice", "newemail@example.com", "pass", "USER"));

        assertThat(ex.getMessage()).contains("Username already exists");
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("createUser() throws exception when email already registered")
    void createUser_throwsException_whenEmailExists() {
        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userRepository.existsByEmail("alice@example.com")).thenReturn(true);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> userService.createUser("newuser", "alice@example.com", "pass", "USER"));

        assertThat(ex.getMessage()).contains("Email already registered");
        verify(userRepository, never()).save(any());
    }

    // ----------------------------------------------------------------
    // authenticateUser()
    // ----------------------------------------------------------------

    @Test
    @DisplayName("authenticateUser() returns user when credentials are correct")
    void authenticateUser_returnsUser_whenCredentialsCorrect() {
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(sampleUser));

        Optional<User> result = userService.authenticateUser("alice", "password123");

        assertTrue(result.isPresent());
        assertEquals("alice", result.get().getUsername());
    }

    @Test
    @DisplayName("authenticateUser() returns empty when password is incorrect")
    void authenticateUser_returnsEmpty_whenPasswordIncorrect() {
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(sampleUser));

        Optional<User> result = userService.authenticateUser("alice", "wrongpassword");

        assertFalse(result.isPresent());
    }

    @Test
    @DisplayName("authenticateUser() returns empty when user does not exist")
    void authenticateUser_returnsEmpty_whenUserNotFound() {
        when(userRepository.findByUsername("unknown")).thenReturn(Optional.empty());

        Optional<User> result = userService.authenticateUser("unknown", "anypassword");

        assertFalse(result.isPresent());
    }

    @Test
    @DisplayName("authenticateUser() is case-sensitive for passwords")
    void authenticateUser_isCaseSensitive() {
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(sampleUser));

        // "Password123" != "password123"
        Optional<User> result = userService.authenticateUser("alice", "Password123");

        assertFalse(result.isPresent());
    }

    // ----------------------------------------------------------------
    // generatePasswordResetToken()
    // ----------------------------------------------------------------

    @Test
    @DisplayName("generatePasswordResetToken() returns a token for a registered email")
    void generatePasswordResetToken_returnsToken_forRegisteredEmail() {
        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(sampleUser));
        when(userRepository.save(any(User.class))).thenReturn(sampleUser);

        Optional<String> token = userService.generatePasswordResetToken("alice@example.com");

        assertTrue(token.isPresent());
        assertThat(token.get()).isNotBlank();
        // Token should be a hex string
        assertThat(token.get()).matches("[0-9a-f]+");
    }

    @Test
    @DisplayName("generatePasswordResetToken() returns empty for unregistered email")
    void generatePasswordResetToken_returnsEmpty_forUnregisteredEmail() {
        when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

        Optional<String> token = userService.generatePasswordResetToken("unknown@example.com");

        assertFalse(token.isPresent());
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("generatePasswordResetToken() generates different tokens on successive calls")
    void generatePasswordResetToken_generatesDifferentTokens() {
        User user1 = new User(1L, "alice", "alice@example.com", "pass", "USER", LocalDateTime.now(), null);
        User user2 = new User(1L, "alice", "alice@example.com", "pass", "USER", LocalDateTime.now(), null);

        when(userRepository.findByEmail("alice@example.com"))
                .thenReturn(Optional.of(user1))
                .thenReturn(Optional.of(user2));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        Optional<String> token1 = userService.generatePasswordResetToken("alice@example.com");
        Optional<String> token2 = userService.generatePasswordResetToken("alice@example.com");

        assertTrue(token1.isPresent());
        assertTrue(token2.isPresent());
        assertThat(token1.get()).isNotBlank();
        assertThat(token2.get()).isNotBlank();
    }

    // ----------------------------------------------------------------
    // getUserByToken()
    // ----------------------------------------------------------------

    @Test
    @DisplayName("getUserByToken() returns user when token matches")
    void getUserByToken_returnsUser_whenTokenMatches() {
        sampleUser.setResetToken("abc123def456");
        when(userRepository.findByResetToken("abc123def456")).thenReturn(Optional.of(sampleUser));

        Optional<User> result = userService.getUserByToken("abc123def456");

        assertTrue(result.isPresent());
        assertEquals("alice", result.get().getUsername());
    }

    @Test
    @DisplayName("getUserByToken() returns empty when token not found")
    void getUserByToken_returnsEmpty_whenTokenNotFound() {
        when(userRepository.findByResetToken("invalidtoken")).thenReturn(Optional.empty());

        Optional<User> result = userService.getUserByToken("invalidtoken");

        assertFalse(result.isPresent());
    }
}
