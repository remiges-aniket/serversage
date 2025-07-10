package tech.remiges.serversage.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tech.remiges.serversage.model.Order;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    
    List<Order> findByUserId(Long userId);
    
    List<Order> findByProductId(Long productId);
    
    List<Order> findByStatus(Order.OrderStatus status);
    
    List<Order> findByUserIdAndStatus(Long userId, Order.OrderStatus status);
    
    @Query("SELECT o FROM Order o WHERE o.createdAt BETWEEN :startDate AND :endDate")
    List<Order> findOrdersBetweenDates(@Param("startDate") LocalDateTime startDate, 
                                      @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT SUM(o.totalAmount) FROM Order o WHERE o.userId = :userId")
    BigDecimal getTotalAmountByUser(@Param("userId") Long userId);
    
    @Query("SELECT COUNT(o) FROM Order o WHERE o.status = :status")
    Long countByStatus(@Param("status") Order.OrderStatus status);
    
    @Query("SELECT o FROM Order o WHERE o.totalAmount > :amount")
    List<Order> findHighValueOrders(@Param("amount") BigDecimal amount);
}
