package tech.remiges.serversage.config;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;
import tech.remiges.serversage.observability.ObservabilityService;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.stream.Collectors;

@Component
public class TraceIsolationInterceptor implements HandlerInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(TraceIsolationInterceptor.class);
    private final Tracer tracer;
    private final ObservabilityService observabilityService;

    @Autowired
    public TraceIsolationInterceptor(ObservabilityService observabilityService) {
        this.observabilityService = observabilityService;
        this.tracer = observabilityService.getTracer();
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // Wrap request and response to capture content
        ContentCachingRequestWrapper wrappedRequest = new ContentCachingRequestWrapper(request);
        ContentCachingResponseWrapper wrappedResponse = new ContentCachingResponseWrapper(response);
        
        // Create a new root span for each HTTP request to ensure isolation
        String operationName = request.getMethod() + " " + getCleanPath(request.getRequestURI());
        
        // Start a new root span (not a child span)
        Span span = tracer.spanBuilder(operationName)
                .setSpanKind(SpanKind.SERVER)
                .setAttribute("http.method", request.getMethod())
                .setAttribute("http.url", request.getRequestURL().toString())
                .setAttribute("http.route", getCleanPath(request.getRequestURI()))
                .setAttribute("http.scheme", request.getScheme())
                .setAttribute("http.target", request.getRequestURI())
                .setAttribute("service.name", "serversage")
                .setAttribute("component", "http-server")
                .setParent(Context.root()) // Ensure this is a root span
                .startSpan();

        // Capture request details
        captureRequestDetails(span, wrappedRequest);

        // Make this span current for the request
        Scope scope = span.makeCurrent();
        
        // Store span and scope in request attributes for cleanup
        request.setAttribute("trace.span", span);
        request.setAttribute("trace.scope", scope);
        request.setAttribute("wrapped.request", wrappedRequest);
        request.setAttribute("wrapped.response", wrappedResponse);
        
        // Add trace context to MDC for logging
        String traceId = span.getSpanContext().getTraceId();
        String spanId = span.getSpanContext().getSpanId();
        MDC.put("traceId", traceId);
        MDC.put("spanId", spanId);
        
        logger.debug("🚀 Started new trace for {} {}: traceId={}", 
                request.getMethod(), request.getRequestURI(), traceId);
        
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, 
                               Object handler, Exception ex) throws Exception {
        
        Span span = (Span) request.getAttribute("trace.span");
        Scope scope = (Scope) request.getAttribute("trace.scope");
        ContentCachingRequestWrapper wrappedRequest = (ContentCachingRequestWrapper) request.getAttribute("wrapped.request");
        ContentCachingResponseWrapper wrappedResponse = (ContentCachingResponseWrapper) request.getAttribute("wrapped.response");
        
        if (span != null) {
            try {
                // Capture response details
                captureResponseDetails(span, wrappedResponse != null ? wrappedResponse : response);
                
                // Set final span attributes
                span.setAttribute("http.status_code", response.getStatus());
                span.setAttribute("http.response.size", response.getBufferSize());
                
                if (ex != null) {
                    span.recordException(ex);
                    span.setStatus(StatusCode.ERROR, ex.getMessage());
                    span.setAttribute("error", true);
                    span.setAttribute("error.type", ex.getClass().getSimpleName());
                    span.setAttribute("error.message", ex.getMessage());
                } else if (response.getStatus() >= 400) {
                    span.setStatus(StatusCode.ERROR, "HTTP " + response.getStatus());
                    span.setAttribute("error", true);
                } else {
                    span.setStatus(StatusCode.OK);
                    span.setAttribute("error", false);
                }
                
                logger.debug("✅ Completed trace for {} {}: status={}, traceId={}", 
                        request.getMethod(), request.getRequestURI(), 
                        response.getStatus(), span.getSpanContext().getTraceId());
                
            } finally {
                // Clean up
                if (scope != null) {
                    scope.close();
                }
                span.end();
                
                // Clear MDC
                MDC.remove("traceId");
                MDC.remove("spanId");
            }
        }
    }
    
    private void captureRequestDetails(Span span, HttpServletRequest request) {
        try {
            // Capture query parameters
            if (request.getQueryString() != null) {
                span.setAttribute("http.query_string", request.getQueryString());
            }
            
            // Capture headers (selective)
            span.setAttribute("http.user_agent", request.getHeader("User-Agent"));
            span.setAttribute("http.content_type", request.getContentType());
            span.setAttribute("http.content_length", request.getContentLength());
            
            // Capture request parameters
            String params = request.getParameterMap().entrySet().stream()
                    .map(entry -> entry.getKey() + "=" + String.join(",", entry.getValue()))
                    .collect(Collectors.joining("&"));
            if (!params.isEmpty()) {
                span.setAttribute("http.request.params", params);
            }
            
            // Capture request body for POST/PUT requests
            if ("POST".equals(request.getMethod()) || "PUT".equals(request.getMethod()) || "PATCH".equals(request.getMethod())) {
                if (request instanceof ContentCachingRequestWrapper) {
                    ContentCachingRequestWrapper wrapper = (ContentCachingRequestWrapper) request;
                    byte[] content = wrapper.getContentAsByteArray();
                    if (content.length > 0) {
                        String requestBody = new String(content, StandardCharsets.UTF_8);
                        // Limit body size to prevent huge spans
                        if (requestBody.length() > 1000) {
                            requestBody = requestBody.substring(0, 1000) + "... (truncated)";
                        }
                        span.setAttribute("http.request.body", requestBody);
                        logger.info("📝 Request Body: {}", requestBody);
                    }
                }
            }
            
        } catch (Exception e) {
            logger.warn("Failed to capture request details: {}", e.getMessage());
        }
    }
    
    private void captureResponseDetails(Span span, HttpServletResponse response) {
        try {
            // Capture response headers
            span.setAttribute("http.response.content_type", response.getContentType());
            
            // Capture response body for successful requests (limited)
            if (response instanceof ContentCachingResponseWrapper && response.getStatus() < 400) {
                ContentCachingResponseWrapper wrapper = (ContentCachingResponseWrapper) response;
                byte[] content = wrapper.getContentAsByteArray();
                if (content.length > 0) {
                    String responseBody = new String(content, StandardCharsets.UTF_8);
                    // Limit response body size
                    if (responseBody.length() > 500) {
                        responseBody = responseBody.substring(0, 500) + "... (truncated)";
                    }
                    span.setAttribute("http.response.body", responseBody);
                    logger.info("📤 Response Body: {}", responseBody);
                }
            }
            
        } catch (Exception e) {
            logger.warn("Failed to capture response details: {}", e.getMessage());
        }
    }
    
    private String getCleanPath(String uri) {
        // Clean up the path for better span naming
        if (uri == null) return "/";
        
        // Remove query parameters
        int queryIndex = uri.indexOf('?');
        if (queryIndex > 0) {
            uri = uri.substring(0, queryIndex);
        }
        
        // Replace IDs with placeholders for better grouping
        uri = uri.replaceAll("/\\d+", "/{id}");
        
        return uri;
    }
}
