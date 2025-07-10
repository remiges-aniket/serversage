package tech.remiges.serversage.config;

import tech.remiges.serversage.observability.HttpObservabilityInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web configuration for registering observability interceptors
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final HttpObservabilityInterceptor httpObservabilityInterceptor;
    private final TraceIsolationInterceptor traceIsolationInterceptor;

    public WebConfig(HttpObservabilityInterceptor httpObservabilityInterceptor,
                    TraceIsolationInterceptor traceIsolationInterceptor) {
        this.httpObservabilityInterceptor = httpObservabilityInterceptor;
        this.traceIsolationInterceptor = traceIsolationInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // Add trace isolation interceptor first (highest priority)
        registry.addInterceptor(traceIsolationInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns("/actuator/**", "/swagger-ui/**", "/api-docs/**")
                .order(1);
                
        // Add observability interceptor second
        registry.addInterceptor(httpObservabilityInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns("/actuator/**", "/swagger-ui/**", "/api-docs/**")
                .order(2);
    }
}
