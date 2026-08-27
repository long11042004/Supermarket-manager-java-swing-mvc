package com.example.productmanager.repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.productmanager.entity.CustomerOrder;
import com.example.productmanager.entity.CustomerOrderItem;
import com.example.productmanager.entity.OrderStatus;

public interface CustomerOrderRepository extends JpaRepository<CustomerOrder, Long> {

	@Query("""
			SELECT DISTINCT o
			FROM CustomerOrder o
			LEFT JOIN FETCH o.items i
			LEFT JOIN FETCH i.product p
			WHERE p.id = :productId
			""")
	List<CustomerOrder> findOrdersByProductId(@Param("productId") Long productId);

	@Query("""
			SELECT i
			FROM CustomerOrderItem i
			LEFT JOIN FETCH i.order o
			LEFT JOIN FETCH i.product p
			WHERE p.id = :productId
			""")
	List<CustomerOrderItem> findItemsByProductId(@Param("productId") Long productId);

	@Query("SELECT DISTINCT o FROM CustomerOrder o LEFT JOIN FETCH o.items i LEFT JOIN FETCH i.product WHERE o.user.id = :userId ORDER BY o.createdAt DESC")
	List<CustomerOrder> findByUserIdOrderByCreatedAtDesc(@Param("userId") Long userId);

	@Query("SELECT o FROM CustomerOrder o LEFT JOIN FETCH o.items i LEFT JOIN FETCH i.product WHERE o.id = :orderId AND o.user.id = :userId")
	Optional<CustomerOrder> findDetailByIdAndUserId(@Param("orderId") Long orderId, @Param("userId") Long userId);

	@Modifying
	@Query("DELETE FROM CustomerOrderItem i WHERE i.product.id = :productId")
	void deleteOrderItemsByProductId(@Param("productId") Long productId);

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

	@Query(value = """
			SELECT COALESCE(p.name_vi, p.name_en) AS productName,
			       COALESCE(p.unit_vi, p.unit_en) AS unit,
			       SUM(coi.quantity) AS totalQuantity,
			       SUM(coi.line_total) AS totalRevenue
			FROM customer_order_items coi
			JOIN products p ON coi.product_id = p.id
			JOIN customer_orders co ON coi.order_id = co.id
			WHERE co.created_at BETWEEN :startTime AND :endTime
			  AND (:status IS NULL OR co.status = :status)
			GROUP BY p.id, p.name_vi, p.name_en, p.unit_vi, p.unit_en
			ORDER BY SUM(coi.line_total) DESC
			""", nativeQuery = true)
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