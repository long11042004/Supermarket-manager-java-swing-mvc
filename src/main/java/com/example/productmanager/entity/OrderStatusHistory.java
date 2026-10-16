package com.example.productmanager.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "order_status_history", indexes = {
		@Index(name = "idx_order_status_history_order_changed", columnList = "order_id, changed_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderStatusHistory {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "order_id", nullable = false)
	private CustomerOrder order;

	@Enumerated(EnumType.STRING)
	@Column(name = "previous_status", nullable = false, length = 20)
	private OrderStatus previousStatus;

	@Enumerated(EnumType.STRING)
	@Column(name = "new_status", nullable = false, length = 20)
	private OrderStatus newStatus;

	@Column(name = "changed_by", nullable = false, length = 150)
	private String changedBy;

	@Column(name = "changed_at", nullable = false, updatable = false)
	private LocalDateTime changedAt;

	@PrePersist
	protected void onCreate() {
		if (changedAt == null) {
			changedAt = LocalDateTime.now();
		}
	}
}
