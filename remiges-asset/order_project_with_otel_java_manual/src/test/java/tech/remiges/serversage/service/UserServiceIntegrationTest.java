package tech.remiges.serversage.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import tech.remiges.serversage.exception.CustomExceptions;
import tech.remiges.serversage.model.User;
import tech.remiges.serversage.repository.UserRepository;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for UserService with real database interactions
 * Tests the complete flow including observability integration
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class UserServiceIntegrationTest {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    private User testUser;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        
        testUser = new User();
        testUser.setName("Integration Test User");
        testUser.setEmail("integration.test@example.com");
        testUser.setRole("USER");
    }

    @Test
    void createUser_ShouldPersistUserAndGenerateObservabilityData() {
        // When
        User createdUser = userService.createUser(testUser);

        // Then
        assertNotNull(createdUser.getId());
        assertEquals(testUser.getEmail(), createdUser.getEmail());
        assertEquals(testUser.getName(), createdUser.getName());
        assertEquals(testUser.getRole(), createdUser.getRole());

        // Verify persistence
        Optional<User> foundUser = userRepository.findById(createdUser.getId());
        assertTrue(foundUser.isPresent());
        assertEquals(testUser.getEmail(), foundUser.get().getEmail());
    }

    @Test
    void getAllUsers_ShouldReturnAllPersistedUsers() {
        // Given
        User user1 = userService.createUser(testUser);
        
        User user2 = new User();
        user2.setName("Second User");
        user2.setEmail("second.user@example.com");
        user2.setRole("ADMIN");
        User createdUser2 = userService.createUser(user2);

        // When
        List<User> allUsers = userService.getAllUsers();

        // Then
        assertEquals(2, allUsers.size());
        assertTrue(allUsers.stream().anyMatch(u -> u.getEmail().equals(user1.getEmail())));
        assertTrue(allUsers.stream().anyMatch(u -> u.getEmail().equals(createdUser2.getEmail())));
    }

    @Test
    void getUserById_ShouldReturnCorrectUser() {
        // Given
        User createdUser = userService.createUser(testUser);

        // When
        Optional<User> foundUser = userService.getUserById(createdUser.getId());

        // Then
        assertTrue(foundUser.isPresent());
        assertEquals(createdUser.getEmail(), foundUser.get().getEmail());
        assertEquals(createdUser.getName(), foundUser.get().getName());
    }

    @Test
    void getUserByEmail_ShouldReturnCorrectUser() {
        // Given
        User createdUser = userService.createUser(testUser);

        // When
        Optional<User> foundUser = userService.getUserByEmail(createdUser.getEmail());

        // Then
        assertTrue(foundUser.isPresent());
        assertEquals(createdUser.getId(), foundUser.get().getId());
        assertEquals(createdUser.getName(), foundUser.get().getName());
    }

    @Test
    void getUsersByRole_ShouldReturnUsersWithSpecificRole() {
        // Given
        User adminUser = new User();
        adminUser.setName("Admin User");
        adminUser.setEmail("admin@example.com");
        adminUser.setRole("ADMIN");
        
        userService.createUser(testUser); // USER role
        userService.createUser(adminUser); // ADMIN role

        // When
        List<User> adminUsers = userService.getUsersByRole("ADMIN");
        List<User> regularUsers = userService.getUsersByRole("USER");

        // Then
        assertEquals(1, adminUsers.size());
        assertEquals("ADMIN", adminUsers.get(0).getRole());
        assertEquals(1, regularUsers.size());
        assertEquals("USER", regularUsers.get(0).getRole());
    }

    @Test
    void searchUsers_ShouldReturnMatchingUsers() {
        // Given
        User user1 = new User();
        user1.setName("John Smith");
        user1.setEmail("john.smith@example.com");
        user1.setRole("USER");
        
        User user2 = new User();
        user2.setName("Jane Doe");
        user2.setEmail("jane.doe@example.com");
        user2.setRole("USER");
        
        userService.createUser(user1);
        userService.createUser(user2);

        // When
        List<User> johnResults = userService.searchUsers("John");
        List<User> janeResults = userService.searchUsers("Jane");

        // Then
        assertEquals(1, johnResults.size());
        assertTrue(johnResults.get(0).getName().contains("John"));
        assertEquals(1, janeResults.size());
        assertTrue(janeResults.get(0).getName().contains("Jane"));
    }

    @Test
    void getUserCountByRole_ShouldReturnCorrectCount() {
        // Given
        User adminUser1 = new User();
        adminUser1.setName("Admin 1");
        adminUser1.setEmail("admin1@example.com");
        adminUser1.setRole("ADMIN");
        
        User adminUser2 = new User();
        adminUser2.setName("Admin 2");
        adminUser2.setEmail("admin2@example.com");
        adminUser2.setRole("ADMIN");
        
        userService.createUser(testUser); // USER role
        userService.createUser(adminUser1); // ADMIN role
        userService.createUser(adminUser2); // ADMIN role

        // When
        Long adminCount = userService.getUserCountByRole("ADMIN");
        Long userCount = userService.getUserCountByRole("USER");

        // Then
        assertEquals(2L, adminCount);
        assertEquals(1L, userCount);
    }

    @Test
    void createMultipleUsers_ShouldPersistAllUsers() {
        // Given
        User user1 = new User();
        user1.setName("Batch User 1");
        user1.setEmail("batch1@example.com");
        user1.setRole("USER");
        
        User user2 = new User();
        user2.setName("Batch User 2");
        user2.setEmail("batch2@example.com");
        user2.setRole("ADMIN");
        
        List<User> batchUsers = Arrays.asList(user1, user2);

        // When
        List<User> createdUsers = userService.createMultipleUsers(batchUsers);

        // Then
        assertEquals(2, createdUsers.size());
        assertNotNull(createdUsers.get(0).getId());
        assertNotNull(createdUsers.get(1).getId());
        
        // Verify persistence
        List<User> allUsers = userRepository.findAll();
        assertEquals(2, allUsers.size());
    }

    @Test
    void getUserProfile_ShouldReturnCompleteProfile() {
        // Given
        User createdUser = userService.createUser(testUser);

        // When
        Map<String, Object> profile = userService.getUserProfile(createdUser.getId());

        // Then
        assertEquals(createdUser.getId(), profile.get("id"));
        assertEquals(createdUser.getName(), profile.get("name"));
        assertEquals(createdUser.getEmail(), profile.get("email"));
        assertEquals(createdUser.getRole(), profile.get("role"));
        assertEquals("ACTIVE", profile.get("accountStatus"));
        assertNotNull(profile.get("lastLogin"));
        assertNotNull(profile.get("createdAt"));
    }

    @Test
    void updateUser_ShouldPersistChanges() {
        // Given
        User createdUser = userService.createUser(testUser);
        
        User updateDetails = new User();
        updateDetails.setName("Updated Name");
        updateDetails.setEmail("updated.email@example.com");
        updateDetails.setRole("ADMIN");

        // When
        Optional<User> updatedUser = userService.updateUser(createdUser.getId(), updateDetails);

        // Then
        assertTrue(updatedUser.isPresent());
        assertEquals("Updated Name", updatedUser.get().getName());
        assertEquals("updated.email@example.com", updatedUser.get().getEmail());
        assertEquals("ADMIN", updatedUser.get().getRole());
        
        // Verify persistence
        Optional<User> foundUser = userRepository.findById(createdUser.getId());
        assertTrue(foundUser.isPresent());
        assertEquals("Updated Name", foundUser.get().getName());
    }

    @Test
    void deleteUser_ShouldRemoveFromDatabase() {
        // Given
        User createdUser = userService.createUser(testUser);
        Long userId = createdUser.getId();

        // When
        userService.deleteUser(userId);

        // Then
        Optional<User> foundUser = userRepository.findById(userId);
        assertFalse(foundUser.isPresent());
    }

    @Test
    void createUser_ShouldThrowDuplicateEmailException_WhenEmailAlreadyExists() {
        // Given
        userService.createUser(testUser);
        
        User duplicateUser = new User();
        duplicateUser.setName("Duplicate User");
        duplicateUser.setEmail(testUser.getEmail()); // Same email
        duplicateUser.setRole("ADMIN");

        // When & Then
        assertThrows(CustomExceptions.DuplicateEmailException.class, 
                () -> userService.createUser(duplicateUser));
    }

    @Test
    void createUser_ShouldThrowDatabaseConnectionException_WhenEmailContainsDberror() {
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
    void createUser_ShouldThrowBusinessLogicException_WhenAdminNameWithoutAdminRole() {
        // Given
        User adminNameUser = new User();
        adminNameUser.setName("admin user");
        adminNameUser.setEmail("admin.user@example.com");
        adminNameUser.setRole("USER");

        // When & Then
        assertThrows(CustomExceptions.BusinessLogicException.class, 
                () -> userService.createUser(adminNameUser));
    }

    @Test
    void getUserById_ShouldThrowArrayIndexOutOfBounds_WhenIdIs999() {
        // When & Then
        assertThrows(ArrayIndexOutOfBoundsException.class, 
                () -> userService.getUserById(999L));
    }

    @Test
    void getUserById_ShouldThrowNullPointerException_WhenIdIs998() {
        // When & Then
        assertThrows(NullPointerException.class, 
                () -> userService.getUserById(998L));
    }

    @Test
    void getUserById_ShouldThrowRateLimitException_WhenIdIs997() {
        // When & Then
        assertThrows(CustomExceptions.RateLimitException.class, 
                () -> userService.getUserById(997L));
    }

    @Test
    void getUserByEmail_ShouldThrowDatabaseConnectionException_WhenEmailContainsDberror() {
        // When & Then
        assertThrows(CustomExceptions.DatabaseConnectionException.class, 
                () -> userService.getUserByEmail("dberror@example.com"));
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
    void updateUser_ShouldThrowBusinessLogicException_WhenAssigningSuperAdminRole() {
        // Given
        User createdUser = userService.createUser(testUser);
        
        User updateDetails = new User();
        updateDetails.setRole("SUPER_ADMIN");

        // When & Then
        assertThrows(CustomExceptions.BusinessLogicException.class, 
                () -> userService.updateUser(createdUser.getId(), updateDetails));
    }

    @Test
    void getUserProfile_ShouldThrowUserNotFoundException_WhenUserNotExists() {
        // When & Then
        assertThrows(CustomExceptions.UserNotFoundException.class, 
                () -> userService.getUserProfile(999L));
    }

    @Test
    void deleteUser_ShouldThrowUserNotFoundException_WhenUserNotExists() {
        // When & Then
        assertThrows(CustomExceptions.UserNotFoundException.class, 
                () -> userService.deleteUser(999L));
    }
}
