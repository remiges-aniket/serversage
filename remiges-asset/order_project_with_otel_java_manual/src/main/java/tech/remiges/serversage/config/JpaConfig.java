package tech.remiges.serversage.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.orm.jpa.HibernatePropertiesCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.util.Map;

/**
 * JPA Configuration to enable database tracing interceptor and custom JDBC template
 */
@Configuration
public class JpaConfig {

    @Autowired
    private DatabaseTracingInterceptor databaseTracingInterceptor;
    
    @Autowired
    private DataSource dataSource;
    
    @Autowired
    private DatabaseTracingConfig.DatabaseTracer databaseTracer;

    @Bean
    public HibernatePropertiesCustomizer hibernatePropertiesCustomizer() {
        return new HibernatePropertiesCustomizer() {
            @Override
            public void customize(Map<String, Object> hibernateProperties) {
                hibernateProperties.put("hibernate.session_factory.interceptor", databaseTracingInterceptor);
                
                // Enable additional SQL logging for tracing
                hibernateProperties.put("hibernate.show_sql", true);
                hibernateProperties.put("hibernate.format_sql", true);
                hibernateProperties.put("hibernate.use_sql_comments", true);
                hibernateProperties.put("hibernate.generate_statistics", true);
                
                // Enable connection pool monitoring
                hibernateProperties.put("hibernate.connection.provider_disables_autocommit", false);
            }
        };
    }
    
    /**
     * Create a traced JDBC template that wraps all database operations
     * with detailed OpenTelemetry spans
     */
    @Bean
    @Primary
    public JdbcTemplate jdbcTemplate() {
        return new TracedJdbcTemplate(dataSource, databaseTracer);
    }
}
