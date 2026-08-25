package com.example.productmanager.dto.product;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.example.productmanager.entity.ProductCategory;

public record ProductResponseDTO(
		Long id,
		String nameVi,
		String nameEn,
		ProductCategory category,
		BigDecimal price,
		Integer quantity,
		String unitVi,
		String unitEn,
		LocalDate expiryDate) {
}
