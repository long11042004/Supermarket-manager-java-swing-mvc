package com.example.productmanager.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.example.productmanager.dto.product.ProductFormDTO;
import com.example.productmanager.dto.product.ProductRequestDTO;
import com.example.productmanager.dto.product.ProductResponseDTO;
import com.example.productmanager.entity.Product;

@Mapper(componentModel = "spring")
public interface ProductMapper {

	@Mapping(target = "id", ignore = true)
	@Mapping(target = "version", ignore = true)
	Product toEntity(ProductRequestDTO request);

	@Mapping(target = "version", ignore = true)
	Product toEntity(ProductFormDTO request);

	ProductResponseDTO toResponse(Product product);

	ProductFormDTO toForm(Product product);
}
