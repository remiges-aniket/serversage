package tech.remiges.serversage.exception;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.common.Attributes;
import tech.remiges.serversage.observability.ObservabilityService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Global exception handler with comprehensive observability integration
 * Features:
 * - Automatic error tracking and correlation
 * - Structured error logging with trace context
 * - Error metrics with exemplar support
 * - Span error recording for distributed tracing
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private final ObservabilityService observabilityService;

    public GlobalExceptionHandler(ObservabilityService observabilityService) {
        this.observabilityService = observabilityService;
    }

    @ExceptionHandler(CustomExceptions.UserNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleUserNotFoundException(CustomExceptions.UserNotFoundException ex, WebRequest request) {
        return handleException(ex, HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "user-service", request);
    }

    @ExceptionHandler(CustomExceptions.ProductNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleProductNotFoundException(CustomExceptions.ProductNotFoundException ex, WebRequest request) {
        return handleException(ex, HttpStatus.NOT_FOUND, "PRODUCT_NOT_FOUND", "product-service", request);
    }

    @ExceptionHandler(CustomExceptions.OrderNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleOrderNotFoundException(CustomExceptions.OrderNotFoundException ex, WebRequest request) {
        return handleException(ex, HttpStatus.NOT_FOUND, "ORDER_NOT_FOUND", "order-service", request);
    }

    @ExceptionHandler(CustomExceptions.DuplicateEmailException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateEmailException(CustomExceptions.DuplicateEmailException ex, WebRequest request) {
        return handleException(ex, HttpStatus.CONFLICT, "DUPLICATE_EMAIL", "user-service", request);
    }

    @ExceptionHandler(CustomExceptions.DuplicateProductException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateProductException(CustomExceptions.DuplicateProductException ex, WebRequest request) {
        return handleException(ex, HttpStatus.CONFLICT, "DUPLICATE_PRODUCT", "product-service", request);
    }

    @ExceptionHandler(CustomExceptions.InsufficientStockException.class)
    public ResponseEntity<ErrorResponse> handleInsufficientStockException(CustomExceptions.InsufficientStockException ex, WebRequest request) {
        return handleException(ex, HttpStatus.BAD_REQUEST, "INSUFFICIENT_STOCK", "inventory-service", request);
    }

    @ExceptionHandler(CustomExceptions.InvalidPriceException.class)
    public ResponseEntity<ErrorResponse> handleInvalidPriceException(CustomExceptions.InvalidPriceException ex, WebRequest request) {
        return handleException(ex, HttpStatus.BAD_REQUEST, "INVALID_PRICE", "validation-service", request);
    }

    @ExceptionHandler(CustomExceptions.DatabaseConnectionException.class)
    public ResponseEntity<ErrorResponse> handleDatabaseConnectionException(CustomExceptions.DatabaseConnectionException ex, WebRequest request) {
        return handleException(ex, HttpStatus.INTERNAL_SERVER_ERROR, "DATABASE_ERROR", "database-service", request);
    }

    @ExceptionHandler(CustomExceptions.ValidationException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(CustomExceptions.ValidationException ex, WebRequest request) {
        return handleException(ex, HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "validation-service", request);
    }

    @ExceptionHandler(CustomExceptions.BusinessLogicException.class)
    public ResponseEntity<ErrorResponse> handleBusinessLogicException(CustomExceptions.BusinessLogicException ex, WebRequest request) {
        return handleException(ex, HttpStatus.UNPROCESSABLE_ENTITY, "BUSINESS_LOGIC_ERROR", "business-service", request);
    }

    @ExceptionHandler(CustomExceptions.ExternalServiceException.class)
    public ResponseEntity<ErrorResponse> handleExternalServiceException(CustomExceptions.ExternalServiceException ex, WebRequest request) {
        return handleException(ex, HttpStatus.SERVICE_UNAVAILABLE, "EXTERNAL_SERVICE_ERROR", "external-service", request);
    }

    @ExceptionHandler(CustomExceptions.TimeoutException.class)
    public ResponseEntity<ErrorResponse> handleTimeoutException(CustomExceptions.TimeoutException ex, WebRequest request) {
        return handleException(ex, HttpStatus.REQUEST_TIMEOUT, "TIMEOUT_ERROR", "timeout-service", request);
    }

    @ExceptionHandler(CustomExceptions.RateLimitException.class)
    public ResponseEntity<ErrorResponse> handleRateLimitException(CustomExceptions.RateLimitException ex, WebRequest request) {
        return handleException(ex, HttpStatus.TOO_MANY_REQUESTS, "RATE_LIMIT_EXCEEDED", "rate-limiter", request);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolationException(DataIntegrityViolationException ex, WebRequest request) {
        return handleException(ex, HttpStatus.CONFLICT, "DATA_INTEGRITY_VIOLATION", "database-service", request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationExceptions(MethodArgumentNotValidException ex, WebRequest request) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        ErrorResponse errorResponse = new ErrorResponse(
                LocalDateTime.now(),
                HttpStatus.BAD_REQUEST.value(),
                "VALIDATION_FAILED",
                "Validation failed for request",
                request.getDescription(false),
                errors,
                observabilityService.getCurrentTraceId(),
                observabilityService.getCurrentSpanId()
        );

        recordComprehensiveError(ex, "VALIDATION_FAILED", "validation-service", request);

        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(ArrayIndexOutOfBoundsException.class)
    public ResponseEntity<ErrorResponse> handleArrayIndexOutOfBoundsException(ArrayIndexOutOfBoundsException ex, WebRequest request) {
        return handleException(ex, HttpStatus.INTERNAL_SERVER_ERROR, "ARRAY_INDEX_OUT_OF_BOUNDS", "application", request);
    }

    @ExceptionHandler(NullPointerException.class)
    public ResponseEntity<ErrorResponse> handleNullPointerException(NullPointerException ex, WebRequest request) {
        return handleException(ex, HttpStatus.INTERNAL_SERVER_ERROR, "NULL_POINTER_EXCEPTION", "application", request);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(IllegalArgumentException ex, WebRequest request) {
        return handleException(ex, HttpStatus.BAD_REQUEST, "ILLEGAL_ARGUMENT", "validation-service", request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex, WebRequest request) {
        return handleException(ex, HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_SERVER_ERROR", "application", request);
    }

    /**
     * Centralized exception handling with comprehensive observability
     */
    private ResponseEntity<ErrorResponse> handleException(Exception ex, HttpStatus status, String errorCode, String component, WebRequest request) {
        ErrorResponse errorResponse = new ErrorResponse(
                LocalDateTime.now(),
                status.value(),
                errorCode,
                ex.getMessage(),
                request.getDescription(false),
                null,
                observabilityService.getCurrentTraceId(),
                observabilityService.getCurrentSpanId()
        );

        recordComprehensiveError(ex, errorCode, component, request);

        return new ResponseEntity<>(errorResponse, status);
    }

    /**
     * Records comprehensive error information across all observability signals
     */
    private void recordComprehensiveError(Exception ex, String errorCode, String component, WebRequest request) {
        // Record error metrics with exemplar support
        observabilityService.recordError(errorCode, component, getOperationFromRequest(request), ex);

        // Record span error for distributed tracing
        recordSpanError(ex, errorCode, component);

        // Log structured error with trace correlation
        observabilityService.logError("Application error occurred", ex, Attributes.builder()
                .put("error.code", errorCode)
                .put("component", component)
                .put("http.path", request.getDescription(false))
                .put("error.handled", true)
                .build());

        // Also log to standard logger for backward compatibility
        logger.error("Error occurred - Code: {}, Component: {}, Path: {}, Message: {}", 
                errorCode, component, request.getDescription(false), ex.getMessage(), ex);
    }

    /**
     * Records error information in the current span
     */
    private void recordSpanError(Exception ex, String errorCode, String component) {
        Span currentSpan = Span.current();
        if (currentSpan != null && currentSpan.getSpanContext().isValid()) {
            currentSpan.setStatus(StatusCode.ERROR, ex.getMessage());
            currentSpan.setAllAttributes(Attributes.builder()
                    .put("error.type", errorCode)
                    .put("error.message", ex.getMessage() != null ? ex.getMessage() : "")
                    .put("component", component)
                    .put("exception.type", ex.getClass().getSimpleName())
                    .build());
            currentSpan.recordException(ex);
        }
    }

    /**
     * Extracts operation name from request for better error categorization
     */
    private String getOperationFromRequest(WebRequest request) {
        String description = request.getDescription(false);
        if (description.contains("uri=")) {
            String uri = description.substring(description.indexOf("uri=") + 4);
            return uri.replaceAll("/\\d+", "/{id}")
                     .replaceAll("/[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12}", "/{uuid}");
        }
        return "unknown";
    }

    /**
     * Enhanced error response with trace correlation information
     */
    public record ErrorResponse(
            LocalDateTime timestamp,
            int status,
            String error,
            String message,
            String path,
            Map<String, String> validationErrors,
            String traceId,
            String spanId
    ) {}
}
