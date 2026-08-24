package com.example.productmanager.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.productmanager.entity.Product;
import com.example.productmanager.entity.ProductCategory;

public interface ProductRepository extends JpaRepository<Product, Long> {

	@Query("""
			SELECT p FROM Product p
			WHERE (:keyword = '' OR LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')))
			  AND (:category IS NULL OR p.category = :category)
			ORDER BY LOWER(p.name) ASC
			""")
	Page<Product> findByKeywordAndCategory(
			@Param("keyword") String keyword,
			@Param("category") ProductCategory category,
			Pageable pageable);

	@Query("SELECT DISTINCT p.category FROM Product p WHERE p.category IS NOT NULL ORDER BY p.category ASC")
	List<ProductCategory> findDistinctCategories();

	@Query("""
			SELECT p FROM Product p
			WHERE (:keyword = '' OR LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')))
			ORDER BY LOWER(p.name) ASC
			""")
	List<Product> getProducts(@Param("keyword") String keyword);

	@Query("SELECT p FROM Product p WHERE (:category IS NULL OR p.category = :category)")
	List<Product> filterByCategory(@Param("category") ProductCategory category);

	@Query(value = """
			SELECT *
			FROM products p
			ORDER BY p.quantity DESC, p.price ASC
			LIMIT :limit
			""", nativeQuery = true)
	List<Product> findTopFeaturedProducts(@Param("limit") int limit);

	@Query(value = """
			SELECT * FROM products p
			WHERE p.quantity < :threshold
			ORDER BY p.quantity ASC
			""", nativeQuery = true)
	List<Product> findLowStock(@Param("threshold") Integer threshold);

	@Query("SELECT p FROM Product p WHERE p.expiryDate IS NOT NULL AND p.expiryDate <= :maxDate ORDER BY p.expiryDate ASC")
	List<Product> findExpiringSoon(@Param("maxDate") LocalDate maxDate);
}