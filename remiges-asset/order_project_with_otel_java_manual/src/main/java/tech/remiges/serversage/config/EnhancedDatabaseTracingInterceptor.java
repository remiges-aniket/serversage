package tech.remiges.serversage.config;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.context.Scope;
import org.hibernate.Interceptor;
import org.hibernate.type.Type;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import tech.remiges.serversage.observability.ObservabilityService;

import java.io.Serializable;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Enhanced Hibernate interceptor to capture detailed SQL queries and parameters in traces
 */
@Component
public class EnhancedDatabaseTracingInterceptor implements Interceptor {

    private static final Logger logger = LoggerFactory.getLogger(EnhancedDatabaseTracingInterceptor.class);
    
    @Autowired
    private ObservabilityService observabilityService;
    
    @Override
    public boolean onLoad(Object entity, Serializable id, Object[] state, String[] propertyNames, Type[] types) {
        String entityName = entity.getClass().getSimpleName();
        String sqlQuery = "SELECT * FROM " + entityName.toLowerCase() + " WHERE id = ?";
        
        Span span = observabilityService.getTracer().spanBuilder("DB Query: " + entityName + " Load")
                .setSpanKind(SpanKind.CLIENT)
                .setAttribute("db.system", "postgresql")
                .setAttribute("db.operation.name", "SELECT")
                .setAttribute("db.sql.table", entityName.toLowerCase())
                .setAttribute("db.statement", sqlQuery)
                .setAttribute("db.entity.class", entityName)
                .setAttribute("db.entity.id", String.valueOf(id))
                .setAttribute("db.operation.type", "load")
                .startSpan();
        
        try (Scope scope = span.makeCurrent()) {
            logger.info("🔍 SQL Query: {} | Parameters: [id={}] | Entity: {}", 
                    sqlQuery, id, entityName);
            
            span.setAttribute("db.query.success", true);
            span.setStatus(StatusCode.OK);
            
            return false; // Continue with normal processing
        } catch (Exception e) {
            span.recordException(e);
            span.setStatus(StatusCode.ERROR, "Entity load failed: " + e.getMessage());
            span.setAttribute("db.query.success", false);
            logger.error("❌ SQL Query failed: {} | Error: {}", sqlQuery, e.getMessage());
            throw e;
        } finally {
            span.end();
        }
    }
    
    @Override
    public boolean onSave(Object entity, Serializable id, Object[] state, String[] propertyNames, Type[] types) {
        String entityName = entity.getClass().getSimpleName();
        String sqlQuery = "INSERT INTO " + entityName.toLowerCase() + " (" + 
                Arrays.stream(propertyNames).collect(Collectors.joining(", ")) + ") VALUES (?)";
        
        Span span = observabilityService.getTracer().spanBuilder("DB Query: " + entityName + " Save")
                .setSpanKind(SpanKind.CLIENT)
                .setAttribute("db.system", "postgresql")
                .setAttribute("db.operation.name", "INSERT")
                .setAttribute("db.sql.table", entityName.toLowerCase())
                .setAttribute("db.statement", sqlQuery)
                .setAttribute("db.entity.class", entityName)
                .setAttribute("db.entity.id", String.valueOf(id))
                .setAttribute("db.operation.type", "save")
                .setAttribute("db.entity.properties", Arrays.toString(propertyNames))
                .startSpan();
        
        try (Scope scope = span.makeCurrent()) {
            String stateValues = Arrays.stream(state)
                    .map(val -> val != null ? val.toString() : "null")
                    .collect(Collectors.joining(", "));
            
            logger.info("🔍 SQL Query: {} | Values: [{}] | Entity: {}", 
                    sqlQuery, stateValues, entityName);
            
            span.setAttribute("db.entity.values", stateValues);
            span.setAttribute("db.query.success", true);
            span.setStatus(StatusCode.OK);
            
            return false; // Continue with normal processing
        } catch (Exception e) {
            span.recordException(e);
            span.setStatus(StatusCode.ERROR, "Entity save failed: " + e.getMessage());
            span.setAttribute("db.query.success", false);
            logger.error("❌ SQL Query failed: {} | Error: {}", sqlQuery, e.getMessage());
            throw e;
        } finally {
            span.end();
        }
    }
    
    @Override
    public boolean onFlushDirty(Object entity, Serializable id, Object[] currentState, 
                               Object[] previousState, String[] propertyNames, Type[] types) {
        String entityName = entity.getClass().getSimpleName();
        String sqlQuery = "UPDATE " + entityName.toLowerCase() + " SET " + 
                Arrays.stream(propertyNames).map(prop -> prop + " = ?").collect(Collectors.joining(", ")) + 
                " WHERE id = ?";
        
        Span span = observabilityService.getTracer().spanBuilder("DB Query: " + entityName + " Update")
                .setSpanKind(SpanKind.CLIENT)
                .setAttribute("db.system", "postgresql")
                .setAttribute("db.operation.name", "UPDATE")
                .setAttribute("db.sql.table", entityName.toLowerCase())
                .setAttribute("db.statement", sqlQuery)
                .setAttribute("db.entity.class", entityName)
                .setAttribute("db.entity.id", String.valueOf(id))
                .setAttribute("db.operation.type", "update")
                .startSpan();
        
        try (Scope scope = span.makeCurrent()) {
            String currentValues = Arrays.stream(currentState)
                    .map(val -> val != null ? val.toString() : "null")
                    .collect(Collectors.joining(", "));
            
            logger.info("🔍 SQL Query: {} | New Values: [{}] | Entity: {} | ID: {}", 
                    sqlQuery, currentValues, entityName, id);
            
            span.setAttribute("db.entity.current_values", currentValues);
            span.setAttribute("db.query.success", true);
            span.setStatus(StatusCode.OK);
            
            return false; // Continue with normal processing
        } catch (Exception e) {
            span.recordException(e);
            span.setStatus(StatusCode.ERROR, "Entity update failed: " + e.getMessage());
            span.setAttribute("db.query.success", false);
            logger.error("❌ SQL Query failed: {} | Error: {}", sqlQuery, e.getMessage());
            throw e;
        } finally {
            span.end();
        }
    }
    
    @Override
    public void onDelete(Object entity, Serializable id, Object[] state, String[] propertyNames, Type[] types) {
        String entityName = entity.getClass().getSimpleName();
        String sqlQuery = "DELETE FROM " + entityName.toLowerCase() + " WHERE id = ?";
        
        Span span = observabilityService.getTracer().spanBuilder("DB Query: " + entityName + " Delete")
                .setSpanKind(SpanKind.CLIENT)
                .setAttribute("db.system", "postgresql")
                .setAttribute("db.operation.name", "DELETE")
                .setAttribute("db.sql.table", entityName.toLowerCase())
                .setAttribute("db.statement", sqlQuery)
                .setAttribute("db.entity.class", entityName)
                .setAttribute("db.entity.id", String.valueOf(id))
                .setAttribute("db.operation.type", "delete")
                .startSpan();
        
        try (Scope scope = span.makeCurrent()) {
            logger.info("🔍 SQL Query: {} | Parameters: [id={}] | Entity: {}", 
                    sqlQuery, id, entityName);
            
            span.setAttribute("db.query.success", true);
            span.setStatus(StatusCode.OK);
        } catch (Exception e) {
            span.recordException(e);
            span.setStatus(StatusCode.ERROR, "Entity delete failed: " + e.getMessage());
            span.setAttribute("db.query.success", false);
            logger.error("❌ SQL Query failed: {} | Error: {}", sqlQuery, e.getMessage());
            throw e;
        } finally {
            span.end();
        }
    }
}
