package tech.remiges.serversage.config;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.context.Scope;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import javax.sql.DataSource;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Custom JDBC Template wrapper that adds detailed OpenTelemetry tracing
 * to all database operations including query details, timing, and errors
 */
public class TracedJdbcTemplate extends JdbcTemplate {

    private static final Logger logger = Logger.getLogger(TracedJdbcTemplate.class.getName());
    
    private DatabaseTracingConfig.DatabaseTracer databaseTracer;
    
    public TracedJdbcTemplate(DataSource dataSource, DatabaseTracingConfig.DatabaseTracer databaseTracer) {
        super(dataSource);
        this.databaseTracer = databaseTracer;
    }
    
    @Override
    public <T> List<T> query(String sql, RowMapper<T> rowMapper) throws DataAccessException {
        return executeWithTracing("query", sql, () -> super.query(sql, rowMapper));
    }
    
    @Override
    public <T> List<T> query(String sql, Object[] args, RowMapper<T> rowMapper) throws DataAccessException {
        String fullSql = buildSqlWithParameters(sql, args);
        return executeWithTracing("query", fullSql, () -> super.query(sql, args, rowMapper));
    }
    
    @Override
    public List<Map<String, Object>> queryForList(String sql) throws DataAccessException {
        return executeWithTracing("query", sql, () -> super.queryForList(sql));
    }
    
    @Override
    public List<Map<String, Object>> queryForList(String sql, Object... args) throws DataAccessException {
        String fullSql = buildSqlWithParameters(sql, args);
        return executeWithTracing("query", fullSql, () -> super.queryForList(sql, args));
    }
    
    @Override
    public <T> T queryForObject(String sql, Class<T> requiredType) throws DataAccessException {
        return executeWithTracing("query", sql, () -> super.queryForObject(sql, requiredType));
    }
    
    @Override
    public <T> T queryForObject(String sql, Object[] args, Class<T> requiredType) throws DataAccessException {
        String fullSql = buildSqlWithParameters(sql, args);
        return executeWithTracing("query", fullSql, () -> super.queryForObject(sql, args, requiredType));
    }
    
    @Override
    public int update(String sql) throws DataAccessException {
        return executeWithTracing("update", sql, () -> super.update(sql));
    }
    
    @Override
    public int update(String sql, Object... args) throws DataAccessException {
        String fullSql = buildSqlWithParameters(sql, args);
        return executeWithTracing("update", fullSql, () -> super.update(sql, args));
    }
    
    @Override
    public int[] batchUpdate(String sql, List<Object[]> batchArgs) throws DataAccessException {
        String batchSql = sql + " [BATCH: " + batchArgs.size() + " operations]";
        return executeWithTracing("batch_update", batchSql, () -> super.batchUpdate(sql, batchArgs));
    }
    
    /**
     * Execute database operation with comprehensive tracing
     */
    private <T> T executeWithTracing(String operation, String sql, DatabaseOperation<T> dbOperation) {
        if (databaseTracer == null) {
            // Fallback to normal execution if tracer not available
            return dbOperation.execute();
        }
        
        Span span = databaseTracer.createDatabaseSpan(operation, sql);
        long startTime = System.currentTimeMillis();
        
        try (Scope scope = span.makeCurrent()) {
            // Add operation-specific attributes
            span.setAttribute("db.operation.type", operation);
            span.setAttribute("db.query.sanitized", sanitizeQuery(sql));
            span.setAttribute("db.query.length", sql.length());
            
            logger.info("Executing " + operation + ": " + sanitizeQuery(sql));
            
            // Execute the database operation
            T result = dbOperation.execute();
            
            long endTime = System.currentTimeMillis();
            int rowCount = getRowCount(result);
            
            // Record successful execution
            databaseTracer.recordQueryExecution(span, sql, startTime, endTime, rowCount, null);
            
            // Add result-specific attributes
            span.setAttribute("db.result.type", result != null ? result.getClass().getSimpleName() : "void");
            if (result instanceof List) {
                span.setAttribute("db.result.list.size", ((List<?>) result).size());
            }
            
            return result;
            
        } catch (Exception e) {
            long endTime = System.currentTimeMillis();
            
            // Record failed execution
            databaseTracer.recordQueryExecution(span, sql, startTime, endTime, 0, e);
            
            // Add error context
            span.setAttribute("db.error.query", sql);
            span.setAttribute("db.error.operation", operation);
            
            logger.severe("Database operation failed - Operation: " + operation + 
                         ", Query: " + sanitizeQuery(sql) + 
                         ", Error: " + e.getMessage());
            
            throw e;
        } finally {
            span.end();
        }
    }
    
    /**
     * Build SQL string with parameter values for better tracing
     */
    private String buildSqlWithParameters(String sql, Object[] args) {
        if (args == null || args.length == 0) {
            return sql;
        }
        
        StringBuilder result = new StringBuilder(sql);
        result.append(" [PARAMS: ");
        for (int i = 0; i < args.length; i++) {
            if (i > 0) result.append(", ");
            result.append(args[i] != null ? args[i].toString() : "null");
        }
        result.append("]");
        
        return result.toString();
    }
    
    /**
     * Sanitize query for logging (remove sensitive data)
     */
    private String sanitizeQuery(String sql) {
        if (sql == null) return "null";
        
        // Remove potential sensitive data patterns
        String sanitized = sql.replaceAll("(?i)(password|token|secret|key)\\s*=\\s*'[^']*'", "$1='***'");
        sanitized = sanitized.replaceAll("(?i)(password|token|secret|key)\\s*=\\s*\"[^\"]*\"", "$1=\"***\"");
        
        // Limit length for logging
        if (sanitized.length() > 500) {
            sanitized = sanitized.substring(0, 500) + "... [TRUNCATED]";
        }
        
        return sanitized;
    }
    
    /**
     * Extract row count from result for metrics
     */
    private int getRowCount(Object result) {
        if (result == null) return 0;
        if (result instanceof List) return ((List<?>) result).size();
        if (result instanceof Integer) return (Integer) result;
        if (result instanceof int[]) return ((int[]) result).length;
        return 1; // Single object result
    }
    
    /**
     * Functional interface for database operations
     */
    @FunctionalInterface
    private interface DatabaseOperation<T> {
        T execute() throws DataAccessException;
    }
}
