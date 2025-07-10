package tech.remiges.serversage.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tech.remiges.serversage.exception.CustomExceptions;
import tech.remiges.serversage.model.User;
import tech.remiges.serversage.model.User.UserDTO;
import tech.remiges.serversage.service.UserService;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;

@RestController
@RequestMapping("/api/users")
@Tag(name = "User Management", description = "APIs for managing users with comprehensive error scenarios for observability testing")
public class UserController {

    private final UserService userService;

    @Autowired
    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    @Operation(
        summary = "Get all users", 
        description = "Retrieve all users from the database. May trigger database connection errors randomly (2% chance) for observability testing."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved users"),
            @ApiResponse(responseCode = "500", description = "Database connection error or internal server error")
    })
    public ResponseEntity<List<UserDTO>> getAllUsers() {
        var users = userService.getAllUsers()
                .stream()
                .map(UserDTO::fromEntity)
                .toList();
        return ResponseEntity.ok(users);
    }

    @GetMapping("/{id}")
    @Operation(
        summary = "Get user by ID", 
        description = "Retrieve a specific user by their ID. Special test IDs: 999 (ArrayIndexOutOfBounds), 998 (NullPointer), 997 (RateLimit)"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User found"),
            @ApiResponse(responseCode = "404", description = "User not found"),
            @ApiResponse(responseCode = "429", description = "Rate limit exceeded (ID: 997)"),
            @ApiResponse(responseCode = "500", description = "Internal server error (IDs: 999, 998)")
    })
    public ResponseEntity<UserDTO> getUserById(
            @Parameter(description = "User ID. Use 999 for ArrayIndexOutOfBounds, 998 for NullPointer, 997 for RateLimit", example = "1") 
            @PathVariable Long id) {
        return userService.getUserById(id)
                .map(UserDTO::fromEntity)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @Operation(
        summary = "Create new user", 
        description = "Create a new user. Error scenarios: email with 'dberror' (DB error), 'timeout' (timeout), 'external' (external service), name with 'admin' without ADMIN role (business logic error)"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "User created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input or validation error"),
            @ApiResponse(responseCode = "409", description = "User with email already exists"),
            @ApiResponse(responseCode = "422", description = "Business logic error"),
            @ApiResponse(responseCode = "500", description = "Database connection error"),
            @ApiResponse(responseCode = "503", description = "External service unavailable"),
            @ApiResponse(responseCode = "408", description = "Request timeout")
    })
    public ResponseEntity<?> createUser(@Valid @RequestBody User user) {
        String validationResult = userService.validateUser(user);

        if (!validationResult.equals("Valid")) {
            return ResponseEntity.badRequest().body(Map.of("error", validationResult));
        }

        CompletableFuture<User> futureUser = userService.createUserAsync(user);
        return futureUser
                .thenApply(UserDTO::fromEntity)
                .thenApply(userDTO -> ResponseEntity.status(HttpStatus.CREATED).body(userDTO))
                .join();
    }

    @PutMapping("/{id}")
    @Operation(
        summary = "Update user", 
        description = "Update an existing user. Cannot assign SUPER_ADMIN role (business logic error)"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User updated successfully"),
            @ApiResponse(responseCode = "404", description = "User not found"),
            @ApiResponse(responseCode = "400", description = "Invalid input"),
            @ApiResponse(responseCode = "422", description = "Business logic error (SUPER_ADMIN role)"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<UserDTO> updateUser(
            @Parameter(description = "User ID") @PathVariable Long id, 
            @Valid @RequestBody User user) {
        return userService.updateUser(id, user)
                .map(UserDTO::fromEntity)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    @Operation(
        summary = "Delete user", 
        description = "Delete a user by ID"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "User deleted successfully"),
            @ApiResponse(responseCode = "404", description = "User not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<Void> deleteUser(
            @Parameter(description = "User ID") @PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/email/{email}")
    @Operation(
        summary = "Get user by email", 
        description = "Retrieve a user by their email address. Use email with 'dberror' to trigger database connection error"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User found"),
            @ApiResponse(responseCode = "404", description = "User not found"),
            @ApiResponse(responseCode = "500", description = "Database connection error or internal server error")
    })
    public ResponseEntity<UserDTO> getUserByEmail(
            @Parameter(description = "User email. Use 'dberror@example.com' to trigger database error", example = "john.doe@example.com") 
            @PathVariable String email) {
        return userService.getUserByEmail(email)
                .map(UserDTO::fromEntity)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/role/{role}")
    @Operation(
        summary = "Get users by role", 
        description = "Retrieve all users with a specific role"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Users retrieved successfully"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<List<UserDTO>> getUsersByRole(
            @Parameter(description = "User role", example = "ADMIN") @PathVariable String role) {
        var users = userService.getUsersByRole(role)
                .stream()
                .map(UserDTO::fromEntity)
                .toList();
        return ResponseEntity.ok(users);
    }

    @GetMapping("/search")
    @Operation(
        summary = "Search users", 
        description = "Search users by name or email. Empty keyword triggers validation error"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Search completed successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid search keyword (empty or null)"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<List<UserDTO>> searchUsers(
            @Parameter(description = "Search keyword. Cannot be empty", example = "john") 
            @RequestParam String keyword) {
        var users = userService.searchUsers(keyword)
                .stream()
                .map(UserDTO::fromEntity)
                .toList();
        return ResponseEntity.ok(users);
    }

    @GetMapping("/role/{role}/count")
    @Operation(
        summary = "Get user count by role", 
        description = "Get the number of users with a specific role"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Count retrieved successfully"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<Long> getUserCountByRole(
            @Parameter(description = "User role") @PathVariable String role) {
        Long count = userService.getUserCountByRole(role);
        return ResponseEntity.ok(count);
    }

    @PostMapping("/batch")
    @Operation(
        summary = "Create multiple users", 
        description = "Create multiple users in a single request. All users must be valid"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Users created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input or validation error"),
            @ApiResponse(responseCode = "409", description = "Duplicate email addresses"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<List<UserDTO>> createMultipleUsers(@Valid @RequestBody List<User> users) {
        var createdUsers = userService.createMultipleUsers(users)
                .stream()
                .map(UserDTO::fromEntity)
                .toList();
        return ResponseEntity.status(HttpStatus.CREATED).body(createdUsers);
    }

    @GetMapping("/{id}/profile")
    @Operation(
        summary = "Get user profile", 
        description = "Get detailed user profile information including account status and timestamps"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Profile retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "User not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<Map<String, Object>> getUserProfile(
            @Parameter(description = "User ID") @PathVariable Long id) {
        Map<String, Object> profile = userService.getUserProfile(id);
        return ResponseEntity.ok(profile);
    }

    // Additional endpoints to reach 20+ APIs

    @GetMapping("/active")
    @Operation(
        summary = "Get active users", 
        description = "Retrieve all active users (non-INACTIVE role)"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Active users retrieved successfully"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<List<UserDTO>> getActiveUsers() {
        var users = userService.getAllUsers()
                .stream()
                .filter(user -> !"INACTIVE".equals(user.getRole()))
                .map(UserDTO::fromEntity)
                .toList();
        return ResponseEntity.ok(users);
    }

    @GetMapping("/stats")
    @Operation(
        summary = "Get user statistics", 
        description = "Get comprehensive user statistics including counts by role"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Statistics retrieved successfully"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<Map<String, Object>> getUserStatistics() {
        List<User> allUsers = userService.getAllUsers();
        
        Map<String, Object> stats = Map.of(
            "totalUsers", allUsers.size(),
            "adminUsers", allUsers.stream().filter(u -> "ADMIN".equals(u.getRole())).count(),
            "regularUsers", allUsers.stream().filter(u -> "USER".equals(u.getRole())).count(),
            "inactiveUsers", allUsers.stream().filter(u -> "INACTIVE".equals(u.getRole())).count(),
            "activeUsers", allUsers.stream().filter(u -> !"INACTIVE".equals(u.getRole())).count()
        );
        
        return ResponseEntity.ok(stats);
    }

    @PostMapping("/{id}/activate")
    @Operation(
        summary = "Activate user", 
        description = "Activate a user by changing their role from INACTIVE to USER"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User activated successfully"),
            @ApiResponse(responseCode = "404", description = "User not found"),
            @ApiResponse(responseCode = "400", description = "User is already active"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<UserDTO> activateUser(
            @Parameter(description = "User ID") @PathVariable Long id) {
        
        return userService.getUserById(id)
                .map(user -> {
                    if (!"INACTIVE".equals(user.getRole())) {
                        throw new CustomExceptions.BusinessLogicException("User is already active");
                    }
                    user.setRole("USER");
                    return userService.updateUser(id, user)
                            .map(UserDTO::fromEntity)
                            .map(ResponseEntity::ok)
                            .orElse(ResponseEntity.notFound().build());
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/deactivate")
    @Operation(
        summary = "Deactivate user", 
        description = "Deactivate a user by changing their role to INACTIVE"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User deactivated successfully"),
            @ApiResponse(responseCode = "404", description = "User not found"),
            @ApiResponse(responseCode = "400", description = "Cannot deactivate admin users"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<UserDTO> deactivateUser(
            @Parameter(description = "User ID") @PathVariable Long id) {
        
        return userService.getUserById(id)
                .map(user -> {
                    if ("ADMIN".equals(user.getRole())) {
                        throw new CustomExceptions.BusinessLogicException("Cannot deactivate admin users");
                    }
                    user.setRole("INACTIVE");
                    return userService.updateUser(id, user)
                            .map(UserDTO::fromEntity)
                            .map(ResponseEntity::ok)
                            .orElse(ResponseEntity.notFound().build());
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/validate/{email}")
    @Operation(
        summary = "Validate email availability", 
        description = "Check if an email address is available for registration"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Email validation completed"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<Map<String, Object>> validateEmail(
            @Parameter(description = "Email to validate") @PathVariable String email) {
        
        boolean available = userService.getUserByEmail(email).isEmpty();
        
        Map<String, Object> result = Map.of(
            "email", email,
            "available", available,
            "message", available ? "Email is available" : "Email is already taken"
        );
        
        return ResponseEntity.ok(result);
    }

    @PostMapping("/{id}/change-role")
    @Operation(
        summary = "Change user role", 
        description = "Change a user's role. Cannot assign SUPER_ADMIN role"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Role changed successfully"),
            @ApiResponse(responseCode = "404", description = "User not found"),
            @ApiResponse(responseCode = "400", description = "Invalid role"),
            @ApiResponse(responseCode = "422", description = "Cannot assign SUPER_ADMIN role"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<UserDTO> changeUserRole(
            @Parameter(description = "User ID") @PathVariable Long id,
            @Parameter(description = "New role") @RequestParam String role) {
        
        if ("SUPER_ADMIN".equals(role)) {
            throw new CustomExceptions.BusinessLogicException("SUPER_ADMIN role cannot be assigned through this API");
        }
        
        if (!List.of("USER", "ADMIN", "INACTIVE").contains(role)) {
            throw new CustomExceptions.ValidationException("Invalid role. Allowed roles: USER, ADMIN, INACTIVE");
        }
        
        return userService.getUserById(id)
                .map(user -> {
                    user.setRole(role);
                    return userService.updateUser(id, user)
                            .map(UserDTO::fromEntity)
                            .map(ResponseEntity::ok)
                            .orElse(ResponseEntity.notFound().build());
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/health-check")
    @Operation(
        summary = "User service health check", 
        description = "Check the health of the user service and database connectivity"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Service is healthy"),
            @ApiResponse(responseCode = "503", description = "Service is unhealthy")
    })
    public ResponseEntity<Map<String, Object>> healthCheck() {
        try {
            // Simple health check by counting users
            long userCount = userService.getAllUsers().size();
            
            Map<String, Object> health = Map.of(
                "status", "UP",
                "service", "user-service",
                "database", "connected",
                "userCount", userCount,
                "timestamp", System.currentTimeMillis()
            );
            
            return ResponseEntity.ok(health);
        } catch (Exception e) {
            Map<String, Object> health = Map.of(
                "status", "DOWN",
                "service", "user-service",
                "database", "disconnected",
                "error", e.getMessage(),
                "timestamp", System.currentTimeMillis()
            );
            
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(health);
        }
    }

    @PostMapping("/simulate-error")
    @Operation(
        summary = "Simulate random error", 
        description = "Simulate various types of errors for observability testing"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "No error simulated"),
            @ApiResponse(responseCode = "400", description = "Validation error"),
            @ApiResponse(responseCode = "408", description = "Timeout error"),
            @ApiResponse(responseCode = "429", description = "Rate limit error"),
            @ApiResponse(responseCode = "500", description = "Internal server error"),
            @ApiResponse(responseCode = "503", description = "External service error")
    })
    public ResponseEntity<Map<String, Object>> simulateError() {
        int errorType = ThreadLocalRandom.current().nextInt(6);
        
        switch (errorType) {
            case 0:
                throw new CustomExceptions.ValidationException("Simulated validation error");
            case 1:
                throw new CustomExceptions.TimeoutException("Simulated timeout error");
            case 2:
                throw new CustomExceptions.RateLimitException("Simulated rate limit error");
            case 3:
                throw new CustomExceptions.DatabaseConnectionException("Simulated database error");
            case 4:
                throw new CustomExceptions.ExternalServiceException("Simulated external service error");
            default:
                return ResponseEntity.ok(Map.of(
                    "message", "No error simulated",
                    "timestamp", System.currentTimeMillis()
                ));
        }
    }
}
