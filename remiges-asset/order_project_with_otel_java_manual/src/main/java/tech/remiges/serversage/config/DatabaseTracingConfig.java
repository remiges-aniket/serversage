package tech.remiges.serversage.config;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.semconv.SemanticAttributes;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceUtils;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.logging.Logger;

/**
 * Configuration for enhanced database tracing with OpenTelemetry
 * Captures database queries, connection details, IP addresses, and failures
 */
@Configuration
public class DatabaseTracingConfig {

    private static final Logger logger = Logger.getLogger(DatabaseTracingConfig.class.getName());
    
    @Autowired
    private OpenTelemetry openTelemetry;
    
    @Autowired
    private DataSource dataSource;
    
    private Tracer tracer;
    
    @Bean
    public DatabaseTracer databaseTracer() {
        this.tracer = openTelemetry.getTracer("serversage-database", "1.0.0");
        return new DatabaseTracer();
    }
    
    /**
     * Custom database tracer to capture detailed database information
     */
    public class DatabaseTracer {
        
        /**
         * Create a database span with detailed connection information
         */
        public Span createDatabaseSpan(String operation, String query) {
            Span span = tracer.spanBuilder("db." + operation)
                    .setSpanKind(io.opentelemetry.api.trace.SpanKind.CLIENT)
                    .startSpan();
            
            try {
                // Get database connection information
                Connection connection = DataSourceUtils.getConnection(dataSource);
                DatabaseMetaData metaData = connection.getMetaData();
                
                // Add database attributes
                span.setAllAttributes(io.opentelemetry.api.common.Attributes.builder()
                        .put(SemanticAttributes.DB_SYSTEM, "postgresql")
                        .put(SemanticAttributes.DB_NAME, getDatabaseName(connection))
                        .put(SemanticAttributes.DB_USER, metaData.getUserName())
                        .put(SemanticAttributes.DB_CONNECTION_STRING, sanitizeConnectionString(metaData.getURL()))
                        .put("db.host", extractHost(metaData.getURL()))
                        .put("db.port", extractPort(metaData.getURL()))
                        .put("db.server.address", extractHost(metaData.getURL()))
                        .put("db.server.port", extractPort(metaData.getURL()))
                        .put("db.driver.name", metaData.getDriverName())
                        .put("db.driver.version", metaData.getDriverVersion())
                        .put("db.product.name", metaData.getDatabaseProductName())
                        .put("db.product.version", metaData.getDatabaseProductVersion())
                        .build());
                
                // Add query if provided and not sanitized
                if (query != null && !query.trim().isEmpty()) {
                    span.setAttribute(SemanticAttributes.DB_STATEMENT, query);
                    span.setAttribute("db.query.type", extractQueryType(query));
                    span.setAttribute("db.query.length", query.length());
                }
                
                // Add connection pool information if available
                addConnectionPoolInfo(span, connection);
                
                DataSourceUtils.releaseConnection(connection, dataSource);
                
            } catch (SQLException e) {
                // Record database connection errors
                span.recordException(e);
                span.setStatus(io.opentelemetry.api.trace.StatusCode.ERROR, "Database connection failed: " + e.getMessage());
                
                logger.severe("Database connection failed: " + e.getMessage());
                
                // Add error attributes
                span.setAllAttributes(io.opentelemetry.api.common.Attributes.builder()
                        .put("error", true)
                        .put("error.type", e.getClass().getSimpleName())
                        .put("error.message", e.getMessage())
                        .put("db.error.code", String.valueOf(e.getErrorCode()))
                        .put("db.error.state", e.getSQLState() != null ? e.getSQLState() : "unknown")
                        .build());
            }
            
            return span;
        }
        
        /**
         * Record database query execution with timing and results
         */
        public void recordQueryExecution(Span span, String query, long startTime, long endTime, 
                                       int rowCount, Exception error) {
            long duration = endTime - startTime;
            
            span.setAllAttributes(io.opentelemetry.api.common.Attributes.builder()
                    .put("db.query.duration_ms", duration)
                    .put("db.query.start_time", startTime)
                    .put("db.query.end_time", endTime)
                    .put("db.query.row_count", rowCount)
                    .build());
            
            if (error != null) {
                span.recordException(error);
                span.setStatus(io.opentelemetry.api.trace.StatusCode.ERROR, "Query execution failed: " + error.getMessage());
                
                span.setAllAttributes(io.opentelemetry.api.common.Attributes.builder()
                        .put("error", true)
                        .put("error.type", error.getClass().getSimpleName())
                        .put("error.message", error.getMessage())
                        .build());
                
                if (error instanceof SQLException) {
                    SQLException sqlError = (SQLException) error;
                    span.setAllAttributes(io.opentelemetry.api.common.Attributes.builder()
                            .put("db.error.code", String.valueOf(sqlError.getErrorCode()))
                            .put("db.error.state", sqlError.getSQLState() != null ? sqlError.getSQLState() : "unknown")
                            .build());
                }
                
                logger.severe("Database query failed: " + error.getMessage() + " | Query: " + query);
            } else {
                span.setStatus(io.opentelemetry.api.trace.StatusCode.OK);
                logger.info("Database query executed successfully in " + duration + "ms, returned " + rowCount + " rows");
            }
        }
        
        private String getDatabaseName(Connection connection) throws SQLException {
            String url = connection.getMetaData().getURL();
            // Extract database name from PostgreSQL URL: jdbc:postgresql://host:port/dbname
            if (url.contains("/") && url.lastIndexOf("/") < url.length() - 1) {
                String dbPart = url.substring(url.lastIndexOf("/") + 1);
                // Remove query parameters if present
                if (dbPart.contains("?")) {
                    dbPart = dbPart.substring(0, dbPart.indexOf("?"));
                }
                return dbPart;
            }
            return "unknown";
        }
        
        private String sanitizeConnectionString(String url) {
            // Remove sensitive information but keep host and port
            if (url.contains("?")) {
                return url.substring(0, url.indexOf("?"));
            }
            return url;
        }
        
        private String extractHost(String url) {
            try {
                // Extract host from jdbc:postgresql://host:port/db
                if (url.contains("://")) {
                    String hostPart = url.substring(url.indexOf("://") + 3);
                    if (hostPart.contains(":")) {
                        return hostPart.substring(0, hostPart.indexOf(":"));
                    } else if (hostPart.contains("/")) {
                        return hostPart.substring(0, hostPart.indexOf("/"));
                    }
                    return hostPart;
                }
            } catch (Exception e) {
                logger.warning("Failed to extract host from URL: " + url);
            }
            return "localhost";
        }
        
        private long extractPort(String url) {
            try {
                // Extract port from jdbc:postgresql://host:port/db
                if (url.contains("://")) {
                    String hostPart = url.substring(url.indexOf("://") + 3);
                    if (hostPart.contains(":") && hostPart.contains("/")) {
                        String portPart = hostPart.substring(hostPart.indexOf(":") + 1, hostPart.indexOf("/"));
                        return Long.parseLong(portPart);
                    }
                }
            } catch (Exception e) {
                logger.warning("Failed to extract port from URL: " + url);
            }
            return 5432; // Default PostgreSQL port
        }
        
        private String extractQueryType(String query) {
            if (query == null || query.trim().isEmpty()) {
                return "unknown";
            }
            
            String trimmed = query.trim().toUpperCase();
            if (trimmed.startsWith("SELECT")) return "SELECT";
            if (trimmed.startsWith("INSERT")) return "INSERT";
            if (trimmed.startsWith("UPDATE")) return "UPDATE";
            if (trimmed.startsWith("DELETE")) return "DELETE";
            if (trimmed.startsWith("CREATE")) return "CREATE";
            if (trimmed.startsWith("DROP")) return "DROP";
            if (trimmed.startsWith("ALTER")) return "ALTER";
            
            return "other";
        }
        
        private void addConnectionPoolInfo(Span span, Connection connection) {
            try {
                // Add HikariCP connection pool information if available
                if (connection.getClass().getName().contains("HikariProxyConnection")) {
                    span.setAttribute("db.connection.pool.type", "HikariCP");
                    // Additional pool metrics could be added here if accessible
                }
                
                span.setAllAttributes(io.opentelemetry.api.common.Attributes.builder()
                        .put("db.connection.class", connection.getClass().getSimpleName())
                        .put("db.connection.auto_commit", connection.getAutoCommit())
                        .put("db.connection.read_only", connection.isReadOnly())
                        .put("db.connection.transaction_isolation", connection.getTransactionIsolation())
                        .build());
                
            } catch (SQLException e) {
                logger.warning("Failed to get connection pool information: " + e.getMessage());
            }
        }
    }
}
