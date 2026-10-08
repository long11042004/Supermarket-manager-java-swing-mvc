package com.example.productmanager.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "promotions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Promotion {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Version
	private Long version;

	@Column(nullable = false, length = 100)
	private String name;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private PromotionKind kind;

	@Column(unique = true, length = 40)
	private String code;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private DiscountType discountType;

	@Column(nullable = false, precision = 12, scale = 2)
	private BigDecimal discountValue;

	@Column(nullable = false, precision = 12, scale = 2)
	private BigDecimal minimumOrderAmount;

	@Column(precision = 12, scale = 2)
	private BigDecimal maximumDiscount;

	@Column(nullable = false)
	private LocalDateTime startsAt;

	@Column(nullable = false)
	private LocalDateTime endsAt;

	private Integer maximumRedemptions;

	@Column(nullable = false)
	private Integer redemptionCount;

	@Column(nullable = false)
	private boolean active;

	@Column(nullable = false, updatable = false)
	private LocalDateTime createdAt;

	@PrePersist
	protected void onCreate() {
		if (createdAt == null) {
			createdAt = LocalDateTime.now();
		}
		if (minimumOrderAmount == null) {
			minimumOrderAmount = BigDecimal.ZERO;
		}
		if (redemptionCount == null) {
			redemptionCount = 0;
		}
	}
}
