package com.example.productmanager.repository;

import java.math.BigDecimal;
import java.time.LocalDate;
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

	@Query("""
			SELECT DISTINCT o
			FROM CustomerOrder o
			LEFT JOIN FETCH o.user
			LEFT JOIN FETCH o.items i
			LEFT JOIN FETCH i.product
			WHERE (:status IS NULL OR o.status = :status)
			  AND (:startDate IS NULL OR o.createdAt >= :startDate)
			  AND (:endDateExclusive IS NULL OR o.createdAt < :endDateExclusive)
			  AND (:keyword IS NULL
			       OR LOWER(COALESCE(o.user.fullName, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))
			       OR LOWER(COALESCE(o.user.username, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))
			       OR LOWER(COALESCE(o.user.email, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))
			       OR LOWER(COALESCE(o.guestName, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))
			       OR LOWER(COALESCE(o.guestEmail, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))
			       OR LOWER(COALESCE(o.contactPhone, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))
			       OR (:orderId IS NOT NULL AND o.id = :orderId))
			ORDER BY o.createdAt DESC
			""")
	List<CustomerOrder> findForManagement(@Param("status") OrderStatus status,
			@Param("startDate") LocalDateTime startDate,
			@Param("endDateExclusive") LocalDateTime endDateExclusive,
			@Param("keyword") String keyword,
			@Param("orderId") Long orderId);

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

	@Query("SELECT o.status AS status, COUNT(o) AS total FROM CustomerOrder o WHERE o.createdAt BETWEEN :startTime AND :endTime AND (:status IS NULL OR o.status = :status) GROUP BY o.status")
	List<OrderStatusCountProjection> countOrdersByStatusInPeriod(@Param("startTime") LocalDateTime startTime,
			@Param("endTime") LocalDateTime endTime,
			@Param("status") OrderStatus status);

	@Query("""
			SELECT FUNCTION('DATE', o.createdAt) AS orderDate,
			       COUNT(o) AS orderCount,
			       COALESCE(SUM(o.totalAmount), 0) AS revenue
			FROM CustomerOrder o
			WHERE o.createdAt BETWEEN :startTime AND :endTime
			  AND (:status IS NULL OR o.status = :status)
			GROUP BY FUNCTION('DATE', o.createdAt)
			ORDER BY FUNCTION('DATE', o.createdAt)
			""")
	List<DailySalesProjection> findDailySalesInPeriod(@Param("startTime") LocalDateTime startTime,
			@Param("endTime") LocalDateTime endTime,
			@Param("status") OrderStatus status);

	@Query(value = """
			SELECT COALESCE(p.name_vi, p.name_en) AS productName,
			       COALESCE(p.unit_vi, p.unit_en) AS unit,
			       SUM(coi.quantity) AS totalQuantity,
			SUM(coi.line_total * CASE
			    WHEN co.subtotal_amount > 0 THEN
			        (co.subtotal_amount - COALESCE(co.discount_amount, 0)) / co.subtotal_amount
			    ELSE 1
			END) AS totalRevenue
			FROM customer_order_items coi
			JOIN products p ON coi.product_id = p.id
			JOIN customer_orders co ON coi.order_id = co.id
			WHERE co.created_at BETWEEN :startTime AND :endTime
			  AND (:status IS NULL OR co.status = :status)
			GROUP BY p.id, p.name_vi, p.name_en, p.unit_vi, p.unit_en
			ORDER BY SUM(coi.line_total * CASE
			           WHEN co.subtotal_amount > 0 THEN
			               (co.subtotal_amount - COALESCE(co.discount_amount, 0)) / co.subtotal_amount
			           ELSE 1
			       END) DESC
			""", nativeQuery = true)
	List<TopProductProjection> findTopProductsInPeriod(@Param("startTime") LocalDateTime startTime,
			@Param("endTime") LocalDateTime endTime,
			@Param("status") OrderStatus status,
			Pageable pageable);

	interface OrderStatusCountProjection {
		OrderStatus getStatus();

		Long getTotal();
	}

	interface DailySalesProjection {
		LocalDate getOrderDate();

		Long getOrderCount();

		BigDecimal getRevenue();
	}

	interface TopProductProjection {
		String getProductName();

		String getUnit();

		Long getTotalQuantity();

		BigDecimal getTotalRevenue();
	}
}