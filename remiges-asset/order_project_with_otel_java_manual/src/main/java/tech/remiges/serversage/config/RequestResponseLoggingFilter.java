package tech.remiges.serversage.config;

import io.opentelemetry.api.trace.Span;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
@Order(1)
public class RequestResponseLoggingFilter implements Filter {

    private static final Logger logger = LoggerFactory.getLogger(RequestResponseLoggingFilter.class);

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        if (request instanceof HttpServletRequest && response instanceof HttpServletResponse) {
            HttpServletRequest httpRequest = (HttpServletRequest) request;
            HttpServletResponse httpResponse = (HttpServletResponse) response;

            // Skip non-API requests
            if (!httpRequest.getRequestURI().startsWith("/api/")) {
                chain.doFilter(request, response);
                return;
            }

            // Wrap request and response to capture content
            ContentCachingRequestWrapper wrappedRequest = new ContentCachingRequestWrapper(httpRequest);
            ContentCachingResponseWrapper wrappedResponse = new ContentCachingResponseWrapper(httpResponse);

            try {
                // Log request details
                logRequestDetails(wrappedRequest);

                // Continue with the filter chain
                chain.doFilter(wrappedRequest, wrappedResponse);

                // Log response details
                logResponseDetails(wrappedResponse);

                // Copy response content back to original response
                wrappedResponse.copyBodyToResponse();

            } catch (Exception e) {
                logger.error("Error in request/response logging filter: {}", e.getMessage());
                throw e;
            }
        } else {
            chain.doFilter(request, response);
        }
    }

    private void logRequestDetails(ContentCachingRequestWrapper request) {
        try {
            Span currentSpan = Span.current();
            
            // Log request method and URL
            logger.info("🚀 HTTP Request: {} {} | Content-Type: {}", 
                    request.getMethod(), request.getRequestURI(), request.getContentType());

            // Log query parameters
            if (request.getQueryString() != null) {
                logger.info("🔍 Query Parameters: {}", request.getQueryString());
                currentSpan.setAttribute("http.query_string", request.getQueryString());
            }

            // Log request body for POST/PUT/PATCH requests
            if ("POST".equals(request.getMethod()) || "PUT".equals(request.getMethod()) || "PATCH".equals(request.getMethod())) {
                byte[] content = request.getContentAsByteArray();
                if (content.length > 0) {
                    String requestBody = new String(content, StandardCharsets.UTF_8);
                    // Limit body size to prevent huge logs
                    if (requestBody.length() > 1000) {
                        requestBody = requestBody.substring(0, 1000) + "... (truncated)";
                    }
                    logger.info("📝 Request Body: {}", requestBody);
                    currentSpan.setAttribute("http.request.body", requestBody);
                }
            }

            // Add request attributes to span
            currentSpan.setAttribute("http.method", request.getMethod());
            currentSpan.setAttribute("http.url", request.getRequestURL().toString());
            currentSpan.setAttribute("http.user_agent", request.getHeader("User-Agent"));
            if (request.getContentType() != null) {
                currentSpan.setAttribute("http.request.content_type", request.getContentType());
            }

        } catch (Exception e) {
            logger.warn("Failed to log request details: {}", e.getMessage());
        }
    }

    private void logResponseDetails(ContentCachingResponseWrapper response) {
        try {
            Span currentSpan = Span.current();
            
            logger.info("📤 HTTP Response: Status {} | Content-Type: {}", 
                    response.getStatus(), response.getContentType());

            // Log response body for successful requests (limited size)
            if (response.getStatus() < 400) {
                byte[] content = response.getContentAsByteArray();
                if (content.length > 0) {
                    String responseBody = new String(content, StandardCharsets.UTF_8);
                    // Limit response body size
                    if (responseBody.length() > 500) {
                        responseBody = responseBody.substring(0, 500) + "... (truncated)";
                    }
                    logger.info("📋 Response Body: {}", responseBody);
                    currentSpan.setAttribute("http.response.body", responseBody);
                }
            }

            // Add response attributes to span
            currentSpan.setAttribute("http.status_code", response.getStatus());
            if (response.getContentType() != null) {
                currentSpan.setAttribute("http.response.content_type", response.getContentType());
            }

        } catch (Exception e) {
            logger.warn("Failed to log response details: {}", e.getMessage());
        }
    }
}
