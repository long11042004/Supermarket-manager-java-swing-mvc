package com.example.productmanager.service;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.example.productmanager.model.Product;
import com.example.productmanager.repository.ProductRepository;

import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class ProductService {

	private final ProductRepository productRepository;

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
				.sorted(Comparator.comparing(product -> product.getName() == null ? "" : product.getName(), String.CASE_INSENSITIVE_ORDER))
				.collect(Collectors.toList());
	}

	public List<String> getAllCategories() {
		return productRepository.findAll().stream()
				.map(product -> product == null ? null : product.getCategory())
				.filter(category -> category != null && !category.isBlank())
				.distinct()
				.sorted(String.CASE_INSENSITIVE_ORDER)
				.collect(Collectors.toList());
	}

	public Product getProductById(Long id) {
		return productRepository.findById(id)
				.orElseThrow(() -> new RuntimeException(new StringBuilder("Khong tim thay san pham voi id: ")
						.append(id)
						.toString()));
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

	public List<Product> getExpiringSoonProducts(int daysAhead) {
		LocalDate maxDate = LocalDate.now().plusDays(daysAhead);
		return productRepository.findExpiringSoon(maxDate);
	}
}