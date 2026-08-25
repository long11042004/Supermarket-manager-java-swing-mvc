package com.example.productmanager.dto.product;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.example.productmanager.entity.ProductCategory;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ProductRequestDTO(
		@NotBlank(message = "{err.product.nameRequired}")
		@Size(max = 120, message = "{err.product.nameTooLong}")
		String nameVi,

		@Size(max = 120, message = "{err.product.nameTooLong}")
		String nameEn,

		@NotNull(message = "{err.product.categoryRequired}")
		ProductCategory category,

		@NotNull(message = "{err.product.pricePositive}")
		@DecimalMin(value = "0.0", inclusive = false, message = "{err.product.pricePositive}")
		BigDecimal price,

		@NotNull(message = "{err.product.quantityInvalid}")
		@Min(value = 0, message = "{err.product.quantityInvalid}")
		Integer quantity,

		@Size(max = 30, message = "{err.product.unitTooLong}")
		String unitVi,

		@Size(max = 30, message = "{err.product.unitTooLong}")
		String unitEn,

		LocalDate expiryDate) {
}
