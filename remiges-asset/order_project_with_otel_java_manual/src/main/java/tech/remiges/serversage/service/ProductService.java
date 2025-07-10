package tech.remiges.serversage.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.remiges.serversage.exception.CustomExceptions;
import tech.remiges.serversage.model.Product;
import tech.remiges.serversage.repository.ProductRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;

@Service
@Transactional
public class ProductService {

    private static final Logger logger = LoggerFactory.getLogger(ProductService.class);
    private final ProductRepository productRepository;

    @Autowired
    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Cacheable("products")
    public List<Product> getAllProducts() {
        logger.info("Fetching all products from database");
        simulateRandomDelay();
        return productRepository.findAll();
    }

    public Optional<Product> getProductById(Long id) {
        logger.info("Fetching product with id: {}", id);
        
        // Simulate array index out of bounds error for specific IDs
        if (id != null && id == 999L) {
            String[] array = {"test"};
            logger.error("Attempting to access array index out of bounds for product ID: {}", id);
            return Optional.of(new Product(array[5], "Error Product", BigDecimal.ZERO, 0, "ERROR")); // This will throw ArrayIndexOutOfBoundsException
        }
        
        // Simulate null pointer exception for specific IDs
        if (id != null && id == 998L) {
            String nullString = null;
            logger.error("Attempting null pointer access for product ID: {}", id);
            return Optional.of(new Product(nullString.toUpperCase(), "Error Product", BigDecimal.ZERO, 0, "ERROR")); // This will throw NullPointerException
        }
        
        simulateRandomDelay();
        return productRepository.findById(id);
    }

    public Product createProduct(Product product) {
        logger.info("Creating new product: {}", product.getName());
        
        // Validate product
        validateProduct(product);
        
        // Check for duplicate product name
        if (productRepository.existsByName(product.getName())) {
            logger.error("Product with name '{}' already exists", product.getName());
            throw new CustomExceptions.DuplicateProductException("Product with name '" + product.getName() + "' already exists");
        }
        
        // Simulate database connection error for specific product names
        if (product.getName() != null && product.getName().toLowerCase().contains("dberror")) {
            logger.error("Simulating database connection error for product: {}", product.getName());
            throw new CustomExceptions.DatabaseConnectionException("Database connection failed while creating product");
        }
        
        try {
            simulateRandomDelay();
            Product savedProduct = productRepository.save(product);
            logger.info("Product created successfully with ID: {}", savedProduct.getId());
            return savedProduct;
        } catch (DataIntegrityViolationException e) {
            logger.error("Data integrity violation while creating product: {}", product.getName(), e);
            throw new CustomExceptions.DuplicateProductException("Product with this name already exists", e);
        }
    }

    @CacheEvict(value = "products", allEntries = true)
    public Optional<Product> updateProduct(Long id, Product productDetails) {
        logger.info("Updating product with id: {}", id);
        
        return productRepository.findById(id)
                .map(product -> {
                    // Simulate business logic error for specific scenarios
                    if (productDetails.getPrice() != null && productDetails.getPrice().compareTo(BigDecimal.valueOf(10000)) > 0) {
                        logger.error("Price too high for product update: {}", productDetails.getPrice());
                        throw new CustomExceptions.BusinessLogicException("Product price cannot exceed $10,000");
                    }
                    
                    product.setName(productDetails.getName());
                    product.setDescription(productDetails.getDescription());
                    product.setPrice(productDetails.getPrice());
                    product.setStockQuantity(productDetails.getStockQuantity());
                    product.setCategory(productDetails.getCategory());
                    
                    simulateRandomDelay();
                    Product updatedProduct = productRepository.save(product);
                    logger.info("Product updated successfully: {}", updatedProduct.getId());
                    return updatedProduct;
                });
    }

    @CacheEvict(value = "products", allEntries = true)
    public void deleteProduct(Long id) {
        logger.info("Deleting product with id: {}", id);
        
        if (!productRepository.existsById(id)) {
            logger.error("Product not found for deletion: {}", id);
            throw new CustomExceptions.ProductNotFoundException("Product not found with id: " + id);
        }
        
        simulateRandomDelay();
        productRepository.deleteById(id);
        logger.info("Product deleted successfully: {}", id);
    }

    public List<Product> getProductsByCategory(String category) {
        logger.info("Fetching products by category: {}", category);
        
        // Simulate timeout for specific categories
        if ("timeout".equalsIgnoreCase(category)) {
            logger.error("Simulating timeout for category: {}", category);
            simulateTimeout();
        }
        
        simulateRandomDelay();
        return productRepository.findByCategory(category);
    }

    public List<Product> searchProducts(String keyword) {
        logger.info("Searching products with keyword: {}", keyword);
        
        if (keyword == null || keyword.trim().isEmpty()) {
            throw new CustomExceptions.ValidationException("Search keyword cannot be empty");
        }
        
        simulateRandomDelay();
        return productRepository.searchProducts(keyword);
    }

    public List<Product> getProductsByPriceRange(BigDecimal minPrice, BigDecimal maxPrice) {
        logger.info("Fetching products in price range: {} - {}", minPrice, maxPrice);
        
        if (minPrice.compareTo(maxPrice) > 0) {
            throw new CustomExceptions.InvalidPriceException("Minimum price cannot be greater than maximum price");
        }
        
        simulateRandomDelay();
        return productRepository.findByPriceBetween(minPrice, maxPrice);
    }

    public List<Product> getLowStockProducts(Integer threshold) {
        logger.info("Fetching low stock products with threshold: {}", threshold);
        simulateRandomDelay();
        return productRepository.findByStockQuantityLessThan(threshold);
    }

    public CompletableFuture<Product> createProductAsync(Product product) {
        logger.info("Creating product asynchronously: {}", product.getName());
        return CompletableFuture.supplyAsync(() -> createProduct(product));
    }

    public void updateStock(Long productId, Integer quantity) {
        logger.info("Updating stock for product: {} with quantity: {}", productId, quantity);
        
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new CustomExceptions.ProductNotFoundException("Product not found with id: " + productId));
        
        int newStock = product.getStockQuantity() + quantity;
        if (newStock < 0) {
            logger.error("Insufficient stock for product: {}. Current: {}, Requested: {}", 
                    productId, product.getStockQuantity(), Math.abs(quantity));
            throw new CustomExceptions.InsufficientStockException("Insufficient stock available");
        }
        
        product.setStockQuantity(newStock);
        productRepository.save(product);
        logger.info("Stock updated successfully for product: {}", productId);
    }

    public Long getProductCountByCategory(String category) {
        logger.info("Getting product count for category: {}", category);
        simulateRandomDelay();
        return productRepository.countByCategory(category);
    }

    // Simulate external service call with potential failure
    public String getProductRecommendations(Long productId) {
        logger.info("Fetching recommendations for product: {}", productId);
        
        // Simulate external service failure
        if (ThreadLocalRandom.current().nextInt(100) < 20) { // 20% failure rate
            logger.error("External recommendation service failed for product: {}", productId);
            throw new CustomExceptions.ExternalServiceException("Recommendation service is currently unavailable");
        }
        
        simulateRandomDelay();
        return "Recommended products: [1, 2, 3]";
    }

    private void validateProduct(Product product) {
        if (product.getName() == null || product.getName().trim().isEmpty()) {
            throw new CustomExceptions.ValidationException("Product name is required");
        }
        if (product.getPrice() == null || product.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new CustomExceptions.InvalidPriceException("Product price must be greater than 0");
        }
        if (product.getStockQuantity() == null || product.getStockQuantity() < 0) {
            throw new CustomExceptions.ValidationException("Stock quantity cannot be negative");
        }
    }

    private void simulateRandomDelay() {
        try {
            // Random delay between 10-100ms to simulate real database operations
            Thread.sleep(ThreadLocalRandom.current().nextInt(10, 101));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void simulateTimeout() {
        try {
            // Simulate a long operation that times out
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new CustomExceptions.TimeoutException("Operation timed out");
        }
        throw new CustomExceptions.TimeoutException("Operation timed out");
    }
}
