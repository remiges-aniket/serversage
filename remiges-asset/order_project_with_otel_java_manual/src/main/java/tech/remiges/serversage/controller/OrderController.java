package tech.remiges.serversage.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tech.remiges.serversage.model.Order;
import tech.remiges.serversage.service.OrderService;

import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/orders")
@Tag(name = "Order Management", description = "APIs for managing orders with various error scenarios")
public class OrderController {

    private final OrderService orderService;

    @Autowired
    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping
    @Operation(summary = "Get all orders", description = "Retrieve all orders from the database")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved orders"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<List<Order.OrderDTO>> getAllOrders() {
        var orders = orderService.getAllOrders()
                .stream()
                .map(Order.OrderDTO::fromEntity)
                .toList();
        return ResponseEntity.ok(orders);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get order by ID", description = "Retrieve a specific order by its ID. Use ID 997 to trigger rate limit error")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Order found"),
            @ApiResponse(responseCode = "404", description = "Order not found"),
            @ApiResponse(responseCode = "429", description = "Rate limit exceeded"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<Order.OrderDTO> getOrderById(
            @Parameter(description = "Order ID", example = "1") @PathVariable Long id) {
        return orderService.getOrderById(id)
                .map(Order.OrderDTO::fromEntity)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @Operation(summary = "Create new order", description = "Create a new order. High-value orders (>$50,000) will trigger business logic error")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Order created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input or insufficient stock"),
            @ApiResponse(responseCode = "404", description = "User or product not found"),
            @ApiResponse(responseCode = "422", description = "Business logic error"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<Order.OrderDTO> createOrder(@Valid @RequestBody Order order) {
        Order createdOrder = orderService.createOrder(order);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Order.OrderDTO.fromEntity(createdOrder));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Update order status", description = "Update the status of an existing order")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Order status updated successfully"),
            @ApiResponse(responseCode = "404", description = "Order not found"),
            @ApiResponse(responseCode = "422", description = "Invalid status transition"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<Order.OrderDTO> updateOrderStatus(
            @Parameter(description = "Order ID") @PathVariable Long id,
            @Parameter(description = "New order status") @RequestParam Order.OrderStatus status) {
        return orderService.updateOrderStatus(id, status)
                .map(Order.OrderDTO::fromEntity)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Cancel order", description = "Cancel an existing order and restore product stock")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Order cancelled successfully"),
            @ApiResponse(responseCode = "404", description = "Order not found"),
            @ApiResponse(responseCode = "422", description = "Cannot cancel order in current status"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<String> cancelOrder(
            @Parameter(description = "Order ID") @PathVariable Long id) {
        orderService.cancelOrder(id);
        return ResponseEntity.ok("Order cancelled successfully");
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "Get orders by user", description = "Retrieve all orders for a specific user")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Orders retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "User not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<List<Order.OrderDTO>> getOrdersByUser(
            @Parameter(description = "User ID", example = "1") @PathVariable Long userId) {
        var orders = orderService.getOrdersByUser(userId)
                .stream()
                .map(Order.OrderDTO::fromEntity)
                .toList();
        return ResponseEntity.ok(orders);
    }

    @GetMapping("/status/{status}")
    @Operation(summary = "Get orders by status", description = "Retrieve all orders with a specific status")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Orders retrieved successfully"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<List<Order.OrderDTO>> getOrdersByStatus(
            @Parameter(description = "Order status") @PathVariable Order.OrderStatus status) {
        var orders = orderService.getOrdersByStatus(status)
                .stream()
                .map(Order.OrderDTO::fromEntity)
                .toList();
        return ResponseEntity.ok(orders);
    }

    @GetMapping("/date-range")
    @Operation(summary = "Get orders by date range", description = "Retrieve orders within a specific date range")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Orders retrieved successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid date range"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<List<Order.OrderDTO>> getOrdersBetweenDates(
            @Parameter(description = "Start date", example = "2024-01-01T00:00:00") 
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @Parameter(description = "End date", example = "2024-12-31T23:59:59") 
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        var orders = orderService.getOrdersBetweenDates(startDate, endDate)
                .stream()
                .map(Order.OrderDTO::fromEntity)
                .toList();
        return ResponseEntity.ok(orders);
    }

    @GetMapping("/user/{userId}/total")
    @Operation(summary = "Get total amount by user", description = "Calculate total order amount for a specific user")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Total amount calculated"),
            @ApiResponse(responseCode = "404", description = "User not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<BigDecimal> getTotalAmountByUser(
            @Parameter(description = "User ID") @PathVariable Long userId) {
        BigDecimal totalAmount = orderService.getTotalAmountByUser(userId);
        return ResponseEntity.ok(totalAmount);
    }

    @GetMapping("/status/{status}/count")
    @Operation(summary = "Get order count by status", description = "Get the number of orders with a specific status")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Count retrieved successfully"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<Long> getOrderCountByStatus(
            @Parameter(description = "Order status") @PathVariable Order.OrderStatus status) {
        Long count = orderService.getOrderCountByStatus(status);
        return ResponseEntity.ok(count);
    }

    @GetMapping("/high-value")
    @Operation(summary = "Get high value orders", description = "Retrieve orders above a specified amount threshold")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "High value orders retrieved"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<List<Order.OrderDTO>> getHighValueOrders(
            @Parameter(description = "Amount threshold", example = "1000.00") 
            @RequestParam(defaultValue = "1000.00") BigDecimal threshold) {
        var orders = orderService.getHighValueOrders(threshold)
                .stream()
                .map(Order.OrderDTO::fromEntity)
                .toList();
        return ResponseEntity.ok(orders);
    }

    @PostMapping("/{id}/payment")
    @Operation(summary = "Process payment", description = "Process payment for an order (simulates external service with 15% failure rate)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Payment processed successfully"),
            @ApiResponse(responseCode = "404", description = "Order not found"),
            @ApiResponse(responseCode = "408", description = "Payment timeout"),
            @ApiResponse(responseCode = "503", description = "Payment service unavailable"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<Map<String, String>> processPayment(
            @Parameter(description = "Order ID") @PathVariable Long id,
            @Parameter(description = "Payment amount") @RequestParam BigDecimal amount) {
        String transactionId = orderService.processPayment(id, amount);
        return ResponseEntity.ok(Map.of("transactionId", transactionId, "status", "SUCCESS"));
    }

    @GetMapping("/inventory-check")
    @Operation(summary = "Check inventory availability", description = "Check if sufficient inventory is available (simulates external service with 10% failure rate)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Inventory check completed"),
            @ApiResponse(responseCode = "503", description = "Inventory service unavailable"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<Map<String, Boolean>> checkInventoryAvailability(
            @Parameter(description = "Product ID") @RequestParam Long productId,
            @Parameter(description = "Required quantity") @RequestParam Integer quantity) {
        boolean available = orderService.checkInventoryAvailability(productId, quantity);
        return ResponseEntity.ok(Map.of("available", available));
    }
}
