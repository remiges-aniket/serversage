package tech.remiges.serversage.service;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.context.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import tech.remiges.serversage.exception.CustomExceptions;
import tech.remiges.serversage.model.Order;
import tech.remiges.serversage.observability.ObservabilityService;
import tech.remiges.serversage.repository.OrderRepository;
import tech.remiges.serversage.repository.ProductRepository;
import tech.remiges.serversage.repository.UserRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class AnalyticsService {

    private static final Logger logger = LoggerFactory.getLogger(AnalyticsService.class);
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final ObservabilityService observabilityService;

    @Autowired
    public AnalyticsService(UserRepository userRepository, ProductRepository productRepository, 
                           OrderRepository orderRepository, ObservabilityService observabilityService) {
        this.userRepository = userRepository;
        this.productRepository = productRepository;
        this.orderRepository = orderRepository;
        this.observabilityService = observabilityService;
    }

    public Map<String, Object> getDashboardStatistics() {
        logger.info("Generating dashboard statistics");
        simulateRandomDelay();

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalUsers", userRepository.count());
        stats.put("totalProducts", productRepository.count());
        stats.put("totalOrders", orderRepository.count());
        stats.put("pendingOrders", orderRepository.countByStatus(Order.OrderStatus.PENDING));
        stats.put("completedOrders", orderRepository.countByStatus(Order.OrderStatus.DELIVERED));
        stats.put("timestamp", LocalDateTime.now());
        stats.put("status", "healthy");

        return stats;
    }

    public Map<String, Object> getUserStatistics() {
        logger.info("Generating user statistics");
        simulateRandomDelay();

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalUsers", userRepository.count());
        stats.put("adminUsers", userRepository.countByRole("ADMIN"));
        stats.put("regularUsers", userRepository.countByRole("USER"));
        stats.put("managerUsers", userRepository.countByRole("MANAGER"));
        stats.put("employeeUsers", userRepository.countByRole("EMPLOYEE"));
        stats.put("activeUsers", userRepository.count() * 0.85); // Simulate 85% active
        stats.put("newUsersThisMonth", ThreadLocalRandom.current().nextInt(10, 50));

        return stats;
    }

    public Map<String, Object> getProductStatistics() {
        logger.info("Generating product statistics");
        simulateRandomDelay();

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalProducts", productRepository.count());
        stats.put("availableProducts", productRepository.findAvailableProducts().size());
        stats.put("lowStockProducts", productRepository.findByStockQuantityLessThan(10).size());
        stats.put("electronicsCategory", productRepository.countByCategory("electronics"));
        stats.put("clothingCategory", productRepository.countByCategory("clothing"));
        stats.put("booksCategory", productRepository.countByCategory("books"));
        stats.put("averagePrice", BigDecimal.valueOf(ThreadLocalRandom.current().nextDouble(50, 500)));

        return stats;
    }

    public Map<String, Object> getOrderStatistics() {
        logger.info("Generating order statistics");
        simulateRandomDelay();

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalOrders", orderRepository.count());
        stats.put("pendingOrders", orderRepository.countByStatus(Order.OrderStatus.PENDING));
        stats.put("confirmedOrders", orderRepository.countByStatus(Order.OrderStatus.CONFIRMED));
        stats.put("shippedOrders", orderRepository.countByStatus(Order.OrderStatus.SHIPPED));
        stats.put("deliveredOrders", orderRepository.countByStatus(Order.OrderStatus.DELIVERED));
        stats.put("cancelledOrders", orderRepository.countByStatus(Order.OrderStatus.CANCELLED));
        stats.put("averageOrderValue", BigDecimal.valueOf(ThreadLocalRandom.current().nextDouble(100, 1000)));
        stats.put("todaysOrders", ThreadLocalRandom.current().nextInt(5, 25));

        return stats;
    }

    public Map<String, Object> getMonthlyRevenue(int year) {
        logger.info("Calculating monthly revenue for year: {}", year);
        simulateRandomDelay();

        Map<String, Object> revenue = new HashMap<>();
        Map<String, BigDecimal> monthlyData = new HashMap<>();

        // Simulate monthly revenue data
        String[] months = {"January", "February", "March", "April", "May", "June",
                          "July", "August", "September", "October", "November", "December"};

        for (String month : months) {
            monthlyData.put(month, BigDecimal.valueOf(ThreadLocalRandom.current().nextDouble(10000, 50000)));
        }

        revenue.put("year", year);
        revenue.put("monthlyRevenue", monthlyData);
        revenue.put("totalRevenue", monthlyData.values().stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        revenue.put("averageMonthlyRevenue", 
                monthlyData.values().stream()
                        .reduce(BigDecimal.ZERO, BigDecimal::add)
                        .divide(BigDecimal.valueOf(12), 2, BigDecimal.ROUND_HALF_UP));

        return revenue;
    }

    public Map<String, Object> getPerformanceMetrics() {
        long startTime = System.nanoTime();
        
        // Create a detailed span for performance metrics collection
        Span span = observabilityService.getTracer().spanBuilder("AnalyticsService.getPerformanceMetrics")
                .setSpanKind(SpanKind.CLIENT)
                .setAttribute("service.method", "getPerformanceMetrics")
                .setAttribute("operation.type", "performance_analysis")
                .startSpan();
        
        try (var scope = span.makeCurrent()) {
            logger.info("🔍 Collecting performance metrics with database queries");
            simulateRandomDelay();

            // Simulate performance failure occasionally
            if (ThreadLocalRandom.current().nextInt(100) < 10) { // 10% failure rate
                logger.error("Performance metrics collection failed");
                throw new CustomExceptions.ExternalServiceException("Performance monitoring service is unavailable");
            }

            Map<String, Object> metrics = new HashMap<>();
            Runtime runtime = Runtime.getRuntime();

            metrics.put("jvmMemoryUsed", runtime.totalMemory() - runtime.freeMemory());
            metrics.put("jvmMemoryFree", runtime.freeMemory());
            metrics.put("jvmMemoryTotal", runtime.totalMemory());
            metrics.put("jvmMemoryMax", runtime.maxMemory());
            metrics.put("availableProcessors", runtime.availableProcessors());
            metrics.put("systemLoadAverage", ThreadLocalRandom.current().nextDouble(0.1, 2.0));
            metrics.put("responseTimeAvg", ThreadLocalRandom.current().nextInt(50, 200));
            metrics.put("requestsPerSecond", ThreadLocalRandom.current().nextInt(10, 100));
            metrics.put("errorRate", ThreadLocalRandom.current().nextDouble(0.01, 0.05));
            metrics.put("uptime", System.currentTimeMillis());

            double durationSeconds = (System.nanoTime() - startTime) / 1_000_000_000.0;
            
            span.setAttribute("performance.collection.duration_ms", durationSeconds * 1000);
            span.setAttribute("performance.metrics.count", metrics.size());
            span.setAttribute("operation.success", true);
            span.setStatus(StatusCode.OK);
            
            logger.info("✅ Performance metrics collected successfully in " + 
                    String.format("%.3f", durationSeconds) + "s");

            return metrics;
            
        } catch (Exception e) {
            span.recordException(e);
            span.setStatus(StatusCode.ERROR, e.getMessage());
            span.setAttribute("operation.success", false);
            span.setAttribute("error.type", e.getClass().getSimpleName());
            
            logger.error("❌ Performance metrics collection failed: " + e.getMessage());
            throw e;
        } finally {
            span.end();
        }
    }

    public Map<String, Object> getDetailedHealthCheck() {
        logger.info("Performing detailed health check");
        simulateRandomDelay();

        Map<String, Object> health = new HashMap<>();
        
        try {
            // Check database connectivity
            long userCount = userRepository.count();
            health.put("database", Map.of(
                    "status", "UP",
                    "responseTime", ThreadLocalRandom.current().nextInt(10, 50) + "ms",
                    "userCount", userCount
            ));
        } catch (Exception e) {
            logger.error("Database health check failed", e);
            health.put("database", Map.of(
                    "status", "DOWN",
                    "error", e.getMessage()
            ));
        }

        // Simulate external service checks
        health.put("paymentService", Map.of(
                "status", ThreadLocalRandom.current().nextBoolean() ? "UP" : "DOWN",
                "responseTime", ThreadLocalRandom.current().nextInt(100, 300) + "ms"
        ));

        health.put("inventoryService", Map.of(
                "status", ThreadLocalRandom.current().nextBoolean() ? "UP" : "DOWN",
                "responseTime", ThreadLocalRandom.current().nextInt(50, 200) + "ms"
        ));

        health.put("overallStatus", "UP");
        health.put("timestamp", LocalDateTime.now());

        return health;
    }

    public Map<String, Object> generateCustomReport(String reportType, String dateRange) {
        logger.info("Generating custom report: {} for date range: {}", reportType, dateRange);
        
        if (reportType == null || reportType.trim().isEmpty()) {
            throw new CustomExceptions.ValidationException("Report type is required");
        }

        simulateRandomDelay();

        Map<String, Object> report = new HashMap<>();
        report.put("reportType", reportType);
        report.put("dateRange", dateRange);
        report.put("generatedAt", LocalDateTime.now());

        switch (reportType.toLowerCase()) {
            case "sales":
                report.put("totalSales", BigDecimal.valueOf(ThreadLocalRandom.current().nextDouble(10000, 100000)));
                report.put("orderCount", ThreadLocalRandom.current().nextInt(100, 1000));
                break;
            case "users":
                report.put("newUsers", ThreadLocalRandom.current().nextInt(10, 100));
                report.put("activeUsers", ThreadLocalRandom.current().nextInt(500, 2000));
                break;
            case "products":
                report.put("topSellingProducts", Map.of("Product A", 150, "Product B", 120, "Product C", 90));
                report.put("lowStockAlerts", ThreadLocalRandom.current().nextInt(5, 20));
                break;
            default:
                throw new CustomExceptions.ValidationException("Unsupported report type: " + reportType);
        }

        return report;
    }

    public Map<String, Object> getErrorSummary() {
        logger.info("Generating error summary");
        simulateRandomDelay();

        Map<String, Object> errorSummary = new HashMap<>();
        errorSummary.put("totalErrors", ThreadLocalRandom.current().nextInt(10, 100));
        errorSummary.put("criticalErrors", ThreadLocalRandom.current().nextInt(0, 5));
        errorSummary.put("warningErrors", ThreadLocalRandom.current().nextInt(5, 20));
        errorSummary.put("infoErrors", ThreadLocalRandom.current().nextInt(20, 50));
        
        Map<String, Integer> errorTypes = new HashMap<>();
        errorTypes.put("ValidationException", ThreadLocalRandom.current().nextInt(5, 15));
        errorTypes.put("DatabaseConnectionException", ThreadLocalRandom.current().nextInt(1, 5));
        errorTypes.put("ExternalServiceException", ThreadLocalRandom.current().nextInt(2, 8));
        errorTypes.put("BusinessLogicException", ThreadLocalRandom.current().nextInt(3, 10));
        errorTypes.put("TimeoutException", ThreadLocalRandom.current().nextInt(1, 3));
        
        errorSummary.put("errorsByType", errorTypes);
        errorSummary.put("errorRate", ThreadLocalRandom.current().nextDouble(0.01, 0.05));
        errorSummary.put("lastUpdated", LocalDateTime.now());

        return errorSummary;
    }

    public Map<String, Object> getCacheStatistics() {
        logger.info("Collecting cache statistics");
        simulateRandomDelay();

        Map<String, Object> cacheStats = new HashMap<>();
        cacheStats.put("hitRate", ThreadLocalRandom.current().nextDouble(0.7, 0.95));
        cacheStats.put("missRate", ThreadLocalRandom.current().nextDouble(0.05, 0.3));
        cacheStats.put("totalRequests", ThreadLocalRandom.current().nextInt(1000, 10000));
        cacheStats.put("cacheSize", ThreadLocalRandom.current().nextInt(100, 1000));
        cacheStats.put("evictions", ThreadLocalRandom.current().nextInt(10, 100));
        cacheStats.put("averageLoadTime", ThreadLocalRandom.current().nextInt(10, 50) + "ms");

        return cacheStats;
    }

    private void simulateRandomDelay() {
        try {
            // Random delay between 20-150ms to simulate real operations
            Thread.sleep(ThreadLocalRandom.current().nextInt(20, 151));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
