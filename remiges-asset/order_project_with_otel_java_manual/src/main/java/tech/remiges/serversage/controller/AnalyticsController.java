package tech.remiges.serversage.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tech.remiges.serversage.service.AnalyticsService;

import java.util.Map;

@RestController
@RequestMapping("/api/analytics")
@Tag(name = "Analytics & Reporting", description = "APIs for analytics and reporting with various error scenarios")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @Autowired
    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @GetMapping("/dashboard")
    @Operation(summary = "Get dashboard statistics", description = "Retrieve comprehensive dashboard statistics")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Dashboard data retrieved successfully"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<Map<String, Object>> getDashboardStats() {
        Map<String, Object> stats = analyticsService.getDashboardStatistics();
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/users/statistics")
    @Operation(summary = "Get user statistics", description = "Retrieve detailed user statistics")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User statistics retrieved successfully"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<Map<String, Object>> getUserStatistics() {
        Map<String, Object> stats = analyticsService.getUserStatistics();
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/products/statistics")
    @Operation(summary = "Get product statistics", description = "Retrieve detailed product statistics")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Product statistics retrieved successfully"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<Map<String, Object>> getProductStatistics() {
        Map<String, Object> stats = analyticsService.getProductStatistics();
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/orders/statistics")
    @Operation(summary = "Get order statistics", description = "Retrieve detailed order statistics")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Order statistics retrieved successfully"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<Map<String, Object>> getOrderStatistics() {
        Map<String, Object> stats = analyticsService.getOrderStatistics();
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/revenue/monthly")
    @Operation(summary = "Get monthly revenue", description = "Calculate monthly revenue data")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Monthly revenue calculated successfully"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<Map<String, Object>> getMonthlyRevenue(
            @Parameter(description = "Year", example = "2024") @RequestParam(defaultValue = "2024") int year) {
        Map<String, Object> revenue = analyticsService.getMonthlyRevenue(year);
        return ResponseEntity.ok(revenue);
    }

    @GetMapping("/performance/metrics")
    @Operation(summary = "Get performance metrics", description = "Retrieve system performance metrics")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Performance metrics retrieved successfully"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<Map<String, Object>> getPerformanceMetrics() {
        Map<String, Object> metrics = analyticsService.getPerformanceMetrics();
        return ResponseEntity.ok(metrics);
    }

    @GetMapping("/health/detailed")
    @Operation(summary = "Get detailed health check", description = "Perform comprehensive health check with database connectivity")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Health check completed successfully"),
            @ApiResponse(responseCode = "503", description = "Service unhealthy"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<Map<String, Object>> getDetailedHealthCheck() {
        Map<String, Object> health = analyticsService.getDetailedHealthCheck();
        return ResponseEntity.ok(health);
    }

    @PostMapping("/reports/generate")
    @Operation(summary = "Generate custom report", description = "Generate a custom report based on parameters")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Report generated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid report parameters"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<Map<String, Object>> generateReport(
            @Parameter(description = "Report type") @RequestParam String reportType,
            @Parameter(description = "Date range") @RequestParam(required = false) String dateRange) {
        Map<String, Object> report = analyticsService.generateCustomReport(reportType, dateRange);
        return ResponseEntity.ok(report);
    }

    @GetMapping("/errors/summary")
    @Operation(summary = "Get error summary", description = "Retrieve error statistics and summary")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Error summary retrieved successfully"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<Map<String, Object>> getErrorSummary() {
        Map<String, Object> errorSummary = analyticsService.getErrorSummary();
        return ResponseEntity.ok(errorSummary);
    }

    @GetMapping("/cache/statistics")
    @Operation(summary = "Get cache statistics", description = "Retrieve cache performance statistics")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Cache statistics retrieved successfully"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<Map<String, Object>> getCacheStatistics() {
        Map<String, Object> cacheStats = analyticsService.getCacheStatistics();
        return ResponseEntity.ok(cacheStats);
    }
}
