package com.example.productmanager.restcontroller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.example.productmanager.dto.product.ProductRequestDTO;
import com.example.productmanager.dto.product.ProductResponseDTO;
import com.example.productmanager.model.Product;
import com.example.productmanager.modelmapper.ProductMapper;
import com.example.productmanager.service.ProductService;

import jakarta.validation.Valid;
import lombok.AllArgsConstructor;

@RestController
@RequestMapping("/api/products")
@AllArgsConstructor
@PreAuthorize("hasAnyRole('STAFF', 'MANAGER', 'ADMIN')")
public class ProductRestController {

	private final ProductService productService;

	@GetMapping("/{id}")
	public ProductResponseDTO getProductById(@PathVariable Long id) {
		Product product = productService.getProductById(id);
		return ProductMapper.toResponse(product);
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
	public ProductResponseDTO createProduct(@Valid @RequestBody ProductRequestDTO request) {
		Product created = productService.createProduct(ProductMapper.toEntity(request));
		return ProductMapper.toResponse(created);
	}

	@PutMapping("/{id}")
	@PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
	public ProductResponseDTO updateProduct(@PathVariable Long id, @Valid @RequestBody ProductRequestDTO request) {
		Product updated = productService.updateProduct(id, ProductMapper.toEntity(request));
		return ProductMapper.toResponse(updated);
	}

	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
	public void deleteProduct(@PathVariable Long id) {
		productService.deleteProduct(id);
	}

	@GetMapping("/category")
	public List<ProductResponseDTO> filterByCategory(@RequestParam String value) {
		return productService.filterByCategory(value)
				.stream()
				.map(ProductMapper::toResponse)
				.toList();
	}

	@GetMapping("/low-stock")
	public List<ProductResponseDTO> getLowStock(@RequestParam(defaultValue = "10") Integer threshold) {
		return productService.getLowStockProducts(threshold)
				.stream()
				.map(ProductMapper::toResponse)
				.toList();
	}
}
