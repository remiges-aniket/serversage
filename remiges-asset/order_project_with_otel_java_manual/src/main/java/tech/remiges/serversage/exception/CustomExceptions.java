package tech.remiges.serversage.exception;

/**
 * Custom exception classes for different error scenarios in observability showcase
 */
public class CustomExceptions {

    public static class UserNotFoundException extends RuntimeException {
        public UserNotFoundException(String message) {
            super(message);
        }
        public UserNotFoundException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    public static class ProductNotFoundException extends RuntimeException {
        public ProductNotFoundException(String message) {
            super(message);
        }
        public ProductNotFoundException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    public static class OrderNotFoundException extends RuntimeException {
        public OrderNotFoundException(String message) {
            super(message);
        }
        public OrderNotFoundException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    public static class DuplicateEmailException extends RuntimeException {
        public DuplicateEmailException(String message) {
            super(message);
        }
        public DuplicateEmailException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    public static class DuplicateProductException extends RuntimeException {
        public DuplicateProductException(String message) {
            super(message);
        }
        public DuplicateProductException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    public static class InsufficientStockException extends RuntimeException {
        public InsufficientStockException(String message) {
            super(message);
        }
        public InsufficientStockException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    public static class InvalidPriceException extends RuntimeException {
        public InvalidPriceException(String message) {
            super(message);
        }
        public InvalidPriceException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    public static class DatabaseConnectionException extends RuntimeException {
        public DatabaseConnectionException(String message) {
            super(message);
        }
        public DatabaseConnectionException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    public static class ValidationException extends RuntimeException {
        public ValidationException(String message) {
            super(message);
        }
        public ValidationException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    public static class BusinessLogicException extends RuntimeException {
        public BusinessLogicException(String message) {
            super(message);
        }
        public BusinessLogicException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    public static class ExternalServiceException extends RuntimeException {
        public ExternalServiceException(String message) {
            super(message);
        }
        public ExternalServiceException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    public static class TimeoutException extends RuntimeException {
        public TimeoutException(String message) {
            super(message);
        }
        public TimeoutException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    public static class RateLimitException extends RuntimeException {
        public RateLimitException(String message) {
            super(message);
        }
        public RateLimitException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
