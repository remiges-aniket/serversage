package tech.remiges.serversage.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tech.remiges.serversage.model.Product;
import tech.remiges.serversage.service.ProductService;

import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/products")
@Tag(name = "Product Management", description = "APIs for managing products with comprehensive error scenarios")
public class ProductController {

    private final ProductService productService;

    @Autowired
    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    @Operation(summary = "Get all products", description = "Retrieve all products from the database")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved products"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<List<Product.ProductDTO>> getAllProducts() {
        var products = productService.getAllProducts()
                .stream()
                .map(Product.ProductDTO::fromEntity)
                .toList();
        return ResponseEntity.ok(products);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get product by ID", description = "Retrieve a specific product by its ID. Use ID 999 for ArrayIndexOutOfBounds error, 998 for NullPointer error")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Product found"),
            @ApiResponse(responseCode = "404", description = "Product not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<Product.ProductDTO> getProductById(
            @Parameter(description = "Product ID", example = "1") @PathVariable Long id) {
        return productService.getProductById(id)
                .map(Product.ProductDTO::fromEntity)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @Operation(summary = "Create new product", description = "Create a new product. Use name containing 'dberror' to simulate database error")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Product created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input"),
            @ApiResponse(responseCode = "409", description = "Product already exists"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<Product.ProductDTO> createProduct(@Valid @RequestBody Product product) {
        Product createdProduct = productService.createProduct(product);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Product.ProductDTO.fromEntity(createdProduct));
    }

    @PostMapping("/async")
    @Operation(summary = "Create product asynchronously", description = "Create a new product using async processing")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Product creation initiated"),
            @ApiResponse(responseCode = "400", description = "Invalid input"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public CompletableFuture<ResponseEntity<Product.ProductDTO>> createProductAsync(@Valid @RequestBody Product product) {
        return productService.createProductAsync(product)
                .thenApply(Product.ProductDTO::fromEntity)
                .thenApply(dto -> ResponseEntity.status(HttpStatus.CREATED).body(dto));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update product", description = "Update an existing product. Use price > 10000 to trigger business logic error")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Product updated successfully"),
            @ApiResponse(responseCode = "404", description = "Product not found"),
            @ApiResponse(responseCode = "422", description = "Business logic error"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<Product.ProductDTO> updateProduct(
            @Parameter(description = "Product ID") @PathVariable Long id,
            @Valid @RequestBody Product product) {
        return productService.updateProduct(id, product)
                .map(Product.ProductDTO::fromEntity)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete product", description = "Delete a product by ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Product deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Product not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<Void> deleteProduct(
            @Parameter(description = "Product ID") @PathVariable Long id) {
        productService.deleteProduct(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/category/{category}")
    @Operation(summary = "Get products by category", description = "Retrieve products by category. Use 'timeout' category to simulate timeout error")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Products retrieved successfully"),
            @ApiResponse(responseCode = "408", description = "Request timeout"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<List<Product.ProductDTO>> getProductsByCategory(
            @Parameter(description = "Product category", example = "electronics") @PathVariable String category) {
        var products = productService.getProductsByCategory(category)
                .stream()
                .map(Product.ProductDTO::fromEntity)
                .toList();
        return ResponseEntity.ok(products);
    }

    @GetMapping("/search")
    @Operation(summary = "Search products", description = "Search products by keyword in name or description")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Search completed successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid search keyword"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<List<Product.ProductDTO>> searchProducts(
            @Parameter(description = "Search keyword", example = "laptop") @RequestParam String keyword) {
        var products = productService.searchProducts(keyword)
                .stream()
                .map(Product.ProductDTO::fromEntity)
                .toList();
        return ResponseEntity.ok(products);
    }

    @GetMapping("/price-range")
    @Operation(summary = "Get products by price range", description = "Retrieve products within a specific price range")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Products retrieved successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid price range"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<List<Product.ProductDTO>> getProductsByPriceRange(
            @Parameter(description = "Minimum price", example = "10.00") @RequestParam BigDecimal minPrice,
            @Parameter(description = "Maximum price", example = "100.00") @RequestParam BigDecimal maxPrice) {
        var products = productService.getProductsByPriceRange(minPrice, maxPrice)
                .stream()
                .map(Product.ProductDTO::fromEntity)
                .toList();
        return ResponseEntity.ok(products);
    }

    @GetMapping("/low-stock")
    @Operation(summary = "Get low stock products", description = "Retrieve products with stock below threshold")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Low stock products retrieved"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<List<Product.ProductDTO>> getLowStockProducts(
            @Parameter(description = "Stock threshold", example = "10") @RequestParam(defaultValue = "10") Integer threshold) {
        var products = productService.getLowStockProducts(threshold)
                .stream()
                .map(Product.ProductDTO::fromEntity)
                .toList();
        return ResponseEntity.ok(products);
    }

    @PatchMapping("/{id}/stock")
    @Operation(summary = "Update product stock", description = "Update stock quantity for a product (positive to add, negative to reduce)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Stock updated successfully"),
            @ApiResponse(responseCode = "400", description = "Insufficient stock"),
            @ApiResponse(responseCode = "404", description = "Product not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<String> updateStock(
            @Parameter(description = "Product ID") @PathVariable Long id,
            @Parameter(description = "Quantity to add/remove", example = "5") @RequestParam Integer quantity) {
        productService.updateStock(id, quantity);
        return ResponseEntity.ok("Stock updated successfully");
    }

    @GetMapping("/category/{category}/count")
    @Operation(summary = "Get product count by category", description = "Get the number of products in a specific category")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Count retrieved successfully"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<Long> getProductCountByCategory(
            @Parameter(description = "Product category") @PathVariable String category) {
        Long count = productService.getProductCountByCategory(category);
        return ResponseEntity.ok(count);
    }

    @GetMapping("/{id}/recommendations")
    @Operation(summary = "Get product recommendations", description = "Get recommendations for a product (simulates external service with 20% failure rate)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Recommendations retrieved"),
            @ApiResponse(responseCode = "404", description = "Product not found"),
            @ApiResponse(responseCode = "503", description = "External service unavailable"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<String> getProductRecommendations(
            @Parameter(description = "Product ID") @PathVariable Long id) {
        String recommendations = productService.getProductRecommendations(id);
        return ResponseEntity.ok(recommendations);
    }
}
