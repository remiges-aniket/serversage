package tech.remiges.serversage.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import tech.remiges.serversage.model.User;
import tech.remiges.serversage.service.UserService;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void getAllUsers_ShouldReturnUserList() throws Exception {
        // Given
        User user1 = new User("John Doe", "john@example.com", "ADMIN");
        user1.setId(1L);
        User user2 = new User("Jane Smith", "jane@example.com", "USER");
        user2.setId(2L);

        when(userService.getAllUsers()).thenReturn(Arrays.asList(user1, user2));

        // When & Then
        mockMvc.perform(get("/api/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].name").value("John Doe"))
                .andExpect(jsonPath("$[0].email").value("john@example.com"))
                .andExpect(jsonPath("$[1].name").value("Jane Smith"));
    }

    @Test
    void getUserById_WhenUserExists_ShouldReturnUser() throws Exception {
        // Given
        User user = new User("John Doe", "john@example.com", "ADMIN");
        user.setId(1L);
        when(userService.getUserById(1L)).thenReturn(Optional.of(user));

        // When & Then
        mockMvc.perform(get("/api/users/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("John Doe"))
                .andExpect(jsonPath("$.email").value("john@example.com"))
                .andExpect(jsonPath("$.role").value("ADMIN"));
    }

    @Test
    void getUserById_WhenUserNotExists_ShouldReturn404() throws Exception {
        // Given
        when(userService.getUserById(999L)).thenReturn(Optional.empty());

        // When & Then
        mockMvc.perform(get("/api/users/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void createUser_WithValidData_ShouldCreateUser() throws Exception {
        // Given
        User inputUser = new User("John Doe", "john@example.com", "ADMIN");
        User savedUser = new User("John Doe", "john@example.com", "ADMIN");
        savedUser.setId(1L);

        when(userService.validateUser(any(User.class))).thenReturn("Valid");
        when(userService.createUserAsync(any(User.class)))
                .thenReturn(CompletableFuture.completedFuture(savedUser));

        // When & Then
        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(inputUser)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("John Doe"))
                .andExpect(jsonPath("$.email").value("john@example.com"))
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void createUser_WithInvalidData_ShouldReturnBadRequest() throws Exception {
        // Given
        User invalidUser = new User("", "invalid-email", "ADMIN");
        when(userService.validateUser(any(User.class))).thenReturn("Name cannot be empty");

        // When & Then
        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidUser)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Name cannot be empty"));
    }

    @Test
    void updateUser_WhenUserExists_ShouldUpdateUser() throws Exception {
        // Given
        User updatedUser = new User("John Updated", "john.updated@example.com", "MANAGER");
        updatedUser.setId(1L);

        when(userService.updateUser(eq(1L), any(User.class))).thenReturn(Optional.of(updatedUser));

        User inputUser = new User("John Updated", "john.updated@example.com", "MANAGER");

        // When & Then
        mockMvc.perform(put("/api/users/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(inputUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("John Updated"))
                .andExpect(jsonPath("$.email").value("john.updated@example.com"))
                .andExpect(jsonPath("$.role").value("MANAGER"));
    }

    @Test
    void deleteUser_ShouldReturnNoContent() throws Exception {
        // When & Then
        mockMvc.perform(delete("/api/users/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    void getUserByEmail_WhenUserExists_ShouldReturnUser() throws Exception {
        // Given
        User user = new User("John Doe", "john@example.com", "ADMIN");
        user.setId(1L);
        when(userService.getUserByEmail("john@example.com")).thenReturn(Optional.of(user));

        // When & Then
        mockMvc.perform(get("/api/users/email/john@example.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("John Doe"))
                .andExpect(jsonPath("$.email").value("john@example.com"));
    }

    @Test
    void getUsersByRole_ShouldReturnUsersWithRole() throws Exception {
        // Given
        User admin1 = new User("Admin One", "admin1@example.com", "ADMIN");
        admin1.setId(1L);
        User admin2 = new User("Admin Two", "admin2@example.com", "ADMIN");
        admin2.setId(2L);

        when(userService.getUsersByRole("ADMIN")).thenReturn(Arrays.asList(admin1, admin2));

        // When & Then
        mockMvc.perform(get("/api/users/role/ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].role").value("ADMIN"))
                .andExpect(jsonPath("$[1].role").value("ADMIN"));
    }

    @Test
    void searchUsers_WithKeyword_ShouldReturnMatchingUsers() throws Exception {
        // Given
        User user = new User("John Doe", "john@example.com", "ADMIN");
        user.setId(1L);
        when(userService.searchUsers("john")).thenReturn(Arrays.asList(user));

        // When & Then
        mockMvc.perform(get("/api/users/search?keyword=john"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("John Doe"));
    }

    @Test
    void getUserProfile_WhenUserExists_ShouldReturnProfile() throws Exception {
        // Given
        Map<String, Object> profile = new HashMap<>();
        profile.put("id", 1L);
        profile.put("name", "John Doe");
        profile.put("email", "john@example.com");
        profile.put("role", "ADMIN");
        profile.put("accountStatus", "ACTIVE");

        when(userService.getUserProfile(1L)).thenReturn(profile);

        // When & Then
        mockMvc.perform(get("/api/users/1/profile"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("John Doe"))
                .andExpect(jsonPath("$.accountStatus").value("ACTIVE"));
    }
}
