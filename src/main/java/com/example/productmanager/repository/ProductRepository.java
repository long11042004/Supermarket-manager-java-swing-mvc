package com.example.productmanager.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.productmanager.model.Product;

public interface ProductRepository extends JpaRepository<Product, Long> {

	@Query("SELECT p FROM Product p WHERE LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%'))")
	List<Product> searchByName(@Param("keyword") String keyword);

	@Query("SELECT p FROM Product p WHERE LOWER(p.category) LIKE LOWER(CONCAT('%', :category, '%'))")
	List<Product> filterByCategory(@Param("category") String category);

	@Query("SELECT p FROM Product p WHERE p.quantity < :threshold ORDER BY p.quantity ASC")
	List<Product> findLowStock(@Param("threshold") Integer threshold);

	@Query("SELECT p FROM Product p WHERE p.expiryDate IS NOT NULL AND p.expiryDate <= :maxDate ORDER BY p.expiryDate ASC")
	List<Product> findExpiringSoon(@Param("maxDate") LocalDate maxDate);
}