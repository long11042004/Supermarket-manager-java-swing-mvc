package com.example.productmanager.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import com.example.productmanager.entity.Product;
import com.example.productmanager.entity.ProductCategory;
import com.example.productmanager.exception.ConflictException;
import com.example.productmanager.exception.NotFoundException;
import com.example.productmanager.i18n.MessageResolver;
import com.example.productmanager.repository.CustomerOrderRepository;
import com.example.productmanager.repository.ProductRepository;

@Service
public class ProductService {

	private final ProductRepository productRepository;
	private final CustomerOrderRepository customerOrderRepository;
	private final MessageResolver messageResolver;

	@Autowired
	public ProductService(ProductRepository productRepository,
			CustomerOrderRepository customerOrderRepository,
			MessageResolver messageResolver) {
		this.productRepository = productRepository;
		this.customerOrderRepository = customerOrderRepository;
		this.messageResolver = messageResolver;
	}

	public ProductService(ProductRepository productRepository, CustomerOrderRepository customerOrderRepository) {
		this(productRepository, customerOrderRepository, null);
	}

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
				.orElseThrow(() -> new NotFoundException(msg("err.product.notFoundById", id)));
	}

	public Product createProduct(Product product) {
		Product sanitizedProduct = normalizeProduct(product);
		validateProduct(sanitizedProduct);
		return productRepository.save(sanitizedProduct);
	}

	public Product updateProduct(Long id, Product request) {
		Product existing = getProductById(id);
		Product sanitizedRequest = normalizeProduct(request);
		validateProduct(sanitizedRequest);
		existing.setName(sanitizedRequest.getName());
		existing.setCategory(sanitizedRequest.getCategory());
		existing.setPrice(sanitizedRequest.getPrice());
		existing.setQuantity(sanitizedRequest.getQuantity());
		existing.setUnit(sanitizedRequest.getUnit());
		existing.setExpiryDate(sanitizedRequest.getExpiryDate());
		return productRepository.save(existing);
	}

	public void deleteProduct(Long id) {
		if (customerOrderRepository.existsOrderItemByProductId(id)) {
			throw new ConflictException(msg("err.product.deleteConflict"));
		}
		Product existing = getProductById(id);
		try {
			productRepository.delete(existing);
			productRepository.flush();
		} catch (DataIntegrityViolationException ex) {
			throw new ConflictException(msg("err.product.deleteConflict"), ex);
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

	private Product normalizeProduct(Product product) {
		if (product == null) {
			return null;
		}
		if (product.getName() != null) {
			product.setName(product.getName().trim());
		}
		if (product.getUnit() != null) {
			String normalizedUnit = product.getUnit().trim();
			product.setUnit(normalizedUnit.isEmpty() ? null : normalizedUnit);
		}
		if (product.getExpiryDate() != null && product.getExpiryDate().isBefore(LocalDate.now().minusYears(20))) {
			throw new IllegalArgumentException(msg("err.product.expiryTooOld"));
		}
		return product;
	}

	private void validateProduct(Product product) {
		if (product == null) {
			throw new IllegalArgumentException(msg("err.product.empty"));
		}
		if (product.getName() == null || product.getName().trim().isEmpty()) {
			throw new IllegalArgumentException(msg("err.product.nameRequired"));
		}
		if (product.getName().trim().length() > 120) {
			throw new IllegalArgumentException(msg("err.product.nameTooLong"));
		}
		if (product.getCategory() == null) {
			throw new IllegalArgumentException(msg("err.product.categoryRequired"));
		}
		if (product.getPrice() == null || product.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
			throw new IllegalArgumentException(msg("err.product.pricePositive"));
		}
		if (product.getPrice().scale() > 2) {
			throw new IllegalArgumentException(msg("err.product.priceInvalidScale"));
		}
		if (product.getPrice().compareTo(new BigDecimal("1000000000000")) > 0) {
			throw new IllegalArgumentException(msg("err.product.priceTooLarge"));
		}
		if (product.getQuantity() == null || product.getQuantity() < 0) {
			throw new IllegalArgumentException(msg("err.product.quantityInvalid"));
		}
		if (product.getQuantity() > 1000000) {
			throw new IllegalArgumentException(msg("err.product.quantityTooLarge"));
		}
		if (product.getUnit() != null) {
			String unit = product.getUnit().trim();
			if (unit.isEmpty()) {
				throw new IllegalArgumentException(msg("err.product.unitBlank"));
			}
			if (unit.length() > 30) {
				throw new IllegalArgumentException(msg("err.product.unitTooLong"));
			}
			if (!unit.matches("[A-Za-zÀ-ỹ0-9/().% -]{1,30}")) {
				throw new IllegalArgumentException(msg("err.product.unitInvalid"));
			}
		}
		if (product.getExpiryDate() != null && product.getExpiryDate().isBefore(LocalDate.now())) {
			throw new IllegalArgumentException(msg("err.product.expiryPast"));
		}
	}

	private String msg(String key, Object... args) {
		if (messageResolver == null) {
			return switch (key) {
				case "err.product.empty" -> "Product data must not be empty";
				case "err.product.nameRequired" -> "Product name is required";
				case "err.product.nameTooLong" -> "Product name must not exceed 120 characters";
				case "err.product.categoryRequired" -> "Product category is required";
				case "err.product.pricePositive" -> "Product price must be greater than 0";
				case "err.product.priceInvalidScale" -> "Product price can have at most 2 decimal places";
				case "err.product.priceTooLarge" -> "Product price must not exceed 1,000,000,000,000";
				case "err.product.quantityInvalid" -> "Product quantity is invalid";
				case "err.product.quantityTooLarge" -> "Product quantity must not exceed 1,000,000";
				case "err.product.unitBlank" -> "Product unit cannot be blank";
				case "err.product.unitTooLong" -> "Product unit must not exceed 30 characters";
				case "err.product.unitInvalid" -> "Product unit contains invalid characters";
				case "err.product.expiryPast" -> "Expiry date cannot be earlier than today";
				case "err.product.expiryTooOld" -> "Product expiry date is invalid";
				case "err.product.notFoundById" -> "Product not found with id: {0}";
				case "err.product.deleteConflict" -> "This product cannot be deleted because related data already exists (orders).";
				default -> key;
			};
		}
		return messageResolver.msg(key, args);
	}

	public List<Product> getLowStockProducts(Integer threshold) {
		return productRepository.findLowStock(threshold);
	}

	public List<Product> getExpiringSoonProducts(int daysAhead) {
		LocalDate maxDate = LocalDate.now().plusDays(daysAhead);
		return productRepository.findExpiringSoon(maxDate);
	}
}