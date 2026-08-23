package com.example.productmanager.dto.product;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ProductResponseDTO(
		Long id,
		String name,
		String category,
		BigDecimal price,
		Integer quantity,
		String unit,
		LocalDate expiryDate) {
}
