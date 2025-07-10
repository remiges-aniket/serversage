package tech.remiges.serversage.config;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.context.Scope;
import org.hibernate.Interceptor;
import org.hibernate.type.Type;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.util.logging.Logger;

/**
 * Hibernate interceptor to trace database operations with detailed information
 */
@Component
public class DatabaseTracingInterceptor implements Interceptor {

    private static final Logger logger = Logger.getLogger(DatabaseTracingInterceptor.class.getName());
    
    @Autowired
    private DatabaseTracingConfig.DatabaseTracer databaseTracer;
    
    @Override
    public boolean onLoad(Object entity, Serializable id, Object[] state, String[] propertyNames, Type[] types) {
        Span span = databaseTracer.createDatabaseSpan("load", "SELECT * FROM " + entity.getClass().getSimpleName() + " WHERE id = " + id);
        
        try (Scope scope = span.makeCurrent()) {
            span.setAttribute("db.operation", "load");
            span.setAttribute("db.entity.class", entity.getClass().getSimpleName());
            span.setAttribute("db.entity.id", String.valueOf(id));
            span.setAttribute("db.entity.properties.count", propertyNames.length);
            
            logger.info("Loading entity: " + entity.getClass().getSimpleName() + " with ID: " + id);
            
            return false; // Continue with normal processing
        } catch (Exception e) {
            span.recordException(e);
            span.setStatus(io.opentelemetry.api.trace.StatusCode.ERROR, "Entity load failed: " + e.getMessage());
            throw e;
        } finally {
            span.end();
        }
    }
    
    @Override
    public boolean onSave(Object entity, Serializable id, Object[] state, String[] propertyNames, Type[] types) {
        Span span = databaseTracer.createDatabaseSpan("save", "INSERT INTO " + entity.getClass().getSimpleName());
        
        try (Scope scope = span.makeCurrent()) {
            span.setAttribute("db.operation", "save");
            span.setAttribute("db.entity.class", entity.getClass().getSimpleName());
            span.setAttribute("db.entity.id", id != null ? String.valueOf(id) : "new");
            span.setAttribute("db.entity.properties.count", propertyNames.length);
            
            // Log entity state for debugging
            StringBuilder stateInfo = new StringBuilder();
            for (int i = 0; i < propertyNames.length && i < state.length; i++) {
                if (i > 0) stateInfo.append(", ");
                stateInfo.append(propertyNames[i]).append("=").append(state[i]);
            }
            
            span.setAttribute("db.entity.state", stateInfo.toString());
            logger.info("Saving entity: " + entity.getClass().getSimpleName() + " with state: " + stateInfo);
            
            return false; // Continue with normal processing
        } catch (Exception e) {
            span.recordException(e);
            span.setStatus(io.opentelemetry.api.trace.StatusCode.ERROR, "Entity save failed: " + e.getMessage());
            throw e;
        } finally {
            span.end();
        }
    }
    
    @Override
    public void onDelete(Object entity, Serializable id, Object[] state, String[] propertyNames, Type[] types) {
        Span span = databaseTracer.createDatabaseSpan("delete", "DELETE FROM " + entity.getClass().getSimpleName() + " WHERE id = " + id);
        
        try (Scope scope = span.makeCurrent()) {
            span.setAttribute("db.operation", "delete");
            span.setAttribute("db.entity.class", entity.getClass().getSimpleName());
            span.setAttribute("db.entity.id", String.valueOf(id));
            
            logger.info("Deleting entity: " + entity.getClass().getSimpleName() + " with ID: " + id);
            
        } catch (Exception e) {
            span.recordException(e);
            span.setStatus(io.opentelemetry.api.trace.StatusCode.ERROR, "Entity delete failed: " + e.getMessage());
            throw e;
        } finally {
            span.end();
        }
    }
    
    @Override
    public boolean onFlushDirty(Object entity, Serializable id, Object[] currentState, Object[] previousState, 
                               String[] propertyNames, Type[] types) {
        Span span = databaseTracer.createDatabaseSpan("update", "UPDATE " + entity.getClass().getSimpleName() + " WHERE id = " + id);
        
        try (Scope scope = span.makeCurrent()) {
            span.setAttribute("db.operation", "update");
            span.setAttribute("db.entity.class", entity.getClass().getSimpleName());
            span.setAttribute("db.entity.id", String.valueOf(id));
            
            // Track what fields changed
            StringBuilder changes = new StringBuilder();
            int changeCount = 0;
            for (int i = 0; i < propertyNames.length && i < currentState.length && i < previousState.length; i++) {
                Object current = currentState[i];
                Object previous = previousState[i];
                
                if ((current == null && previous != null) || 
                    (current != null && !current.equals(previous))) {
                    if (changeCount > 0) changes.append(", ");
                    changes.append(propertyNames[i]).append(": ").append(previous).append(" -> ").append(current);
                    changeCount++;
                }
            }
            
            span.setAttribute("db.entity.changes", changes.toString());
            span.setAttribute("db.entity.changes.count", changeCount);
            
            logger.info("Updating entity: " + entity.getClass().getSimpleName() + " with ID: " + id + 
                       ", changes: " + changes);
            
            return false; // Continue with normal processing
        } catch (Exception e) {
            span.recordException(e);
            span.setStatus(io.opentelemetry.api.trace.StatusCode.ERROR, "Entity update failed: " + e.getMessage());
            throw e;
        } finally {
            span.end();
        }
    }
}
