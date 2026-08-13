package com.example.productmanager.service;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.example.productmanager.model.Product;
import com.example.productmanager.repository.ProductRepository;

@Service
public class ProductService {

	private final ProductRepository productRepository;

	public ProductService(ProductRepository productRepository) {
		this.productRepository = productRepository;
	}

	public List<Product> getAllProducts() {
		return productRepository.findAll();
	}

	public List<Product> getFilteredProducts(String keyword, String category) {
		String normalizedKeyword = keyword == null ? "" : keyword.trim();
		String normalizedCategory = category == null ? "" : category.trim();

		return productRepository.findAll().stream()
				.filter(product -> {
					boolean matchKeyword = normalizedKeyword.isEmpty()
							|| product.getName() != null && product.getName().toLowerCase().contains(normalizedKeyword.toLowerCase());
					boolean matchCategory = normalizedCategory.isEmpty()
							|| product.getCategory() != null && product.getCategory().equalsIgnoreCase(normalizedCategory);
					return matchKeyword && matchCategory;
				})
				.sorted(Comparator.comparing(Product::getName, String.CASE_INSENSITIVE_ORDER))
				.collect(Collectors.toList());
	}

	public List<String> getAllCategories() {
		return productRepository.findAll().stream()
				.map(Product::getCategory)
				.filter(category -> category != null && !category.isBlank())
				.distinct()
				.sorted(String.CASE_INSENSITIVE_ORDER)
				.collect(Collectors.toList());
	}

	public Product getProductById(Long id) {
		return productRepository.findById(id)
				.orElseThrow(() -> new RuntimeException("Khong tim thay san pham voi id: " + id));
	}

	public Product createProduct(Product product) {
		return productRepository.save(product);
	}

	public Product updateProduct(Long id, Product request) {
		Product existing = getProductById(id);
		existing.setName(request.getName());
		existing.setCategory(request.getCategory());
		existing.setPrice(request.getPrice());
		existing.setQuantity(request.getQuantity());
		existing.setUnit(request.getUnit());
		existing.setExpiryDate(request.getExpiryDate());
		return productRepository.save(existing);
	}

	public void deleteProduct(Long id) {
		Product existing = getProductById(id);
		productRepository.delete(existing);
	}

	public List<Product> searchByName(String keyword) {
		return productRepository.searchByName(keyword);
	}

	public List<Product> filterByCategory(String category) {
		return productRepository.filterByCategory(category);
	}

	public List<Product> getLowStockProducts(Integer threshold) {
		return productRepository.findLowStock(threshold);
	}
}