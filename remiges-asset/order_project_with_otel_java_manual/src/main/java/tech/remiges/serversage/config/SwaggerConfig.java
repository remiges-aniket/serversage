package tech.remiges.serversage.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("ServerSage - Comprehensive OpenTelemetry Observability Showcase")
                        .description("""
                                This API demonstrates comprehensive observability with OpenTelemetry including:
                                
                                🔍 **Observability Features:**
                                - Distributed tracing with spans and trace correlation
                                - Custom metrics with exemplars linking to traces
                                - Structured logging with trace context
                                - Error tracking and correlation
                                
                                ⚠️ **Error Scenarios for Testing:**
                                - Database connection errors (use 'dberror' in names)
                                - Array index out of bounds (Product ID 999)
                                - Null pointer exceptions (Product ID 998)
                                - Rate limiting (Order ID 997)
                                - Business logic errors (prices > $10,000)
                                - External service failures (random 10-20% failure rates)
                                - Timeout scenarios (category 'timeout')
                                - Validation errors (empty/invalid data)
                                
                                📊 **Dashboard Integration:**
                                - Business metrics for non-technical users
                                - Technical metrics for developers
                                - JVM monitoring and performance metrics
                                - Error correlation and root cause analysis
                                
                                🚀 **20+ APIs Available:**
                                - User Management (CRUD + search, batch operations)
                                - Product Management (CRUD + inventory, recommendations)
                                - Order Management (CRUD + payment, status tracking)
                                - Analytics & Reporting (dashboard, statistics, health checks)
                                """)
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("ServerSage Team")
                                .email("support@serversage.com")
                                .url("https://github.com/serversage/observability-demo"))
                        .license(new License()
                                .name("MIT License")
                                .url("https://opensource.org/licenses/MIT")))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:8081")
                                .description("Local Development Server"),
                        new Server()
                                .url("https://api.serversage.com")
                                .description("Production Server")));
    }
}
