package tech.remiges.serversage.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import tech.remiges.serversage.exception.CustomExceptions;
import tech.remiges.serversage.model.User;
import tech.remiges.serversage.observability.ObservabilityService;
import tech.remiges.serversage.repository.UserRepository;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive test suite for UserService
 * Tests all functionality including error scenarios for observability testing
 */
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private ObservabilityService observabilityService;

    @InjectMocks
    private UserService userService;

    private User testUser;
    private User testUser2;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setName("John Doe");
        testUser.setEmail("john.doe@example.com");
        testUser.setRole("USER");

        testUser2 = new User();
        testUser2.setId(2L);
        testUser2.setName("Jane Smith");
        testUser2.setEmail("jane.smith@example.com");
        testUser2.setRole("ADMIN");

        // Mock observability service to return values for span execution
        when(observabilityService.executeInSpan(anyString(), anyString(), any(java.util.function.Supplier.class)))
                .thenAnswer(invocation -> {
                    java.util.function.Supplier<?> supplier = invocation.getArgument(2);
                    return supplier.get();
                });
    }

    @Test
    void getAllUsers_ShouldReturnAllUsers() {
        // Given
        List<User> users = Arrays.asList(testUser, testUser2);
        when(userRepository.findAll()).thenReturn(users);

        // When
        List<User> result = userService.getAllUsers();

        // Then
        assertEquals(2, result.size());
        assertEquals(testUser.getEmail(), result.get(0).getEmail());
        assertEquals(testUser2.getEmail(), result.get(1).getEmail());
        verify(userRepository).findAll();
        verify(observabilityService).executeInSpan(eq("UserService.getAllUsers"), eq("user-service"), any(java.util.function.Supplier.class));
    }

    @Test
    void getAllUsers_ShouldThrowDatabaseException_WhenDatabaseFails() {
        // Given
        when(userRepository.findAll()).thenThrow(new RuntimeException("Database connection failed"));

        // When & Then
        assertThrows(RuntimeException.class, () -> userService.getAllUsers());
        verify(userRepository).findAll();
    }

    @Test
    void getUserById_ShouldReturnUser_WhenUserExists() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        // When
        Optional<User> result = userService.getUserById(1L);

        // Then
        assertTrue(result.isPresent());
        assertEquals(testUser.getEmail(), result.get().getEmail());
        verify(userRepository).findById(1L);
    }

    @Test
    void getUserById_ShouldReturnEmpty_WhenUserNotExists() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        // When
        Optional<User> result = userService.getUserById(1L);

        // Then
        assertFalse(result.isPresent());
        verify(userRepository).findById(1L);
    }

    @Test
    void getUserById_ShouldThrowArrayIndexOutOfBounds_WhenIdIs999() {
        // When & Then
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> userService.getUserById(999L));
    }

    @Test
    void getUserById_ShouldThrowNullPointerException_WhenIdIs998() {
        // When & Then
        assertThrows(NullPointerException.class, () -> userService.getUserById(998L));
    }

    @Test
    void getUserById_ShouldThrowRateLimitException_WhenIdIs997() {
        // When & Then
        assertThrows(CustomExceptions.RateLimitException.class, () -> userService.getUserById(997L));
    }

    @Test
    void getUserByEmail_ShouldReturnUser_WhenUserExists() {
        // Given
        when(userRepository.findByEmail("john.doe@example.com")).thenReturn(Optional.of(testUser));

        // When
        Optional<User> result = userService.getUserByEmail("john.doe@example.com");

        // Then
        assertTrue(result.isPresent());
        assertEquals(testUser.getEmail(), result.get().getEmail());
        verify(userRepository).findByEmail("john.doe@example.com");
    }

    @Test
    void getUserByEmail_ShouldThrowDatabaseException_WhenEmailContainsDberror() {
        // When & Then
        assertThrows(CustomExceptions.DatabaseConnectionException.class, 
                () -> userService.getUserByEmail("dberror@example.com"));
    }

    @Test
    void getUsersByRole_ShouldReturnUsersWithRole() {
        // Given
        List<User> adminUsers = Arrays.asList(testUser2);
        when(userRepository.findByRole("ADMIN")).thenReturn(adminUsers);

        // When
        List<User> result = userService.getUsersByRole("ADMIN");

        // Then
        assertEquals(1, result.size());
        assertEquals("ADMIN", result.get(0).getRole());
        verify(userRepository).findByRole("ADMIN");
    }

    @Test
    void searchUsers_ShouldReturnMatchingUsers() {
        // Given
        List<User> searchResults = Arrays.asList(testUser);
        when(userRepository.searchUsers("John")).thenReturn(searchResults);

        // When
        List<User> result = userService.searchUsers("John");

        // Then
        assertEquals(1, result.size());
        assertEquals(testUser.getName(), result.get(0).getName());
        verify(userRepository).searchUsers("John");
    }

    @Test
    void searchUsers_ShouldThrowValidationException_WhenKeywordIsEmpty() {
        // When & Then
        assertThrows(CustomExceptions.ValidationException.class, 
                () -> userService.searchUsers(""));
        assertThrows(CustomExceptions.ValidationException.class, 
                () -> userService.searchUsers(null));
    }

    @Test
    void getUserCountByRole_ShouldReturnCount() {
        // Given
        when(userRepository.countByRole("USER")).thenReturn(5L);

        // When
        Long result = userService.getUserCountByRole("USER");

        // Then
        assertEquals(5L, result);
        verify(userRepository).countByRole("USER");
    }

    @Test
    void createUser_ShouldCreateUser_WhenValidUser() {
        // Given
        User newUser = new User();
        newUser.setName("New User");
        newUser.setEmail("new.user@example.com");
        newUser.setRole("USER");

        when(userRepository.existsByEmail("new.user@example.com")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenReturn(newUser);

        // When
        User result = userService.createUser(newUser);

        // Then
        assertEquals(newUser.getEmail(), result.getEmail());
        verify(userRepository).existsByEmail("new.user@example.com");
        verify(userRepository).save(newUser);
    }

    @Test
    void createUser_ShouldThrowValidationException_WhenUserInvalid() {
        // Given
        User invalidUser = new User();
        invalidUser.setName("");
        invalidUser.setEmail("invalid-email");

        // When & Then
        assertThrows(CustomExceptions.ValidationException.class, 
                () -> userService.createUser(invalidUser));
    }

    @Test
    void createUser_ShouldThrowDuplicateEmailException_WhenEmailExists() {
        // Given
        when(userRepository.existsByEmail("john.doe@example.com")).thenReturn(true);

        // When & Then
        assertThrows(CustomExceptions.DuplicateEmailException.class, 
                () -> userService.createUser(testUser));
        verify(userRepository).existsByEmail("john.doe@example.com");
        verify(userRepository, never()).save(any());
    }

    @Test
    void createUser_ShouldThrowDatabaseException_WhenEmailContainsDberror() {
        // Given
        User dbErrorUser = new User();
        dbErrorUser.setName("DB Error User");
        dbErrorUser.setEmail("dberror@example.com");
        dbErrorUser.setRole("USER");

        // When & Then
        assertThrows(CustomExceptions.DatabaseConnectionException.class, 
                () -> userService.createUser(dbErrorUser));
    }

    @Test
    void createUser_ShouldThrowTimeoutException_WhenEmailContainsTimeout() {
        // Given
        User timeoutUser = new User();
        timeoutUser.setName("Timeout User");
        timeoutUser.setEmail("timeout@example.com");
        timeoutUser.setRole("USER");

        when(userRepository.existsByEmail("timeout@example.com")).thenReturn(false);

        // When & Then
        assertThrows(CustomExceptions.TimeoutException.class, 
                () -> userService.createUser(timeoutUser));
    }

    @Test
    void createUser_ShouldThrowExternalServiceException_WhenEmailContainsExternal() {
        // Given
        User externalUser = new User();
        externalUser.setName("External User");
        externalUser.setEmail("external@example.com");
        externalUser.setRole("USER");

        when(userRepository.existsByEmail("external@example.com")).thenReturn(false);

        // When & Then
        assertThrows(CustomExceptions.ExternalServiceException.class, 
                () -> userService.createUser(externalUser));
    }

    @Test
    void createUser_ShouldThrowBusinessLogicException_WhenAdminNameWithoutAdminRole() {
        // Given
        User adminUser = new User();
        adminUser.setName("admin user");
        adminUser.setEmail("admin.user@example.com");
        adminUser.setRole("USER");

        when(userRepository.existsByEmail("admin.user@example.com")).thenReturn(false);

        // When & Then
        assertThrows(CustomExceptions.BusinessLogicException.class, 
                () -> userService.createUser(adminUser));
    }

    @Test
    void createUser_ShouldHandleDataIntegrityViolation() {
        // Given
        when(userRepository.existsByEmail("john.doe@example.com")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenThrow(new DataIntegrityViolationException("Duplicate key"));

        // When & Then
        assertThrows(CustomExceptions.DuplicateEmailException.class, 
                () -> userService.createUser(testUser));
        verify(userRepository).save(testUser);
    }

    @Test
    void createMultipleUsers_ShouldCreateAllUsers_WhenAllValid() {
        // Given
        List<User> users = Arrays.asList(testUser, testUser2);
        when(userRepository.saveAll(users)).thenReturn(users);

        // When
        List<User> result = userService.createMultipleUsers(users);

        // Then
        assertEquals(2, result.size());
        verify(userRepository).saveAll(users);
    }

    @Test
    void createMultipleUsers_ShouldThrowValidationException_WhenAnyUserInvalid() {
        // Given
        User invalidUser = new User();
        invalidUser.setName("");
        List<User> users = Arrays.asList(testUser, invalidUser);

        // When & Then
        assertThrows(CustomExceptions.ValidationException.class, 
                () -> userService.createMultipleUsers(users));
        verify(userRepository, never()).saveAll(any());
    }

    @Test
    void createMultipleUsers_ShouldThrowDuplicateEmailException_WhenDataIntegrityViolation() {
        // Given
        List<User> users = Arrays.asList(testUser, testUser2);
        when(userRepository.saveAll(users)).thenThrow(new DataIntegrityViolationException("Duplicate key"));

        // When & Then
        assertThrows(CustomExceptions.DuplicateEmailException.class, 
                () -> userService.createMultipleUsers(users));
        verify(userRepository).saveAll(users);
    }

    @Test
    void getUserProfile_ShouldReturnProfile_WhenUserExists() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        // When
        Map<String, Object> result = userService.getUserProfile(1L);

        // Then
        assertEquals(testUser.getId(), result.get("id"));
        assertEquals(testUser.getName(), result.get("name"));
        assertEquals(testUser.getEmail(), result.get("email"));
        assertEquals(testUser.getRole(), result.get("role"));
        assertEquals("ACTIVE", result.get("accountStatus"));
        verify(userRepository).findById(1L);
    }

    @Test
    void getUserProfile_ShouldThrowUserNotFoundException_WhenUserNotExists() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(CustomExceptions.UserNotFoundException.class, 
                () -> userService.getUserProfile(1L));
        verify(userRepository).findById(1L);
    }

    @Test
    void createUserAsync_ShouldReturnCompletableFuture() {
        // Given
        User newUser = new User();
        newUser.setName("Async User");
        newUser.setEmail("async.user@example.com");
        newUser.setRole("USER");

        when(userRepository.existsByEmail("async.user@example.com")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenReturn(newUser);

        // When
        CompletableFuture<User> result = userService.createUserAsync(newUser);

        // Then
        assertNotNull(result);
        assertTrue(result instanceof CompletableFuture);
    }

    @Test
    void updateUser_ShouldUpdateUser_WhenUserExists() {
        // Given
        User updatedDetails = new User();
        updatedDetails.setName("Updated Name");
        updatedDetails.setEmail("updated@example.com");
        updatedDetails.setRole("ADMIN");

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        Optional<User> result = userService.updateUser(1L, updatedDetails);

        // Then
        assertTrue(result.isPresent());
        verify(userRepository).findById(1L);
        verify(userRepository).save(any(User.class));
    }

    @Test
    void updateUser_ShouldReturnEmpty_WhenUserNotExists() {
        // Given
        User updatedDetails = new User();
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        // When
        Optional<User> result = userService.updateUser(1L, updatedDetails);

        // Then
        assertFalse(result.isPresent());
        verify(userRepository).findById(1L);
        verify(userRepository, never()).save(any());
    }

    @Test
    void updateUser_ShouldThrowBusinessLogicException_WhenAssigningSuperAdminRole() {
        // Given
        User updatedDetails = new User();
        updatedDetails.setRole("SUPER_ADMIN");

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        // When & Then
        assertThrows(CustomExceptions.BusinessLogicException.class, 
                () -> userService.updateUser(1L, updatedDetails));
        verify(userRepository).findById(1L);
        verify(userRepository, never()).save(any());
    }

    @Test
    void deleteUser_ShouldDeleteUser_WhenUserExists() {
        // Given
        when(userRepository.existsById(1L)).thenReturn(true);

        // When
        assertDoesNotThrow(() -> userService.deleteUser(1L));

        // Then
        verify(userRepository).existsById(1L);
        verify(userRepository).deleteById(1L);
    }

    @Test
    void deleteUser_ShouldThrowUserNotFoundException_WhenUserNotExists() {
        // Given
        when(userRepository.existsById(1L)).thenReturn(false);

        // When & Then
        assertThrows(CustomExceptions.UserNotFoundException.class, 
                () -> userService.deleteUser(1L));
        verify(userRepository).existsById(1L);
        verify(userRepository, never()).deleteById(any());
    }

    @Test
    void validateUser_ShouldReturnValid_WhenUserIsValid() {
        // When
        String result = userService.validateUser(testUser);

        // Then
        assertEquals("Valid", result);
    }

    @Test
    void validateUser_ShouldReturnError_WhenUserIsNull() {
        // When
        String result = userService.validateUser(null);

        // Then
        assertEquals("User cannot be null", result);
    }

    @Test
    void validateUser_ShouldReturnError_WhenNameIsEmpty() {
        // Given
        testUser.setName("");

        // When
        String result = userService.validateUser(testUser);

        // Then
        assertEquals("Name cannot be empty", result);
    }

    @Test
    void validateUser_ShouldReturnError_WhenEmailIsEmpty() {
        // Given
        testUser.setEmail("");

        // When
        String result = userService.validateUser(testUser);

        // Then
        assertEquals("Email cannot be empty", result);
    }

    @Test
    void validateUser_ShouldReturnError_WhenEmailIsInvalid() {
        // Given
        testUser.setEmail("invalid-email");

        // When
        String result = userService.validateUser(testUser);

        // Then
        assertEquals("Email format is invalid", result);
    }
}
