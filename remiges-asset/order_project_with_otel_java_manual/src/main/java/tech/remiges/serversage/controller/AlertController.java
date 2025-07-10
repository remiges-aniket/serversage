package tech.remiges.serversage.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/api/alerts")
@Tag(name = "Alert Management", description = "APIs for receiving and managing alerts from Grafana")
public class AlertController {

    private static final Logger logger = LoggerFactory.getLogger(AlertController.class);
    
    // In-memory storage for demo purposes
    private final Map<String, List<AlertNotification>> alertHistory = new ConcurrentHashMap<>();
    
    @PostMapping("/webhook")
    @Operation(summary = "Receive general alerts", description = "Webhook endpoint for receiving general alerts from Grafana")
    public ResponseEntity<String> receiveGeneralAlert(@RequestBody Map<String, Object> alertPayload) {
        logger.info("📢 Received general alert: {}", alertPayload);
        
        AlertNotification notification = new AlertNotification(
            "GENERAL",
            extractTitle(alertPayload),
            extractMessage(alertPayload),
            LocalDateTime.now()
        );
        
        alertHistory.computeIfAbsent("general", k -> new ArrayList<>()).add(notification);
        
        // Log with structured format for observability
        logger.warn("ALERT_RECEIVED: type=general, title={}, timestamp={}", 
            notification.title(), notification.timestamp());
        
        return ResponseEntity.ok("Alert received successfully");
    }
    
    @PostMapping("/critical")
    @Operation(summary = "Receive critical alerts", description = "Webhook endpoint for receiving critical alerts from Grafana")
    public ResponseEntity<String> receiveCriticalAlert(@RequestBody Map<String, Object> alertPayload) {
        logger.error("🚨 Received CRITICAL alert: {}", alertPayload);
        
        AlertNotification notification = new AlertNotification(
            "CRITICAL",
            extractTitle(alertPayload),
            extractMessage(alertPayload),
            LocalDateTime.now()
        );
        
        alertHistory.computeIfAbsent("critical", k -> new ArrayList<>()).add(notification);
        
        // Log with structured format for observability
        logger.error("CRITICAL_ALERT_RECEIVED: title={}, message={}, timestamp={}", 
            notification.title(), notification.message(), notification.timestamp());
        
        return ResponseEntity.ok("Critical alert received successfully");
    }
    
    @PostMapping("/warning")
    @Operation(summary = "Receive warning alerts", description = "Webhook endpoint for receiving warning alerts from Grafana")
    public ResponseEntity<String> receiveWarningAlert(@RequestBody Map<String, Object> alertPayload) {
        logger.warn("⚠️ Received WARNING alert: {}", alertPayload);
        
        AlertNotification notification = new AlertNotification(
            "WARNING",
            extractTitle(alertPayload),
            extractMessage(alertPayload),
            LocalDateTime.now()
        );
        
        alertHistory.computeIfAbsent("warning", k -> new ArrayList<>()).add(notification);
        
        // Log with structured format for observability
        logger.warn("WARNING_ALERT_RECEIVED: title={}, message={}, timestamp={}", 
            notification.title(), notification.message(), notification.timestamp());
        
        return ResponseEntity.ok("Warning alert received successfully");
    }
    
    @GetMapping("/history")
    @Operation(summary = "Get alert history", description = "Retrieve the history of received alerts")
    public ResponseEntity<Map<String, List<AlertNotification>>> getAlertHistory() {
        logger.info("Retrieving alert history");
        return ResponseEntity.ok(alertHistory);
    }
    
    @GetMapping("/history/{type}")
    @Operation(summary = "Get alert history by type", description = "Retrieve the history of alerts by type (general, critical, warning)")
    public ResponseEntity<List<AlertNotification>> getAlertHistoryByType(@PathVariable String type) {
        logger.info("Retrieving alert history for type: {}", type);
        List<AlertNotification> alerts = alertHistory.getOrDefault(type.toLowerCase(), new ArrayList<>());
        return ResponseEntity.ok(alerts);
    }
    
    @DeleteMapping("/history")
    @Operation(summary = "Clear alert history", description = "Clear all alert history")
    public ResponseEntity<String> clearAlertHistory() {
        logger.info("Clearing alert history");
        alertHistory.clear();
        return ResponseEntity.ok("Alert history cleared successfully");
    }
    
    @GetMapping("/status")
    @Operation(summary = "Get alert system status", description = "Get the current status of the alert system")
    public ResponseEntity<Map<String, Object>> getAlertStatus() {
        Map<String, Object> status = Map.of(
            "totalAlerts", alertHistory.values().stream().mapToInt(List::size).sum(),
            "criticalAlerts", alertHistory.getOrDefault("critical", new ArrayList<>()).size(),
            "warningAlerts", alertHistory.getOrDefault("warning", new ArrayList<>()).size(),
            "generalAlerts", alertHistory.getOrDefault("general", new ArrayList<>()).size(),
            "lastUpdated", LocalDateTime.now()
        );
        
        return ResponseEntity.ok(status);
    }
    
    private String extractTitle(Map<String, Object> payload) {
        return payload.getOrDefault("title", "Unknown Alert").toString();
    }
    
    private String extractMessage(Map<String, Object> payload) {
        return payload.getOrDefault("message", "No message provided").toString();
    }
    
    public record AlertNotification(
        String type,
        String title,
        String message,
        LocalDateTime timestamp
    ) {}
}
