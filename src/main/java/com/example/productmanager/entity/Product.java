package com.example.productmanager.entity;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.springframework.context.i18n.LocaleContextHolder;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.persistence.Version;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "products")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Version
	private Long version;

	@NotBlank(message = "{err.product.nameRequired}")
	@Size(max = 120, message = "{err.product.nameTooLong}")
	@Column(length = 120)
	private String nameVi;

	@Column(length = 120)
	private String nameEn;

	@NotNull(message = "{err.product.categoryRequired}")
	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 40)
	private ProductCategory category;

	@NotNull(message = "{err.product.pricePositive}")
	@DecimalMin(value = "0.0", inclusive = false, message = "{err.product.pricePositive}")
	@Column(nullable = false, precision = 12, scale = 2)
	private BigDecimal price;

	@NotNull(message = "{err.product.quantityInvalid}")
	@Min(value = 0, message = "{err.product.quantityInvalid}")
	@Column(nullable = false)
	private Integer quantity;

	@Size(max = 30, message = "{err.product.unitTooLong}")
	@Column(length = 30)
	private String unitVi;

	@Column(length = 30)
	private String unitEn;

	private LocalDate expiryDate;

	@Transient
	public String getDisplayName() {
		String locale = LocaleContextHolder.getLocale() != null ? LocaleContextHolder.getLocale().getLanguage() : "vi";
		if ("en".equalsIgnoreCase(locale)) {
			if (nameEn != null && !nameEn.isBlank()) {
				return nameEn;
			}
			if (nameVi != null && !nameVi.isBlank()) {
				return nameVi;
			}
			return nameEn != null ? nameEn : nameVi;
		}
		if (nameVi != null && !nameVi.isBlank()) {
			return nameVi;
		}
		return nameEn != null ? nameEn : nameVi;
	}

	@Transient
	public String getDisplayUnit() {
		String locale = LocaleContextHolder.getLocale() != null ? LocaleContextHolder.getLocale().getLanguage() : "vi";
		if ("en".equalsIgnoreCase(locale)) {
			if (unitEn != null && !unitEn.isBlank()) {
				return unitEn;
			}
			if (unitVi != null && !unitVi.isBlank()) {
				return unitVi;
			}
			return unitEn != null ? unitEn : unitVi;
		}
		if (unitVi != null && !unitVi.isBlank()) {
			return unitVi;
		}
		return unitEn != null ? unitEn : unitVi;
	}
}