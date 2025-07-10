package tech.remiges.serversage.observability;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.testing.exporter.InMemoryMetricReader;
import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.semconv.ResourceAttributes;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class ObservabilityServiceTest {

    private ObservabilityService observabilityService;
    private InMemorySpanExporter spanExporter;
    private InMemoryMetricReader metricReader;

    @BeforeEach
    void setUp() {
        spanExporter = InMemorySpanExporter.create();
        metricReader = InMemoryMetricReader.create();

        Resource resource = Resource.getDefault()
                .merge(Resource.create(io.opentelemetry.api.common.Attributes.of(
                        ResourceAttributes.SERVICE_NAME, "serversage-test")));

        SdkTracerProvider tracerProvider = SdkTracerProvider.builder()
                .addSpanProcessor(SimpleSpanProcessor.create(spanExporter))
                .setResource(resource)
                .build();

        SdkMeterProvider meterProvider = SdkMeterProvider.builder()
                .registerMetricReader(metricReader)
                .setResource(resource)
                .build();

        OpenTelemetry openTelemetry = OpenTelemetrySdk.builder()
                .setTracerProvider(tracerProvider)
                .setMeterProvider(meterProvider)
                .build();

        observabilityService = new ObservabilityService(openTelemetry);
    }

    @Test
    void testRecordHttpRequest() {
        // Given
        String method = "GET";
        String route = "/api/users";
        int statusCode = 200;
        long durationMs = 150;

        // When
        observabilityService.recordHttpRequest(method, route, statusCode, durationMs);

        // Then
        var metrics = metricReader.collectAllMetrics();
        assertFalse(metrics.isEmpty());
        
        // Verify that metrics were recorded
        assertTrue(metrics.stream().anyMatch(metric -> 
            metric.getName().equals("serversage_http_requests_total")));
        assertTrue(metrics.stream().anyMatch(metric -> 
            metric.getName().equals("serversage_http_request_duration_seconds")));
    }

    @Test
    void testRecordDatabaseOperation() {
        // Given
        String operation = "SELECT";
        String table = "users";
        long durationMs = 50;
        boolean success = true;

        // When
        observabilityService.recordDatabaseOperation(operation, table, durationMs, success);

        // Then
        var metrics = metricReader.collectAllMetrics();
        assertTrue(metrics.stream().anyMatch(metric -> 
            metric.getName().equals("serversage_database_operations_total")));
        assertTrue(metrics.stream().anyMatch(metric -> 
            metric.getName().equals("serversage_database_operation_duration_seconds")));
    }

    @Test
    void testRecordError() {
        // Given
        String errorType = "ValidationException";
        String errorMessage = "Invalid input data";

        // When
        observabilityService.recordError(errorType, errorMessage);

        // Then
        var metrics = metricReader.collectAllMetrics();
        assertTrue(metrics.stream().anyMatch(metric -> 
            metric.getName().equals("serversage_errors_total")));
    }

    @Test
    void testExecuteInSpanSuccess() {
        // Given
        String spanName = "test-operation";
        String component = "test-component";
        String expectedResult = "success";

        // When
        String result = observabilityService.executeInSpan(spanName, component, () -> expectedResult);

        // Then
        assertEquals(expectedResult, result);
        
        var spans = spanExporter.getFinishedSpanItems();
        assertEquals(1, spans.size());
        
        var span = spans.get(0);
        assertEquals(spanName, span.getName());
        assertEquals(StatusCode.OK, span.getStatus().getStatusCode());
        assertEquals(component, span.getAttributes().get(io.opentelemetry.api.common.AttributeKey.stringKey("component")));
    }

    @Test
    void testExecuteInSpanWithException() {
        // Given
        String spanName = "test-operation";
        String component = "test-component";
        RuntimeException expectedException = new RuntimeException("Test exception");

        // When & Then
        assertThrows(RuntimeException.class, () -> 
            observabilityService.executeInSpan(spanName, component, () -> {
                throw expectedException;
            }));
        
        var spans = spanExporter.getFinishedSpanItems();
        assertEquals(1, spans.size());
        
        var span = spans.get(0);
        assertEquals(spanName, span.getName());
        assertEquals(StatusCode.ERROR, span.getStatus().getStatusCode());
        assertEquals("Test exception", span.getStatus().getDescription());
    }

    @Test
    void testExecuteInSpanVoid() {
        // Given
        String spanName = "test-void-operation";
        String component = "test-component";
        final boolean[] executed = {false};

        // When
        observabilityService.executeInSpan(spanName, component, () -> executed[0] = true);

        // Then
        assertTrue(executed[0]);
        
        var spans = spanExporter.getFinishedSpanItems();
        assertEquals(1, spans.size());
        
        var span = spans.get(0);
        assertEquals(spanName, span.getName());
        assertEquals(StatusCode.OK, span.getStatus().getStatusCode());
    }

    @Test
    void testUpdateUserCount() {
        // Given
        long count = 100;

        // When
        observabilityService.updateUserCount(count);

        // Then
        var metrics = metricReader.collectAllMetrics();
        assertTrue(metrics.stream().anyMatch(metric -> 
            metric.getName().equals("serversage_users_total")));
    }

    @Test
    void testUpdateProductCount() {
        // Given
        long count = 50;

        // When
        observabilityService.updateProductCount(count);

        // Then
        var metrics = metricReader.collectAllMetrics();
        assertTrue(metrics.stream().anyMatch(metric -> 
            metric.getName().equals("serversage_products_total")));
    }

    @Test
    void testUpdateOrderCount() {
        // Given
        long count = 25;

        // When
        observabilityService.updateOrderCount(count);

        // Then
        var metrics = metricReader.collectAllMetrics();
        assertTrue(metrics.stream().anyMatch(metric -> 
            metric.getName().equals("serversage_orders_total")));
    }

    @Test
    void testStartSpan() {
        // Given
        String spanName = "custom-span";

        // When
        Span span = observabilityService.startSpan(spanName);

        // Then
        assertNotNull(span);
        assertTrue(span.isRecording());
        
        // Clean up
        span.end();
    }

    @Test
    void testAddSpanAttributes() {
        // Given
        Span span = observabilityService.startSpan("test-span");
        
        try (var scope = span.makeCurrent()) {
            // When
            observabilityService.addSpanAttribute("test.key", "test.value");
            observabilityService.addSpanAttribute("test.number", 42L);
            observabilityService.addUserContextToSpan("user123", "ADMIN");
        } finally {
            span.end();
        }

        // Then
        var spans = spanExporter.getFinishedSpanItems();
        assertEquals(1, spans.size());
        
        var finishedSpan = spans.get(0);
        assertEquals("test.value", finishedSpan.getAttributes().get(io.opentelemetry.api.common.AttributeKey.stringKey("test.key")));
        assertEquals(42L, finishedSpan.getAttributes().get(io.opentelemetry.api.common.AttributeKey.longKey("test.number")));
        assertEquals("user123", finishedSpan.getAttributes().get(io.opentelemetry.api.common.AttributeKey.stringKey("user.id")));
        assertEquals("ADMIN", finishedSpan.getAttributes().get(io.opentelemetry.api.common.AttributeKey.stringKey("user.role")));
    }

    @Test
    void testLoggingMethods() {
        // These tests verify that logging methods don't throw exceptions
        // Actual log verification would require more complex setup
        
        assertDoesNotThrow(() -> observabilityService.logInfo("Test info message"));
        assertDoesNotThrow(() -> observabilityService.logError("Test error message"));
        assertDoesNotThrow(() -> observabilityService.logWarn("Test warn message"));
        assertDoesNotThrow(() -> observabilityService.logDebug("Test debug message"));
    }
}
