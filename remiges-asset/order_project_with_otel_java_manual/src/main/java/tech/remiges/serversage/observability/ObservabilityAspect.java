package tech.remiges.serversage.observability;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Span;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

/**
 * AOP aspect for automatic observability instrumentation
 * Following SOLID principles:
 * - Single Responsibility: Handles cross-cutting observability concerns
 * - Open/Closed: Extensible for new pointcuts and instrumentation
 * - Dependency Inversion: Depends on ObservabilityService abstraction
 */
@Aspect
@Component
public class ObservabilityAspect {

    private final ObservabilityService observabilityService;

    public ObservabilityAspect(ObservabilityService observabilityService) {
        this.observabilityService = observabilityService;
    }

    /**
     * Automatically instruments all service methods with tracing and metrics
     * Creates spans with exemplars for metrics correlation
     */
    @Around("execution(* tech.remiges.serversage.service.*.*(..))")
    public Object instrumentServiceMethods(ProceedingJoinPoint joinPoint) throws Throwable {
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = joinPoint.getSignature().getName();
        String spanName = className + "." + methodName;

        Span span = observabilityService.startSpan(spanName);
        span.setAllAttributes(Attributes.builder()
                .put("component", "service")
                .put("operation", methodName)
                .build());

        long startTime = System.nanoTime();
        try (var scope = span.makeCurrent()) {
            Object result = joinPoint.proceed();
            
            // Record successful operation
            double durationSeconds = (System.nanoTime() - startTime) / 1_000_000_000.0;
            observabilityService.recordDatabaseOperation(methodName, className, durationSeconds, true, null);
            
            // Log successful operation
            observabilityService.logInfo("Service operation completed successfully",
                Attributes.builder()
                    .put("service.class", className)
                    .put("service.method", methodName)
                    .put("duration_seconds", durationSeconds)
                    .build());
            
            return result;
        } catch (Exception e) {
            // Record error metrics and logs
            span.recordException(e);
            span.setStatus(io.opentelemetry.api.trace.StatusCode.ERROR, e.getMessage());
            
            observabilityService.recordError(e.getClass().getSimpleName(), className, methodName, e);
            observabilityService.logError("Service operation failed",
                e,
                Attributes.builder()
                    .put("service.class", className)
                    .put("service.method", methodName)
                    .build());
            
            throw e;
        } finally {
            span.end();
        }
    }

    /**
     * Automatically instruments all controller methods with HTTP metrics
     * Creates exemplars linking HTTP metrics to traces
     */
    @Around("execution(* tech.remiges.serversage.controller.*.*(..))")
    public Object instrumentControllerMethods(ProceedingJoinPoint joinPoint) throws Throwable {
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = joinPoint.getSignature().getName();
        String spanName = "HTTP " + methodName;

        Span span = observabilityService.startSpan(spanName);
        span.setAllAttributes(Attributes.builder()
                .put("component", "controller")
                .put("operation", methodName)
                .build());

        long startTime = System.nanoTime();
        int statusCode = 200; // Default success
        
        try (var scope = span.makeCurrent()) {
            Object result = joinPoint.proceed();
            return result;
        } catch (Exception e) {
            statusCode = 500; // Error status
            span.recordException(e);
            span.setStatus(io.opentelemetry.api.trace.StatusCode.ERROR, e.getMessage());
            
            observabilityService.recordError(e.getClass().getSimpleName(), className, methodName, e);
            throw e;
        } finally {
            double durationSeconds = (System.nanoTime() - startTime) / 1_000_000_000.0;
            
            // Record HTTP request metrics with exemplars
            observabilityService.recordHttpRequest("HTTP", className + "." + methodName, statusCode, durationSeconds, null);
            
            span.end();
        }
    }

    /**
     * Instruments repository methods for database operation tracking
     */
    @Around("execution(* tech.remiges.serversage.repository.*.*(..))")
    public Object instrumentRepositoryMethods(ProceedingJoinPoint joinPoint) throws Throwable {
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = joinPoint.getSignature().getName();
        String spanName = "DB " + methodName;

        Span span = observabilityService.startSpan(spanName);
        span.setAllAttributes(Attributes.builder()
                .put("component", "repository")
                .put("operation", methodName)
                .build());

        long startTime = System.nanoTime();
        try (var scope = span.makeCurrent()) {
            Object result = joinPoint.proceed();
            
            double durationSeconds = (System.nanoTime() - startTime) / 1_000_000_000.0;
            observabilityService.recordDatabaseOperation(methodName, className, durationSeconds, true, null);
            
            return result;
        } catch (Exception e) {
            span.recordException(e);
            span.setStatus(io.opentelemetry.api.trace.StatusCode.ERROR, e.getMessage());
            
            observabilityService.recordError(e.getClass().getSimpleName(), "database", methodName, e);
            throw e;
        } finally {
            span.end();
        }
    }
}