package com.example.productmanager.dto.product;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.example.productmanager.model.ProductCategory;

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

	@NotBlank(message = "Ten san pham khong duoc de trong")
	@Size(max = 120, message = "Ten san pham toi da 120 ky tu")
	private String name;

	@NotNull(message = "Danh muc khong duoc de trong")
	private ProductCategory category;

	@NotNull(message = "Gia khong duoc de trong")
	@DecimalMin(value = "0.0", inclusive = false, message = "Gia phai lon hon 0")
	private BigDecimal price;

	@NotNull(message = "So luong khong duoc de trong")
	@Min(value = 0, message = "So luong khong duoc am")
	private Integer quantity;

	@Size(max = 30, message = "Don vi toi da 30 ky tu")
	private String unit;

	private LocalDate expiryDate;
}