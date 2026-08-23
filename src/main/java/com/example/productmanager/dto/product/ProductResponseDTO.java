package com.example.productmanager.dto.product;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.example.productmanager.model.ProductCategory;

public record ProductResponseDTO(
		Long id,
		String name,
		ProductCategory category,
		BigDecimal price,
		Integer quantity,
		String unit,
		LocalDate expiryDate) {
}
