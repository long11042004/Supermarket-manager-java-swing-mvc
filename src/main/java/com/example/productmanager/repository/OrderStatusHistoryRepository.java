package com.example.productmanager.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.productmanager.entity.OrderStatusHistory;

public interface OrderStatusHistoryRepository extends JpaRepository<OrderStatusHistory, Long> {

	@Query("""
			SELECT history
			FROM OrderStatusHistory history
			WHERE history.order.id IN :orderIds
			ORDER BY history.changedAt DESC, history.id DESC
			""")
	List<OrderStatusHistory> findForOrders(@Param("orderIds") List<Long> orderIds);
}
