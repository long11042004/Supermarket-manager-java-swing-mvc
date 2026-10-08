package com.example.productmanager.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.productmanager.entity.Promotion;
import com.example.productmanager.entity.PromotionKind;

public interface PromotionRepository extends JpaRepository<Promotion, Long> {

	Optional<Promotion> findByCodeIgnoreCase(String code);

	boolean existsByCodeIgnoreCase(String code);

	List<Promotion> findByKindAndActiveTrueAndStartsAtLessThanEqualAndEndsAtGreaterThanEqual(
			PromotionKind kind, LocalDateTime startsAt, LocalDateTime endsAt);

	List<Promotion> findAllByOrderByCreatedAtDesc();
}
