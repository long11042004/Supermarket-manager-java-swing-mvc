package com.example.productmanager.modelmapper;

import com.example.productmanager.dto.product.ProductRequestDTO;
import com.example.productmanager.dto.product.ProductResponseDTO;
import com.example.productmanager.dto.product.ProductFormDTO;
import com.example.productmanager.model.Product;

import lombok.NoArgsConstructor;

@NoArgsConstructor
public final class ProductMapper {

	public static Product toEntity(ProductRequestDTO request) {
		if (request == null) {
			return null;
		}
		return Product.builder()
				.name(request.name())
				.category(request.category())
				.price(request.price())
				.quantity(request.quantity())
				.unit(request.unit())
				.expiryDate(request.expiryDate())
				.build();
	}

	public static Product toEntity(ProductFormDTO request) {
		if (request == null) {
			return null;
		}
		return Product.builder()
				.id(request.getId())
				.name(request.getName())
				.category(request.getCategory())
				.price(request.getPrice())
				.quantity(request.getQuantity())
				.unit(request.getUnit())
				.expiryDate(request.getExpiryDate())
				.build();
	}

	public static ProductResponseDTO toResponse(Product product) {
		if (product == null) {
			return null;
		}
		return new ProductResponseDTO(
				product.getId(),
				product.getName(),
				product.getCategory(),
				product.getPrice(),
				product.getQuantity(),
				product.getUnit(),
				product.getExpiryDate());
	}

	public static ProductFormDTO toForm(Product product) {
		if (product == null) {
			return null;
		}
		ProductFormDTO form = new ProductFormDTO();
		form.setId(product.getId());
		form.setName(product.getName());
		form.setCategory(product.getCategory());
		form.setPrice(product.getPrice());
		form.setQuantity(product.getQuantity());
		form.setUnit(product.getUnit());
		form.setExpiryDate(product.getExpiryDate());
		return form;
	}
}
