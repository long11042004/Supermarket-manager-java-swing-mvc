package com.example.productmanager.repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.productmanager.model.CustomerOrder;
import com.example.productmanager.model.OrderStatus;

public interface CustomerOrderRepository extends JpaRepository<CustomerOrder, Long> {

	@Query("SELECT DISTINCT o FROM CustomerOrder o LEFT JOIN FETCH o.items i LEFT JOIN FETCH i.product WHERE o.user.id = :userId ORDER BY o.createdAt DESC")
	List<CustomerOrder> findByUserIdOrderByCreatedAtDesc(@Param("userId") Long userId);

	@Query("SELECT o FROM CustomerOrder o LEFT JOIN FETCH o.items i LEFT JOIN FETCH i.product WHERE o.id = :orderId AND o.user.id = :userId")
	Optional<CustomerOrder> findDetailByIdAndUserId(@Param("orderId") Long orderId, @Param("userId") Long userId);

	@Query("SELECT COUNT(i) > 0 FROM CustomerOrderItem i WHERE i.product.id = :productId")
	boolean existsOrderItemByProductId(@Param("productId") Long productId);

	@Query("SELECT COUNT(o) FROM CustomerOrder o WHERE o.createdAt BETWEEN :startTime AND :endTime AND (:status IS NULL OR o.status = :status)")
	long countOrdersInPeriod(@Param("startTime") LocalDateTime startTime,
			@Param("endTime") LocalDateTime endTime,
			@Param("status") OrderStatus status);

	@Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM CustomerOrder o WHERE o.createdAt BETWEEN :startTime AND :endTime AND (:status IS NULL OR o.status = :status)")
	BigDecimal sumRevenueInPeriod(@Param("startTime") LocalDateTime startTime,
			@Param("endTime") LocalDateTime endTime,
			@Param("status") OrderStatus status);

	@Query("SELECT COUNT(o) FROM CustomerOrder o WHERE o.createdAt BETWEEN :startTime AND :endTime AND o.user IS NULL AND (:status IS NULL OR o.status = :status)")
	long countGuestOrdersInPeriod(@Param("startTime") LocalDateTime startTime,
			@Param("endTime") LocalDateTime endTime,
			@Param("status") OrderStatus status);

	@Query("SELECT o.status AS status, COUNT(o) AS total FROM CustomerOrder o WHERE o.createdAt BETWEEN :startTime AND :endTime GROUP BY o.status")
	List<OrderStatusCountProjection> countOrdersByStatusInPeriod(@Param("startTime") LocalDateTime startTime,
			@Param("endTime") LocalDateTime endTime);

	@Query("""
			SELECT i.product.name AS productName,
			       i.product.unit AS unit,
			       SUM(i.quantity) AS totalQuantity,
			       SUM(i.lineTotal) AS totalRevenue
			FROM CustomerOrderItem i
			WHERE i.order.createdAt BETWEEN :startTime AND :endTime
			  AND (:status IS NULL OR i.order.status = :status)
			GROUP BY i.product.id, i.product.name, i.product.unit
			ORDER BY SUM(i.lineTotal) DESC
			""")
	List<TopProductProjection> findTopProductsInPeriod(@Param("startTime") LocalDateTime startTime,
			@Param("endTime") LocalDateTime endTime,
			@Param("status") OrderStatus status,
			Pageable pageable);

	interface OrderStatusCountProjection {
		OrderStatus getStatus();

		Long getTotal();
	}

	interface TopProductProjection {
		String getProductName();

		String getUnit();

		Long getTotalQuantity();

		BigDecimal getTotalRevenue();
	}
}