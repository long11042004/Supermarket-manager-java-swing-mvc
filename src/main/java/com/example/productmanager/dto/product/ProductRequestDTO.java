package com.example.productmanager.dto.product;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ProductRequestDTO(
		@NotBlank(message = "Ten san pham khong duoc de trong")
		@Size(max = 120, message = "Ten san pham toi da 120 ky tu")
		String name,

		@NotBlank(message = "Danh muc khong duoc de trong")
		@Size(max = 80, message = "Danh muc toi da 80 ky tu")
		String category,

		@NotNull(message = "Gia khong duoc de trong")
		@DecimalMin(value = "0.0", inclusive = false, message = "Gia phai lon hon 0")
		BigDecimal price,

		@NotNull(message = "So luong khong duoc de trong")
		@Min(value = 0, message = "So luong khong duoc am")
		Integer quantity,

		@Size(max = 30, message = "Don vi toi da 30 ky tu")
		String unit,

		LocalDate expiryDate) {
}
