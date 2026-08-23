package com.example.productmanager.service;

import java.time.LocalDate;
import java.util.List;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import com.example.productmanager.exception.ConflictException;
import com.example.productmanager.exception.NotFoundException;
import com.example.productmanager.model.Product;
import com.example.productmanager.model.ProductCategory;
import com.example.productmanager.repository.CustomerOrderRepository;
import com.example.productmanager.repository.ProductRepository;

import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class ProductService {

	private final ProductRepository productRepository;
	private final CustomerOrderRepository customerOrderRepository;

	public List<Product> getProducts(String keyword) {
		String normalizedKeyword = keyword == null ? "" : keyword.trim();
		return productRepository.getProducts(normalizedKeyword);
	}

	public List<Product> getFeaturedProducts(int limit) {
		int normalizedLimit = Math.max(1, Math.min(limit, 10));
		return productRepository.findTopFeaturedProducts(normalizedLimit);
	}

	public Page<Product> getFilteredProducts(String keyword, String category, int page, int size) {
		String normalizedKeyword = keyword == null ? "" : keyword.trim();
		ProductCategory normalizedCategory = parseCategoryInput(category);
		int normalizedPage = Math.max(page, 0);
		int normalizedSize = (size == 8 || size == 12 || size == 20) ? size : 12;
		return productRepository.findByKeywordAndCategory(
				normalizedKeyword,
				normalizedCategory,
				PageRequest.of(normalizedPage, normalizedSize));
	}

	public List<ProductCategory> getAllCategories() {
		return productRepository.findDistinctCategories();
	}

	public Product getProductById(Long id) {
		return productRepository.findById(id)
				.orElseThrow(() -> new NotFoundException(new StringBuilder("Khong tim thay san pham voi id: ")
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
		if (customerOrderRepository.existsOrderItemByProductId(id)) {
			throw new ConflictException("Khong the xoa san pham nay vi da phat sinh du lieu lien quan (don hang).");
		}
		Product existing = getProductById(id);
		try {
			productRepository.delete(existing);
			productRepository.flush();
		} catch (DataIntegrityViolationException ex) {
			throw new ConflictException("Khong the xoa san pham nay vi da phat sinh du lieu lien quan (don hang).", ex);
		}
	}

	public List<Product> filterByCategory(String category) {
		ProductCategory normalizedCategory = parseCategoryInput(category);
		return productRepository.filterByCategory(normalizedCategory);
	}

	private ProductCategory parseCategoryInput(String category) {
		if (category == null || category.isBlank()) {
			return null;
		}
		return ProductCategory.fromValue(category);
	}

	public List<Product> getLowStockProducts(Integer threshold) {
		return productRepository.findLowStock(threshold);
	}

	public List<Product> getExpiringSoonProducts(int daysAhead) {
		LocalDate maxDate = LocalDate.now().plusDays(daysAhead);
		return productRepository.findExpiringSoon(maxDate);
	}
}