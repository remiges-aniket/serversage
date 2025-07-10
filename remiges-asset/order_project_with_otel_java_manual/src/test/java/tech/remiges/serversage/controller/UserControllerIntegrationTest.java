package tech.remiges.serversage.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import tech.remiges.serversage.model.User;
import tech.remiges.serversage.service.UserService;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)
@ActiveProfiles("test")
class UserControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserService userService;

    @Test
    void testGetAllUsers() throws Exception {
        // Given
        List<User> users = Arrays.asList(
            new User("John Doe", "john@example.com", "USER"),
            new User("Jane Smith", "jane@example.com", "ADMIN")
        );
        when(userService.getAllUsers()).thenReturn(users);

        // When & Then
        mockMvc.perform(get("/api/users"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void testGetUserById() throws Exception {
        // Given
        User user = new User("John Doe", "john@example.com", "USER");
        user.setId(1L);
        when(userService.getUserById(1L)).thenReturn(Optional.of(user));

        // When & Then
        mockMvc.perform(get("/api/users/1"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.name").value("John Doe"))
                .andExpect(jsonPath("$.email").value("john@example.com"))
                .andExpect(jsonPath("$.role").value("USER"));
    }

    @Test
    void testGetUserByIdNotFound() throws Exception {
        // Given
        when(userService.getUserById(999L)).thenReturn(Optional.empty());

        // When & Then
        mockMvc.perform(get("/api/users/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void testCreateUser() throws Exception {
        // Given
        User inputUser = new User("John Doe", "john.doe@example.com", "USER");
        User savedUser = new User("John Doe", "john.doe@example.com", "USER");
        savedUser.setId(1L);
        
        when(userService.validateUser(any(User.class))).thenReturn("Valid");
        when(userService.createUserAsync(any(User.class))).thenReturn(
            java.util.concurrent.CompletableFuture.completedFuture(savedUser)
        );

        // When & Then
        mockMvc.perform(post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(inputUser)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("John Doe"))
                .andExpect(jsonPath("$.email").value("john.doe@example.com"))
                .andExpect(jsonPath("$.role").value("USER"));
    }

    @Test
    void testCreateUserWithValidationError() throws Exception {
        // Given
        User user = new User("", "invalid-email", "USER");
        when(userService.validateUser(any(User.class))).thenReturn("Name is required");

        // When & Then
        mockMvc.perform(post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(user)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Name is required"));
    }

    @Test
    void testUpdateUser() throws Exception {
        // Given
        User updatedUser = new User("Updated User", "updated@example.com", "ADMIN");
        updatedUser.setId(1L);
        
        when(userService.updateUser(anyLong(), any(User.class))).thenReturn(Optional.of(updatedUser));

        // When & Then
        mockMvc.perform(put("/api/users/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updatedUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated User"))
                .andExpect(jsonPath("$.email").value("updated@example.com"))
                .andExpect(jsonPath("$.role").value("ADMIN"));
    }

    @Test
    void testDeleteUser() throws Exception {
        // When & Then
        mockMvc.perform(delete("/api/users/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    void testSearchUsers() throws Exception {
        // Given
        List<User> users = Arrays.asList(
            new User("John Doe", "john@example.com", "USER")
        );
        when(userService.searchUsers("John")).thenReturn(users);

        // When & Then
        mockMvc.perform(get("/api/users/search?keyword=John"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void testGetUsersByRole() throws Exception {
        // Given
        List<User> users = Arrays.asList(
            new User("Admin User", "admin@example.com", "ADMIN")
        );
        when(userService.getUsersByRole("ADMIN")).thenReturn(users);

        // When & Then
        mockMvc.perform(get("/api/users/role/ADMIN"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void testGetUserCountByRole() throws Exception {
        // Given
        when(userService.getUserCountByRole("USER")).thenReturn(5L);

        // When & Then
        mockMvc.perform(get("/api/users/role/USER/count"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").value(5));
    }

    @Test
    void testHealthCheck() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/users/health-check"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("healthy"))
                .andExpect(jsonPath("$.service").value("UserService"))
                .andExpect(jsonPath("$.timestamp").exists());
    }
}
