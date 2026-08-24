package com.example.productmanager.dto.product;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.example.productmanager.entity.ProductCategory;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ProductFormDTO {

	private Long id;

	@NotBlank(message = "{err.product.nameRequired}")
	@Size(max = 120, message = "{err.product.nameTooLong}")
	private String name;

	@NotNull(message = "{err.product.categoryRequired}")
	private ProductCategory category;

	@NotNull(message = "{err.product.pricePositive}")
	@DecimalMin(value = "0.0", inclusive = false, message = "{err.product.pricePositive}")
	private BigDecimal price;

	@NotNull(message = "{err.product.quantityInvalid}")
	@Min(value = 0, message = "{err.product.quantityInvalid}")
	private Integer quantity;

	@Size(max = 30, message = "{err.product.unitTooLong}")
	private String unit;

	private LocalDate expiryDate;
}