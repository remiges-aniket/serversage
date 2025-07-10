package tech.remiges.serversage.observability;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.logs.Logger;
import io.opentelemetry.api.logs.Severity;
import io.opentelemetry.api.metrics.DoubleHistogram;
import io.opentelemetry.api.metrics.LongCounter;
import io.opentelemetry.api.metrics.LongUpDownCounter;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.concurrent.TimeUnit;

@Service
public class ObservabilityService {

    private static final org.slf4j.Logger slf4jLogger = LoggerFactory.getLogger(ObservabilityService.class);

    private final Tracer tracer;
    private final Meter meter;
    private final Logger otelLogger;

    // Metrics
    private final LongCounter httpRequestsTotal;
    private final DoubleHistogram httpRequestDuration;
    private final LongCounter databaseOperationsTotal;
    private final DoubleHistogram databaseOperationDuration;
    private final LongCounter errorsTotal;
    private final LongUpDownCounter activeUsers;
    private final LongUpDownCounter activeProducts;
    private final LongUpDownCounter activeOrders;
    private final LongUpDownCounter activeSessions;

    // Attribute keys
    private static final AttributeKey<String> HTTP_METHOD = AttributeKey.stringKey("http.method");
    private static final AttributeKey<String> HTTP_ROUTE = AttributeKey.stringKey("http.route");
    private static final AttributeKey<Long> HTTP_STATUS_CODE = AttributeKey.longKey("http.status_code");
    private static final AttributeKey<String> ERROR_TYPE = AttributeKey.stringKey("error.type");
    private static final AttributeKey<String> DB_OPERATION = AttributeKey.stringKey("db.operation");
    private static final AttributeKey<String> DB_TABLE = AttributeKey.stringKey("db.table");
    private static final AttributeKey<String> USER_ID = AttributeKey.stringKey("user.id");
    private static final AttributeKey<String> USER_ROLE = AttributeKey.stringKey("user.role");

    public ObservabilityService(OpenTelemetry openTelemetry) {
        this.tracer = openTelemetry.getTracer("serversage");
        this.meter = openTelemetry.getMeter("serversage");
        this.otelLogger = openTelemetry.getLogsBridge().get("serversage");

        // Initialize metrics
        this.httpRequestsTotal = meter
                .counterBuilder("serversage_http_requests_total")
                .setDescription("Total number of HTTP requests")
                .build();

        this.httpRequestDuration = meter
                .histogramBuilder("serversage_http_request_duration_seconds")
                .setDescription("HTTP request duration in seconds")
                .setUnit("s")
                .build();

        this.databaseOperationsTotal = meter
                .counterBuilder("serversage_database_operations_total")
                .setDescription("Total number of database operations")
                .build();

        this.databaseOperationDuration = meter
                .histogramBuilder("serversage_database_operation_duration_seconds")
                .setDescription("Database operation duration in seconds")
                .setUnit("s")
                .build();

        this.errorsTotal = meter
                .counterBuilder("serversage_errors_total")
                .setDescription("Total number of errors by type")
                .build();

        this.activeUsers = meter
                .upDownCounterBuilder("serversage_users_total")
                .setDescription("Total number of users")
                .build();

        this.activeProducts = meter
                .upDownCounterBuilder("serversage_products_total")
                .setDescription("Total number of products")
                .build();

        this.activeOrders = meter
                .upDownCounterBuilder("serversage_orders_total")
                .setDescription("Total number of orders")
                .build();

        this.activeSessions = meter
                .upDownCounterBuilder("serversage_active_sessions")
                .setDescription("Number of active sessions")
                .build();
    }

    public void recordHttpRequest(String method, String route, int statusCode, long durationMs) {
        Attributes attributes = Attributes.of(
                HTTP_METHOD, method,
                HTTP_ROUTE, route,
                HTTP_STATUS_CODE, (long) statusCode
        );

        httpRequestsTotal.add(1, attributes);
        httpRequestDuration.record(durationMs / 1000.0, attributes);

        // Log with trace correlation
        logWithTraceContext("HTTP Request", 
            String.format("Method: %s, Route: %s, Status: %d, Duration: %dms", 
                method, route, statusCode, durationMs),
            statusCode >= 400 ? Severity.ERROR : Severity.INFO);
    }

    public void recordDatabaseOperation(String operation, String table, long durationMs, boolean success) {
        Attributes attributes = Attributes.of(
                DB_OPERATION, operation,
                DB_TABLE, table
        );

        databaseOperationsTotal.add(1, attributes);
        databaseOperationDuration.record(durationMs / 1000.0, attributes);

        if (!success) {
            recordError("DatabaseError", "Database operation failed: " + operation + " on " + table);
        }

        // Log with trace correlation
        logWithTraceContext("Database Operation", 
            String.format("Operation: %s, Table: %s, Duration: %dms, Success: %b", 
                operation, table, durationMs, success),
            success ? Severity.INFO : Severity.ERROR);
    }

    public void recordError(String errorType, String errorMessage) {
        Attributes attributes = Attributes.of(ERROR_TYPE, errorType);
        errorsTotal.add(1, attributes);

        // Add error to current span
        Span currentSpan = Span.current();
        if (currentSpan != null) {
            currentSpan.recordException(new RuntimeException(errorMessage));
            currentSpan.setStatus(StatusCode.ERROR, errorMessage);
        }

        // Log error with trace correlation
        logWithTraceContext("Error Recorded", 
            String.format("Type: %s, Message: %s", errorType, errorMessage),
            Severity.ERROR);
    }

    public void updateUserCount(long count) {
        activeUsers.add(count);
        logWithTraceContext("User Count Updated", "Total users: " + count, Severity.INFO);
    }

    public void updateProductCount(long count) {
        activeProducts.add(count);
        logWithTraceContext("Product Count Updated", "Total products: " + count, Severity.INFO);
    }

    public void updateOrderCount(long count) {
        activeOrders.add(count);
        logWithTraceContext("Order Count Updated", "Total orders: " + count, Severity.INFO);
    }

    public void updateActiveSessionCount(long count) {
        activeSessions.add(count);
        logWithTraceContext("Active Session Count Updated", "Active sessions: " + count, Severity.INFO);
    }

    public Span startSpan(String spanName) {
        return tracer.spanBuilder(spanName).startSpan();
    }

    public Span startSpan(String spanName, Attributes attributes) {
        return tracer.spanBuilder(spanName)
                .setAllAttributes(attributes)
                .startSpan();
    }

    public Span startSpan(String spanName, io.opentelemetry.api.trace.SpanKind spanKind) {
        return tracer.spanBuilder(spanName)
                .setSpanKind(spanKind)
                .startSpan();
    }

    public void addSpanAttribute(String key, String value) {
        Span currentSpan = Span.current();
        if (currentSpan != null) {
            currentSpan.setAttribute(key, value);
        }
    }

    public void addSpanAttribute(String key, long value) {
        Span currentSpan = Span.current();
        if (currentSpan != null) {
            currentSpan.setAttribute(key, value);
        }
    }

    public void addUserContextToSpan(String userId, String userRole) {
        Span currentSpan = Span.current();
        if (currentSpan != null) {
            currentSpan.setAllAttributes(Attributes.of(
                    USER_ID, userId,
                    USER_ROLE, userRole
            ));
        }
    }

    private void logWithTraceContext(String event, String message, Severity severity) {
        // Get current trace context
        Span currentSpan = Span.current();
        String traceId = currentSpan.getSpanContext().getTraceId();
        String spanId = currentSpan.getSpanContext().getSpanId();

        // Add to MDC for SLF4J correlation
        MDC.put("traceId", traceId);
        MDC.put("spanId", spanId);

        try {
            // Log via SLF4J (will be correlated)
            switch (severity) {
                case ERROR:
                    slf4jLogger.error("[{}] {}", event, message);
                    break;
                case WARN:
                    slf4jLogger.warn("[{}] {}", event, message);
                    break;
                case INFO:
                    slf4jLogger.info("[{}] {}", event, message);
                    break;
                default:
                    slf4jLogger.debug("[{}] {}", event, message);
            }

            // Also log via OpenTelemetry Logs API
            otelLogger.logRecordBuilder()
                    .setTimestamp(Instant.now())
                    .setSeverity(severity)
                    .setSeverityText(severity.name())
                    .setBody(String.format("[%s] %s", event, message))
                    .setContext(Context.current())
                    .emit();

        } finally {
            // Clean up MDC
            MDC.remove("traceId");
            MDC.remove("spanId");
        }
    }

    public void logInfo(String message) {
        logWithTraceContext("INFO", message, Severity.INFO);
    }

    public void logError(String message) {
        logWithTraceContext("ERROR", message, Severity.ERROR);
    }

    public void logWarn(String message) {
        logWithTraceContext("WARN", message, Severity.WARN);
    }

    public void logDebug(String message) {
        logWithTraceContext("DEBUG", message, Severity.DEBUG);
    }

    // Compatibility methods for existing code
    public void updateBusinessMetrics(long userCount, int productCount, int orderCount, long sessionCount) {
        updateUserCount(userCount);
        updateProductCount(productCount);
        updateOrderCount(orderCount);
        updateActiveSessionCount(sessionCount);
    }

    public void logWarning(String message, Attributes attributes) {
        logWarn(message);
    }

    public void logInfo(String message, Attributes attributes) {
        logInfo(message);
    }

    public void logError(String message, Exception exception, Attributes attributes) {
        logError(message + ": " + exception.getMessage());
    }

    // Overloaded recordDatabaseOperation methods
    public void recordDatabaseOperation(String operation, String table, double durationMs, boolean success, String details) {
        recordDatabaseOperation(operation, table, (long) durationMs, success);
    }

    public void recordDatabaseOperation(String operation, String table, double durationMs, boolean success, Object nullParam) {
        recordDatabaseOperation(operation, table, (long) durationMs, success);
    }

    // Overloaded recordHttpRequest method
    public void recordHttpRequest(String method, String route, int statusCode, double durationMs, String details) {
        recordHttpRequest(method, route, statusCode, (long) durationMs);
    }

    public void recordHttpRequest(String method, String route, int statusCode, double durationMs, Object nullParam) {
        recordHttpRequest(method, route, statusCode, (long) durationMs);
    }

    // Overloaded recordError methods
    public void recordError(String errorType, String errorMessage, String details, Exception exception) {
        recordError(errorType, errorMessage + " - " + details + ": " + exception.getMessage());
    }

    // Methods for getting current trace context
    public String getCurrentTraceId() {
        Span currentSpan = Span.current();
        return currentSpan.getSpanContext().getTraceId();
    }

    public String getCurrentSpanId() {
        Span currentSpan = Span.current();
        return currentSpan.getSpanContext().getSpanId();
    }
    
    public Tracer getTracer() {
        return this.tracer;
    }

    // Utility method for executing code within a properly isolated span
    public <T> T executeInSpan(String spanName, String component, java.util.function.Supplier<T> supplier) {
        // Create a new span with proper isolation
        Span span = tracer.spanBuilder(spanName)
                .setAttribute("component", component)
                .setAttribute("service.name", "serversage")
                .setAttribute("operation.type", extractOperationType(spanName))
                .setSpanKind(SpanKind.INTERNAL)
                .startSpan();
        
        try (var scope = span.makeCurrent()) {
            // Add operation-specific attributes
            span.setAttribute("span.kind", "internal");
            span.setAttribute("operation.name", spanName);
            
            T result = supplier.get();
            span.setStatus(StatusCode.OK);
            span.setAttribute("operation.success", true);
            return result;
        } catch (Exception e) {
            span.recordException(e);
            span.setStatus(StatusCode.ERROR, e.getMessage());
            span.setAttribute("operation.success", false);
            span.setAttribute("error.type", e.getClass().getSimpleName());
            recordError(e.getClass().getSimpleName(), e.getMessage());
            throw e;
        } finally {
            span.end();
        }
    }

    // Utility method for executing void operations within a properly isolated span
    public void executeInSpan(String spanName, String component, Runnable runnable) {
        // Create a new span with proper isolation
        Span span = tracer.spanBuilder(spanName)
                .setAttribute("component", component)
                .setAttribute("service.name", "serversage")
                .setAttribute("operation.type", extractOperationType(spanName))
                .setSpanKind(SpanKind.INTERNAL)
                .startSpan();
        
        try (var scope = span.makeCurrent()) {
            // Add operation-specific attributes
            span.setAttribute("span.kind", "internal");
            span.setAttribute("operation.name", spanName);
            
            runnable.run();
            span.setStatus(StatusCode.OK);
            span.setAttribute("operation.success", true);
        } catch (Exception e) {
            span.recordException(e);
            span.setStatus(StatusCode.ERROR, e.getMessage());
            span.setAttribute("operation.success", false);
            span.setAttribute("error.type", e.getClass().getSimpleName());
            recordError(e.getClass().getSimpleName(), e.getMessage());
            throw e;
        } finally {
            span.end();
        }
    }
    
    // Create a new root span for HTTP operations
    public <T> T executeInNewTrace(String operationName, String httpMethod, String endpoint, java.util.function.Supplier<T> supplier) {
        // Create a new root span for this HTTP operation
        Span span = tracer.spanBuilder(operationName)
                .setAttribute("http.method", httpMethod)
                .setAttribute("http.route", endpoint)
                .setAttribute("service.name", "serversage")
                .setAttribute("component", "http-handler")
                .setSpanKind(SpanKind.SERVER)
                .startSpan();
        
        try (var scope = span.makeCurrent()) {
            span.setAttribute("operation.type", "http_request");
            span.setAttribute("http.endpoint", endpoint);
            
            T result = supplier.get();
            span.setStatus(StatusCode.OK);
            span.setAttribute("http.status_code", 200);
            return result;
        } catch (Exception e) {
            span.recordException(e);
            span.setStatus(StatusCode.ERROR, e.getMessage());
            span.setAttribute("http.status_code", 500);
            span.setAttribute("error.type", e.getClass().getSimpleName());
            recordError(e.getClass().getSimpleName(), e.getMessage());
            throw e;
        } finally {
            span.end();
        }
    }
    
    private String extractOperationType(String spanName) {
        if (spanName.contains("UserService")) return "user_operation";
        if (spanName.contains("ProductService")) return "product_operation";
        if (spanName.contains("OrderService")) return "order_operation";
        if (spanName.contains("AnalyticsService")) return "analytics_operation";
        return "service_operation";
    }
}
