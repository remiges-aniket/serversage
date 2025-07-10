package tech.remiges.serversage.service;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.remiges.serversage.exception.CustomExceptions;
import tech.remiges.serversage.model.User;
import tech.remiges.serversage.observability.ObservabilityService;
import tech.remiges.serversage.repository.UserRepository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Enhanced User Service with comprehensive observability
 * Features:
 * - Full OpenTelemetry integration with traces, metrics, and logs
 * - Database operation monitoring with duration tracking
 * - Error scenarios for testing observability
 * - Business metrics tracking
 * - Cache operation monitoring
 */
@Service
@Transactional
public class UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);
    private final UserRepository userRepository;
    private final ObservabilityService observabilityService;

    @Autowired
    public UserService(UserRepository userRepository, ObservabilityService observabilityService) {
        this.userRepository = userRepository;
        this.observabilityService = observabilityService;
    }

    public List<User> getAllUsers() {
        long startTime = System.nanoTime();
        
        // Create a detailed span for this database operation
        Span span = observabilityService.getTracer().spanBuilder("UserService.getAllUsers")
                .setSpanKind(SpanKind.CLIENT)
                .setAttribute("db.system", "postgresql")
                .setAttribute("db.operation.name", "SELECT")
                .setAttribute("db.sql.table", "users")
                .setAttribute("db.statement", "SELECT u.id, u.name, u.email, u.role FROM users u")
                .setAttribute("service.method", "getAllUsers")
                .startSpan();
        
        try (var scope = span.makeCurrent()) {
            observabilityService.logInfo("🔍 Executing SQL: SELECT u.id, u.name, u.email, u.role FROM users u");
            
            simulateRandomDelay();
            
            // Simulate potential database error
            if (ThreadLocalRandom.current().nextInt(100) < 2) { // 2% chance
                throw new CustomExceptions.DatabaseConnectionException("Database connection timeout while fetching users");
            }
            
            List<User> users = userRepository.findAll();
            
            double durationSeconds = (System.nanoTime() - startTime) / 1_000_000_000.0;
            
            // Add detailed span attributes
            span.setAttribute("db.rows_affected", users.size());
            span.setAttribute("db.query.duration_ms", durationSeconds * 1000);
            span.setAttribute("db.query.success", true);
            span.setAttribute("db.result.count", users.size());
            
            observabilityService.recordDatabaseOperation("SELECT", "users", durationSeconds, true, null);
            
            // Calculate statistics for business metrics
            long totalUsers = users.size();
            long activeUsers = users.stream()
                .filter(user -> user.getRole() != null && !"INACTIVE".equals(user.getRole()))
                .count();
            
            observabilityService.updateBusinessMetrics(totalUsers, 0, 0, activeUsers);
            
            observabilityService.logInfo("✅ Query completed successfully. Retrieved " + users.size() + 
                    " users in " + String.format("%.3f", durationSeconds) + "s");
            
            span.setStatus(StatusCode.OK);
            return users;
            
        } catch (Exception e) {
            span.recordException(e);
            span.setStatus(StatusCode.ERROR, e.getMessage());
            span.setAttribute("db.query.success", false);
            span.setAttribute("error.type", e.getClass().getSimpleName());
            
            observabilityService.logError("❌ SQL Query failed: " + e.getMessage());
            throw e;
        } finally {
            span.end();
        }
    }

    public Optional<User> getUserById(Long id) {
        return observabilityService.executeInSpan("UserService.getUserById", "user-service", () -> {
            long startTime = System.nanoTime();
            
            // Trigger specific error scenarios for testing
            if (id != null) {
                if (id == 999L) {
                    // Simulate array index out of bounds
                    int[] testArray = new int[5];
                    int value = testArray[10]; // This will throw ArrayIndexOutOfBoundsException
                }
                if (id == 998L) {
                    // Simulate null pointer exception
                    String nullString = null;
                    int length = nullString.length(); // This will throw NullPointerException
                }
                if (id == 997L) {
                    // Simulate rate limiting
                    throw new CustomExceptions.RateLimitException("Rate limit exceeded for user lookup");
                }
            }
            
            observabilityService.logInfo("Starting to fetch user by ID", Attributes.builder()
                    .put("operation", "getUserById")
                    .put("user.id", id != null ? id : -1)
                    .build());
            
            simulateRandomDelay();
            
            Optional<User> user = userRepository.findById(id);
            
            double durationSeconds = (System.nanoTime() - startTime) / 1_000_000_000.0;
            observabilityService.recordDatabaseOperation("SELECT", "users", durationSeconds, true, null);
            
            if (user.isPresent()) {
                observabilityService.logInfo("User found by ID", Attributes.builder()
                        .put("operation", "getUserById")
                        .put("user.id", id)
                        .put("user.email", user.get().getEmail())
                        .put("duration_seconds", durationSeconds)
                        .put("found", true)
                        .build());
            } else {
                observabilityService.logWarning("User not found by ID", Attributes.builder()
                        .put("operation", "getUserById")
                        .put("user.id", id)
                        .put("duration_seconds", durationSeconds)
                        .put("found", false)
                        .build());
            }
            
            return user;
        });
    }

    public Optional<User> getUserByEmail(String email) {
        return observabilityService.executeInSpan("UserService.getUserByEmail", "user-service", () -> {
            long startTime = System.nanoTime();
            
            // Simulate database error for specific emails
            if (email != null && email.toLowerCase().contains("dberror")) {
                throw new CustomExceptions.DatabaseConnectionException("Database connection failed for email lookup");
            }
            
            observabilityService.logInfo("Starting to fetch user by email", Attributes.builder()
                    .put("operation", "getUserByEmail")
                    .put("user.email", email != null ? email : "null")
                    .build());
            
            simulateRandomDelay();
            
            Optional<User> user = userRepository.findByEmail(email);
            
            double durationSeconds = (System.nanoTime() - startTime) / 1_000_000_000.0;
            observabilityService.recordDatabaseOperation("SELECT", "users", durationSeconds, true, null);
            
            observabilityService.logInfo("User email lookup completed", Attributes.builder()
                    .put("operation", "getUserByEmail")
                    .put("user.email", email)
                    .put("found", user.isPresent())
                    .put("duration_seconds", durationSeconds)
                    .build());
            
            return user;
        });
    }

    public List<User> getUsersByRole(String role) {
        return observabilityService.executeInSpan("UserService.getUsersByRole", "user-service", () -> {
            long startTime = System.nanoTime();
            
            observabilityService.logInfo("Fetching users by role", Attributes.builder()
                    .put("operation", "getUsersByRole")
                    .put("user.role", role != null ? role : "null")
                    .build());
            
            simulateRandomDelay();
            List<User> users = userRepository.findByRole(role);
            
            double durationSeconds = (System.nanoTime() - startTime) / 1_000_000_000.0;
            observabilityService.recordDatabaseOperation("SELECT", "users", durationSeconds, true, null);
            
            observabilityService.logInfo("Users fetched by role", Attributes.builder()
                    .put("operation", "getUsersByRole")
                    .put("user.role", role)
                    .put("user.count", users.size())
                    .put("duration_seconds", durationSeconds)
                    .build());
            
            return users;
        });
    }

    public List<User> searchUsers(String keyword) {
        return observabilityService.executeInSpan("UserService.searchUsers", "user-service", () -> {
            long startTime = System.nanoTime();
            
            if (keyword == null || keyword.trim().isEmpty()) {
                throw new CustomExceptions.ValidationException("Search keyword cannot be empty");
            }
            
            observabilityService.logInfo("Searching users", Attributes.builder()
                    .put("operation", "searchUsers")
                    .put("search.keyword", keyword)
                    .build());
            
            simulateRandomDelay();
            List<User> users = userRepository.searchUsers(keyword);
            
            double durationSeconds = (System.nanoTime() - startTime) / 1_000_000_000.0;
            observabilityService.recordDatabaseOperation("SELECT", "users", durationSeconds, true, null);
            
            observabilityService.logInfo("User search completed", Attributes.builder()
                    .put("operation", "searchUsers")
                    .put("search.keyword", keyword)
                    .put("user.count", users.size())
                    .put("duration_seconds", durationSeconds)
                    .build());
            
            return users;
        });
    }

    public Long getUserCountByRole(String role) {
        return observabilityService.executeInSpan("UserService.getUserCountByRole", "user-service", () -> {
            long startTime = System.nanoTime();
            
            observabilityService.logInfo("Getting user count by role", Attributes.builder()
                    .put("operation", "getUserCountByRole")
                    .put("user.role", role != null ? role : "null")
                    .build());
            
            simulateRandomDelay();
            Long count = userRepository.countByRole(role);
            
            double durationSeconds = (System.nanoTime() - startTime) / 1_000_000_000.0;
            observabilityService.recordDatabaseOperation("SELECT", "users", durationSeconds, true, null);
            
            observabilityService.logInfo("User count by role retrieved", Attributes.builder()
                    .put("operation", "getUserCountByRole")
                    .put("user.role", role)
                    .put("user.count", count)
                    .put("duration_seconds", durationSeconds)
                    .build());
            
            return count;
        });
    }

    public List<User> createMultipleUsers(List<User> users) {
        return observabilityService.executeInSpan("UserService.createMultipleUsers", "user-service", () -> {
            long startTime = System.nanoTime();
            
            observabilityService.logInfo("Creating multiple users in batch", Attributes.builder()
                    .put("operation", "createMultipleUsers")
                    .put("user.batch_size", users.size())
                    .build());
            
            // Validate all users first
            for (User user : users) {
                String validationResult = validateUser(user);
                if (!validationResult.equals("Valid")) {
                    throw new CustomExceptions.ValidationException("Validation failed for user: " + validationResult);
                }
            }
            
            try {
                simulateRandomDelay();
                List<User> savedUsers = userRepository.saveAll(users);
                
                double durationSeconds = (System.nanoTime() - startTime) / 1_000_000_000.0;
                observabilityService.recordDatabaseOperation("INSERT_BATCH", "users", durationSeconds, true, null);
                
                observabilityService.logInfo("Batch user creation completed", Attributes.builder()
                        .put("operation", "createMultipleUsers")
                        .put("user.batch_size", users.size())
                        .put("user.created_count", savedUsers.size())
                        .put("duration_seconds", durationSeconds)
                        .build());
                
                return savedUsers;
            } catch (DataIntegrityViolationException e) {
                double durationSeconds = (System.nanoTime() - startTime) / 1_000_000_000.0;
                observabilityService.recordDatabaseOperation("INSERT_BATCH", "users", durationSeconds, false, "DATA_INTEGRITY_VIOLATION");
                
                observabilityService.logError("Data integrity violation during batch user creation", e, Attributes.builder()
                        .put("operation", "createMultipleUsers")
                        .put("user.batch_size", users.size())
                        .put("duration_seconds", durationSeconds)
                        .build());
                
                throw new CustomExceptions.DuplicateEmailException("One or more users have duplicate email addresses", e);
            }
        });
    }

    public Map<String, Object> getUserProfile(Long id) {
        // Simple, clean service method without nested spans
        long startTime = System.nanoTime();
        
        observabilityService.logInfo("Getting user profile for ID: " + id);
        
        User user = userRepository.findById(id)
                .orElseThrow(() -> new CustomExceptions.UserNotFoundException("User not found with id: " + id));
        
        Map<String, Object> profile = new HashMap<>();
        profile.put("id", user.getId());
        profile.put("name", user.getName());
        profile.put("email", user.getEmail());
        profile.put("role", user.getRole());
        profile.put("accountStatus", "ACTIVE");
        profile.put("lastLogin", "2024-07-05T10:30:00");
        profile.put("createdAt", "2024-01-01T00:00:00");
        
        simulateRandomDelay();
        
        double durationSeconds = (System.nanoTime() - startTime) / 1_000_000_000.0;
        observabilityService.recordDatabaseOperation("SELECT", "users", durationSeconds, true, null);
        
        observabilityService.logInfo("User profile retrieved successfully for ID: " + id + 
                " in " + String.format("%.3f", durationSeconds) + "s");
        
        return profile;
    }

    // Using thread pool for concurrent processing
    public CompletableFuture<User> createUserAsync(User user) {
        observabilityService.logInfo("Creating user asynchronously", Attributes.builder()
                .put("operation", "createUserAsync")
                .put("user.email", user.getEmail() != null ? user.getEmail() : "null")
                .build());
        
        return CompletableFuture.supplyAsync(
                () -> createUser(user), Executors.newCachedThreadPool());
    }

    public User createUser(User user) {
        return observabilityService.executeInSpan("UserService.createUser", "user-service", () -> {
            long startTime = System.nanoTime();
            
            observabilityService.logInfo("Starting user creation", Attributes.builder()
                    .put("operation", "createUser")
                    .put("user.email", user.getEmail() != null ? user.getEmail() : "null")
                    .put("user.name", user.getName() != null ? user.getName() : "null")
                    .build());
            
            // Validate user
            String validationResult = validateUser(user);
            if (!validationResult.equals("Valid")) {
                throw new CustomExceptions.ValidationException(validationResult);
            }
            
            // Check for duplicate email
            if (userRepository.existsByEmail(user.getEmail())) {
                observabilityService.logError("Duplicate email detected", 
                        new CustomExceptions.DuplicateEmailException("User with email '" + user.getEmail() + "' already exists"),
                        Attributes.builder()
                                .put("operation", "createUser")
                                .put("user.email", user.getEmail())
                                .build());
                throw new CustomExceptions.DuplicateEmailException("User with email '" + user.getEmail() + "' already exists");
            }
            
            // Simulate various error scenarios for testing
            if (user.getEmail() != null) {
                if (user.getEmail().toLowerCase().contains("dberror")) {
                    throw new CustomExceptions.DatabaseConnectionException("Database connection failed while creating user");
                }
                if (user.getEmail().toLowerCase().contains("timeout")) {
                    throw new CustomExceptions.TimeoutException("Request timeout while creating user");
                }
                if (user.getEmail().toLowerCase().contains("external")) {
                    throw new CustomExceptions.ExternalServiceException("External service unavailable for user validation");
                }
            }
            
            // Business logic validation
            if (user.getName() != null && user.getName().toLowerCase().contains("admin") && 
                !"ADMIN".equals(user.getRole())) {
                throw new CustomExceptions.BusinessLogicException("Users with 'admin' in name must have ADMIN role");
            }
            
            try {
                simulateRandomDelay();
                User savedUser = userRepository.save(user);
                
                double durationSeconds = (System.nanoTime() - startTime) / 1_000_000_000.0;
                observabilityService.recordDatabaseOperation("INSERT", "users", durationSeconds, true, null);
                
                observabilityService.logInfo("User created successfully", Attributes.builder()
                        .put("operation", "createUser")
                        .put("user.id", savedUser.getId())
                        .put("user.email", savedUser.getEmail())
                        .put("duration_seconds", durationSeconds)
                        .build());
                
                return savedUser;
            } catch (DataIntegrityViolationException e) {
                double durationSeconds = (System.nanoTime() - startTime) / 1_000_000_000.0;
                observabilityService.recordDatabaseOperation("INSERT", "users", durationSeconds, false, "DATA_INTEGRITY_VIOLATION");
                
                observabilityService.logError("Data integrity violation during user creation", e, Attributes.builder()
                        .put("operation", "createUser")
                        .put("user.email", user.getEmail())
                        .put("duration_seconds", durationSeconds)
                        .build());
                
                throw new CustomExceptions.DuplicateEmailException("User with this email already exists", e);
            }
        });
    }

    public Optional<User> updateUser(Long id, User userDetails) {
        return observabilityService.executeInSpan("UserService.updateUser", "user-service", () -> {
            long startTime = System.nanoTime();
            
            observabilityService.logInfo("Starting user update", Attributes.builder()
                    .put("operation", "updateUser")
                    .put("user.id", id != null ? id : -1)
                    .build());
            
            return userRepository.findById(id)
                    .map(existingUser -> {
                        // Simulate business logic error for specific scenarios
                        if (userDetails.getRole() != null && "SUPER_ADMIN".equals(userDetails.getRole())) {
                            throw new CustomExceptions.BusinessLogicException("SUPER_ADMIN role cannot be assigned through this API");
                        }
                        
                        existingUser.setName(userDetails.getName());
                        existingUser.setEmail(userDetails.getEmail());
                        existingUser.setRole(userDetails.getRole());
                        
                        simulateRandomDelay();
                        User updatedUser = userRepository.save(existingUser);
                        
                        double durationSeconds = (System.nanoTime() - startTime) / 1_000_000_000.0;
                        observabilityService.recordDatabaseOperation("UPDATE", "users", durationSeconds, true, null);
                        
                        observabilityService.logInfo("User updated successfully", Attributes.builder()
                                .put("operation", "updateUser")
                                .put("user.id", updatedUser.getId())
                                .put("user.email", updatedUser.getEmail())
                                .put("duration_seconds", durationSeconds)
                                .build());
                        
                        return updatedUser;
                    });
        });
    }

    public void deleteUser(Long id) {
        observabilityService.executeInSpan("UserService.deleteUser", "user-service", () -> {
            long startTime = System.nanoTime();
            
            observabilityService.logInfo("Starting user deletion", Attributes.builder()
                    .put("operation", "deleteUser")
                    .put("user.id", id != null ? id : -1)
                    .build());
            
            if (!userRepository.existsById(id)) {
                throw new CustomExceptions.UserNotFoundException("User not found with id: " + id);
            }
            
            simulateRandomDelay();
            userRepository.deleteById(id);
            
            double durationSeconds = (System.nanoTime() - startTime) / 1_000_000_000.0;
            observabilityService.recordDatabaseOperation("DELETE", "users", durationSeconds, true, null);
            
            observabilityService.logInfo("User deleted successfully", Attributes.builder()
                    .put("operation", "deleteUser")
                    .put("user.id", id)
                    .put("duration_seconds", durationSeconds)
                    .build());
        });
    }

    public String validateUser(User user) {
        if (user == null) {
            return "User cannot be null";
        }
        if (user.getName() == null || user.getName().isBlank()) {
            return "Name cannot be empty";
        }
        if (user.getEmail() == null || user.getEmail().isBlank()) {
            return "Email cannot be empty";
        }
        if (!user.getEmail().contains("@")) {
            return "Email format is invalid";
        }
        return "Valid";
    }

    private void simulateRandomDelay() {
        try {
            // Random delay between 10-100ms to simulate real database operations
            Thread.sleep(ThreadLocalRandom.current().nextInt(10, 101));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
