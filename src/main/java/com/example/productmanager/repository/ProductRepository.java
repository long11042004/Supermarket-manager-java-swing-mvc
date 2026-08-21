package com.example.productmanager.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.productmanager.model.Product;

public interface ProductRepository extends JpaRepository<Product, Long> {

	@Query("""
			SELECT p FROM Product p
			WHERE (:keyword = '' OR LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')))
			  AND (:category = '' OR LOWER(p.category) = LOWER(:category))
			ORDER BY LOWER(p.name) ASC
			""")
	Page<Product> findByKeywordAndCategory(
			@Param("keyword") String keyword,
			@Param("category") String category,
			Pageable pageable);

	@Query("SELECT DISTINCT p.category FROM Product p WHERE p.category IS NOT NULL AND TRIM(p.category) <> '' ORDER BY LOWER(p.category) ASC")
	List<String> findDistinctCategories();

	@Query("""
			SELECT p FROM Product p
			WHERE (:keyword = '' OR LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')))
			ORDER BY LOWER(p.name) ASC
			""")
	List<Product> getProducts(@Param("keyword") String keyword);

	@Query("SELECT p FROM Product p WHERE LOWER(p.category) LIKE LOWER(CONCAT('%', :category, '%'))")
	List<Product> filterByCategory(@Param("category") String category);

	@Query("SELECT p FROM Product p WHERE p.quantity < :threshold ORDER BY p.quantity ASC")
	List<Product> findLowStock(@Param("threshold") Integer threshold);

	@Query("SELECT p FROM Product p WHERE p.expiryDate IS NOT NULL AND p.expiryDate <= :maxDate ORDER BY p.expiryDate ASC")
	List<Product> findExpiringSoon(@Param("maxDate") LocalDate maxDate);
}