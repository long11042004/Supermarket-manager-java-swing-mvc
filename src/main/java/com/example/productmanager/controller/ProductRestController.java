package com.example.productmanager.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
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

import com.example.productmanager.model.Product;
import com.example.productmanager.service.ProductService;

import jakarta.validation.Valid;
import lombok.AllArgsConstructor;

@RestController
@RequestMapping("/api/products")
@AllArgsConstructor
public class ProductRestController {

	private final ProductService productService;

	@GetMapping
	public List<Product> getAllProducts() {
		return productService.getAllProducts();
	}

	@GetMapping("/{id}")
	public Product getProductById(@PathVariable Long id) {
		return productService.getProductById(id);
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public Product createProduct(@Valid @RequestBody Product product) {
		return productService.createProduct(product);
	}

	@PutMapping("/{id}")
	public Product updateProduct(@PathVariable Long id, @Valid @RequestBody Product product) {
		return productService.updateProduct(id, product);
	}

	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void deleteProduct(@PathVariable Long id) {
		productService.deleteProduct(id);
	}

	@GetMapping("/search")
	public List<Product> searchByName(@RequestParam String keyword) {
		return productService.searchByName(keyword);
	}

	@GetMapping("/category")
	public List<Product> filterByCategory(@RequestParam String value) {
		return productService.filterByCategory(value);
	}

	@GetMapping("/low-stock")
	public List<Product> getLowStock(@RequestParam(defaultValue = "10") Integer threshold) {
		return productService.getLowStockProducts(threshold);
	}
}
