package tech.remiges.serversage.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.remiges.serversage.exception.CustomExceptions;
import tech.remiges.serversage.model.Order;
import tech.remiges.serversage.model.Product;
import tech.remiges.serversage.model.User;
import tech.remiges.serversage.observability.ObservabilityService;
import tech.remiges.serversage.repository.OrderRepository;
import tech.remiges.serversage.repository.ProductRepository;
import tech.remiges.serversage.repository.UserRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

@Service
@Transactional
public class OrderService {

    private static final Logger logger = LoggerFactory.getLogger(OrderService.class);
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final ObservabilityService observabilityService;

    @Autowired
    public OrderService(OrderRepository orderRepository, UserRepository userRepository, 
                       ProductRepository productRepository, ObservabilityService observabilityService) {
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
        this.productRepository = productRepository;
        this.observabilityService = observabilityService;
    }

    public List<Order> getAllOrders() {
        logger.info("Fetching all orders");
        simulateRandomDelay();
        return orderRepository.findAll();
    }

    public Optional<Order> getOrderById(Long id) {
        logger.info("Fetching order with id: {}", id);
        
        // Simulate rate limiting for high frequency requests
        if (id != null && id == 997L) {
            logger.error("Rate limit exceeded for order ID: {}", id);
            throw new CustomExceptions.RateLimitException("Too many requests. Please try again later.");
        }
        
        simulateRandomDelay();
        return orderRepository.findById(id);
    }

    public Order createOrder(Order order) {
        logger.info("Creating new order for user: {} and product: {}", order.getUserId(), order.getProductId());
        
        // Validate order
        validateOrder(order);
        
        // Check if user exists
        User user = userRepository.findById(order.getUserId())
                .orElseThrow(() -> new CustomExceptions.UserNotFoundException("User not found with id: " + order.getUserId()));
        
        // Check if product exists and has sufficient stock
        Product product = productRepository.findById(order.getProductId())
                .orElseThrow(() -> new CustomExceptions.ProductNotFoundException("Product not found with id: " + order.getProductId()));
        
        if (product.getStockQuantity() < order.getQuantity()) {
            logger.error("Insufficient stock for product: {}. Available: {}, Requested: {}", 
                    product.getId(), product.getStockQuantity(), order.getQuantity());
            throw new CustomExceptions.InsufficientStockException("Insufficient stock available for product: " + product.getName());
        }
        
        // Calculate total amount
        BigDecimal totalAmount = product.getPrice().multiply(BigDecimal.valueOf(order.getQuantity()));
        order.setTotalAmount(totalAmount);
        
        // Simulate business logic error for high-value orders
        if (totalAmount.compareTo(BigDecimal.valueOf(50000)) > 0) {
            logger.error("Order value too high: {}", totalAmount);
            throw new CustomExceptions.BusinessLogicException("Orders above $50,000 require manual approval");
        }
        
        // Update product stock
        product.setStockQuantity(product.getStockQuantity() - order.getQuantity());
        productRepository.save(product);
        
        simulateRandomDelay();
        Order savedOrder = orderRepository.save(order);
        
        // Track order creation with proper observability
        observabilityService.logInfo("Order created successfully with ID: " + savedOrder.getId());
        observabilityService.updateOrderCount(1); // Increment by 1
        
        logger.info("Order created successfully with ID: {}", savedOrder.getId());
        return savedOrder;
    }

    public Optional<Order> updateOrderStatus(Long id, Order.OrderStatus status) {
        logger.info("Updating order status for id: {} to: {}", id, status);
        
        return orderRepository.findById(id)
                .map(order -> {
                    // Simulate business logic validation
                    if (order.getStatus() == Order.OrderStatus.DELIVERED && status != Order.OrderStatus.DELIVERED) {
                        logger.error("Cannot change status of delivered order: {}", id);
                        throw new CustomExceptions.BusinessLogicException("Cannot change status of delivered order");
                    }
                    
                    if (order.getStatus() == Order.OrderStatus.CANCELLED && status != Order.OrderStatus.CANCELLED) {
                        logger.error("Cannot change status of cancelled order: {}", id);
                        throw new CustomExceptions.BusinessLogicException("Cannot change status of cancelled order");
                    }
                    
                    order.setStatus(status);
                    simulateRandomDelay();
                    Order updatedOrder = orderRepository.save(order);
                    logger.info("Order status updated successfully: {}", updatedOrder.getId());
                    return updatedOrder;
                });
    }

    public void cancelOrder(Long id) {
        logger.info("Cancelling order with id: {}", id);
        
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new CustomExceptions.OrderNotFoundException("Order not found with id: " + id));
        
        if (order.getStatus() == Order.OrderStatus.DELIVERED) {
            logger.error("Cannot cancel delivered order: {}", id);
            throw new CustomExceptions.BusinessLogicException("Cannot cancel delivered order");
        }
        
        if (order.getStatus() == Order.OrderStatus.CANCELLED) {
            logger.error("Order already cancelled: {}", id);
            throw new CustomExceptions.BusinessLogicException("Order is already cancelled");
        }
        
        // Restore product stock
        Product product = productRepository.findById(order.getProductId())
                .orElseThrow(() -> new CustomExceptions.ProductNotFoundException("Product not found with id: " + order.getProductId()));
        
        product.setStockQuantity(product.getStockQuantity() + order.getQuantity());
        productRepository.save(product);
        
        order.setStatus(Order.OrderStatus.CANCELLED);
        orderRepository.save(order);
        
        // Track order cancellation
        observabilityService.logInfo("Order cancelled successfully: " + id);
        observabilityService.updateOrderCount(-1); // Decrement by 1
        
        logger.info("Order cancelled successfully: {}", id);
    }

    public List<Order> getOrdersByUser(Long userId) {
        logger.info("Fetching orders for user: {}", userId);
        
        // Verify user exists
        if (!userRepository.existsById(userId)) {
            throw new CustomExceptions.UserNotFoundException("User not found with id: " + userId);
        }
        
        simulateRandomDelay();
        return orderRepository.findByUserId(userId);
    }

    public List<Order> getOrdersByStatus(Order.OrderStatus status) {
        logger.info("Fetching orders by status: {}", status);
        simulateRandomDelay();
        return orderRepository.findByStatus(status);
    }

    public List<Order> getOrdersBetweenDates(LocalDateTime startDate, LocalDateTime endDate) {
        logger.info("Fetching orders between dates: {} and {}", startDate, endDate);
        
        if (startDate.isAfter(endDate)) {
            throw new CustomExceptions.ValidationException("Start date cannot be after end date");
        }
        
        simulateRandomDelay();
        return orderRepository.findOrdersBetweenDates(startDate, endDate);
    }

    public BigDecimal getTotalAmountByUser(Long userId) {
        logger.info("Calculating total amount for user: {}", userId);
        
        // Verify user exists
        if (!userRepository.existsById(userId)) {
            throw new CustomExceptions.UserNotFoundException("User not found with id: " + userId);
        }
        
        simulateRandomDelay();
        BigDecimal total = orderRepository.getTotalAmountByUser(userId);
        return total != null ? total : BigDecimal.ZERO;
    }

    public Long getOrderCountByStatus(Order.OrderStatus status) {
        logger.info("Getting order count for status: {}", status);
        simulateRandomDelay();
        return orderRepository.countByStatus(status);
    }

    public List<Order> getHighValueOrders(BigDecimal threshold) {
        logger.info("Fetching high value orders above: {}", threshold);
        simulateRandomDelay();
        return orderRepository.findHighValueOrders(threshold);
    }

    // Simulate payment processing with potential failures
    public String processPayment(Long orderId, BigDecimal amount) {
        logger.info("Processing payment for order: {} with amount: {}", orderId, amount);
        
        // Simulate payment service failure
        if (ThreadLocalRandom.current().nextInt(100) < 15) { // 15% failure rate
            logger.error("Payment processing failed for order: {}", orderId);
            throw new CustomExceptions.ExternalServiceException("Payment service is currently unavailable");
        }
        
        // Simulate payment timeout
        if (ThreadLocalRandom.current().nextInt(100) < 5) { // 5% timeout rate
            logger.error("Payment processing timed out for order: {}", orderId);
            simulateTimeout();
        }
        
        simulateRandomDelay();
        String transactionId = "TXN_" + System.currentTimeMillis();
        logger.info("Payment processed successfully for order: {} with transaction ID: {}", orderId, transactionId);
        return transactionId;
    }

    // Simulate inventory check with external service
    public boolean checkInventoryAvailability(Long productId, Integer quantity) {
        logger.info("Checking inventory availability for product: {} with quantity: {}", productId, quantity);
        
        // Simulate external inventory service failure
        if (ThreadLocalRandom.current().nextInt(100) < 10) { // 10% failure rate
            logger.error("Inventory service failed for product: {}", productId);
            throw new CustomExceptions.ExternalServiceException("Inventory service is currently unavailable");
        }
        
        simulateRandomDelay();
        return true; // Assume inventory is available
    }

    private void validateOrder(Order order) {
        if (order.getUserId() == null) {
            throw new CustomExceptions.ValidationException("User ID is required");
        }
        if (order.getProductId() == null) {
            throw new CustomExceptions.ValidationException("Product ID is required");
        }
        if (order.getQuantity() == null || order.getQuantity() <= 0) {
            throw new CustomExceptions.ValidationException("Quantity must be greater than 0");
        }
    }

    private void simulateRandomDelay() {
        try {
            // Random delay between 20-150ms to simulate real operations
            Thread.sleep(ThreadLocalRandom.current().nextInt(20, 151));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void simulateTimeout() {
        try {
            Thread.sleep(6000); // Simulate long operation
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new CustomExceptions.TimeoutException("Payment processing timed out");
        }
        throw new CustomExceptions.TimeoutException("Payment processing timed out");
    }
}
