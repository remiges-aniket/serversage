package tech.remiges.serversage.observability;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.common.Attributes;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * HTTP interceptor for comprehensive request observability
 * Captures all HTTP requests with full OpenTelemetry integration
 * 
 * Features:
 * - Automatic span creation for each request
 * - HTTP metrics with exemplar support
 * - Request/response correlation
 * - Error tracking and correlation
 * - User agent and IP tracking
 */
@Component
public class HttpObservabilityInterceptor implements HandlerInterceptor {

    private final ObservabilityService observabilityService;
    private static final String START_TIME_ATTRIBUTE = "startTime";
    private static final String SPAN_ATTRIBUTE = "otelSpan";

    public HttpObservabilityInterceptor(ObservabilityService observabilityService) {
        this.observabilityService = observabilityService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        long startTime = System.nanoTime();
        request.setAttribute(START_TIME_ATTRIBUTE, startTime);

        // Create span for the request
        String operationName = String.format("%s %s", request.getMethod(), getRoutePath(request));
        Span span = observabilityService.startSpan(operationName, SpanKind.SERVER);
        
        // Add comprehensive HTTP attributes
        span.setAllAttributes(Attributes.builder()
                .put("http.method", request.getMethod())
                .put("http.url", request.getRequestURL().toString())
                .put("http.route", getRoutePath(request))
                .put("http.scheme", request.getScheme())
                .put("http.host", request.getServerName())
                .put("http.target", request.getRequestURI())
                .put("http.user_agent", request.getHeader("User-Agent") != null ? request.getHeader("User-Agent") : "unknown")
                .put("http.client_ip", getClientIpAddress(request))
                .put("component", "http-server")
                .build());

        request.setAttribute(SPAN_ATTRIBUTE, span);
        span.makeCurrent();

        // Log request start
        observabilityService.logInfo("HTTP request started", Attributes.builder()
                .put("http.method", request.getMethod())
                .put("http.url", request.getRequestURL().toString())
                .put("http.user_agent", request.getHeader("User-Agent") != null ? request.getHeader("User-Agent") : "unknown")
                .build());

        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        Long startTime = (Long) request.getAttribute(START_TIME_ATTRIBUTE);
        Span span = (Span) request.getAttribute(SPAN_ATTRIBUTE);

        if (startTime != null && span != null) {
            double durationSeconds = (System.nanoTime() - startTime) / 1_000_000_000.0;
            int statusCode = response.getStatus();

            // Update span with response information
            span.setAllAttributes(Attributes.builder()
                    .put("http.status_code", statusCode)
                    .put("http.response.size", response.getBufferSize())
                    .build());

            // Handle exceptions
            if (ex != null) {
                span.recordException(ex);
                span.setStatus(io.opentelemetry.api.trace.StatusCode.ERROR, ex.getMessage());
                observabilityService.recordError("http_request_exception", "http-server", request.getRequestURI(), ex);
            } else if (statusCode >= 400) {
                span.setStatus(io.opentelemetry.api.trace.StatusCode.ERROR, "HTTP " + statusCode);
            } else {
                span.setStatus(io.opentelemetry.api.trace.StatusCode.OK);
            }

            // Record comprehensive metrics
            observabilityService.recordHttpRequest(
                    request.getMethod(),
                    getRoutePath(request),
                    statusCode,
                    durationSeconds,
                    request.getHeader("User-Agent")
            );

            // Log request completion
            observabilityService.logInfo("HTTP request completed", Attributes.builder()
                    .put("http.method", request.getMethod())
                    .put("http.route", getRoutePath(request))
                    .put("http.status_code", statusCode)
                    .put("duration_seconds", durationSeconds)
                    .put("success", statusCode < 400)
                    .build());

            span.end();
        }
    }

    /**
     * Extracts route path from request URI
     */
    private String getRoutePath(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String contextPath = request.getContextPath();
        
        if (contextPath != null && !contextPath.isEmpty()) {
            uri = uri.substring(contextPath.length());
        }
        
        // Normalize path parameters for better grouping
        return uri.replaceAll("/\\d+", "/{id}")
                  .replaceAll("/[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12}", "/{uuid}");
    }

    /**
     * Extracts client IP address from request
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }
}
