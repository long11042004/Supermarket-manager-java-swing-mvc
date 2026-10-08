package com.example.productmanager.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.productmanager.dto.promotion.PromotionFormDTO;
import com.example.productmanager.entity.DiscountType;
import com.example.productmanager.entity.Promotion;
import com.example.productmanager.entity.PromotionKind;
import com.example.productmanager.multilanguage.MessageResolver;
import com.example.productmanager.repository.PromotionRepository;

import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class PromotionService {

	private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");

	private final PromotionRepository promotionRepository;
	private final MessageResolver messageResolver;

	@Transactional(readOnly = true)
	public PromotionQuote quote(BigDecimal subtotal, String couponCode) {
		BigDecimal normalizedSubtotal = subtotal == null ? BigDecimal.ZERO : subtotal.max(BigDecimal.ZERO);
		LocalDateTime now = LocalDateTime.now();
		Promotion coupon = null;
		BigDecimal couponDiscount = BigDecimal.ZERO;

		if (couponCode != null && !couponCode.isBlank()) {
			String normalizedCode = normalizeCode(couponCode);
			coupon = promotionRepository.findByCodeIgnoreCase(normalizedCode)
					.filter(promotion -> promotion.getKind() == PromotionKind.COUPON)
					.filter(promotion -> isAvailable(promotion, now))
					.orElseThrow(() -> new IllegalArgumentException(messageResolver.msg("err.promotion.couponUnavailable")));
			if (normalizedSubtotal.compareTo(coupon.getMinimumOrderAmount()) < 0) {
				throw new IllegalArgumentException(messageResolver.msg(
						"err.promotion.minimumOrder", coupon.getMinimumOrderAmount().toPlainString()));
			}
			couponDiscount = calculateDiscount(coupon, normalizedSubtotal);
		}

		BigDecimal remainingSubtotal = normalizedSubtotal.subtract(couponDiscount).max(BigDecimal.ZERO);
		Promotion automaticPromotion = promotionRepository
				.findByKindAndActiveTrueAndStartsAtLessThanEqualAndEndsAtGreaterThanEqual(
						PromotionKind.AUTOMATIC, now, now)
				.stream()
				.filter(promotion -> isAvailable(promotion, now))
				.filter(promotion -> normalizedSubtotal.compareTo(promotion.getMinimumOrderAmount()) >= 0)
				.filter(promotion -> calculateDiscount(promotion, remainingSubtotal).signum() > 0)
				.max(Comparator.comparing(promotion -> calculateDiscount(promotion, remainingSubtotal)))
				.orElse(null);
		BigDecimal automaticDiscount = automaticPromotion == null
				? BigDecimal.ZERO
				: calculateDiscount(automaticPromotion, remainingSubtotal);

		BigDecimal totalDiscount = couponDiscount.add(automaticDiscount).min(normalizedSubtotal);
		return new PromotionQuote(
				coupon == null ? null : coupon.getId(),
				coupon == null ? null : coupon.getName(),
				coupon == null ? null : coupon.getCode(),
				couponDiscount,
				automaticPromotion == null ? null : automaticPromotion.getId(),
				automaticPromotion == null ? null : automaticPromotion.getName(),
				automaticDiscount,
				totalDiscount,
				normalizedSubtotal.subtract(totalDiscount));
	}

	@Transactional
	public Promotion createPromotion(PromotionFormDTO form) {
		validate(form);
		PromotionKind kind = form.getKind();
		String code = kind == PromotionKind.COUPON ? normalizeCode(form.getCode()) : null;
		if (code != null && promotionRepository.existsByCodeIgnoreCase(code)) {
			throw new IllegalArgumentException(messageResolver.msg("err.promotion.duplicateCode"));
		}
		return promotionRepository.save(Promotion.builder()
				.name(form.getName().trim())
				.kind(kind)
				.code(code)
				.discountType(form.getDiscountType())
				.discountValue(form.getDiscountValue())
				.minimumOrderAmount(form.getMinimumOrderAmount() == null
						? BigDecimal.ZERO
						: form.getMinimumOrderAmount())
				.maximumDiscount(form.getMaximumDiscount())
				.startsAt(form.getStartsAt())
				.endsAt(form.getEndsAt())
				.maximumRedemptions(form.getMaximumRedemptions())
				.redemptionCount(0)
				.active(true)
				.build());
	}

	@Transactional(readOnly = true)
	public List<Promotion> getAllPromotions() {
		return promotionRepository.findAllByOrderByCreatedAtDesc();
	}

	@Transactional(readOnly = true)
	public List<Promotion> getFeaturedPromotions() {
		LocalDateTime now = LocalDateTime.now();
		return Stream.of(PromotionKind.COUPON, PromotionKind.AUTOMATIC)
				.flatMap(kind -> promotionRepository
						.findByKindAndActiveTrueAndStartsAtLessThanEqualAndEndsAtGreaterThanEqual(kind, now, now)
						.stream())
				.filter(promotion -> isAvailable(promotion, now))
				.sorted(Comparator.comparing(Promotion::getCreatedAt).reversed())
				.limit(6)
				.toList();
	}

	@Transactional
	public void updateActive(Long promotionId, boolean active) {
		Promotion promotion = promotionRepository.findById(promotionId)
				.orElseThrow(() -> new IllegalArgumentException(messageResolver.msg("err.promotion.notFound")));
		promotion.setActive(active);
	}

	@Transactional
	public void recordRedemptions(PromotionQuote quote) {
		incrementRedemption(quote.couponId());
		incrementRedemption(quote.automaticPromotionId());
	}

	private void incrementRedemption(Long promotionId) {
		if (promotionId == null) {
			return;
		}
		Promotion promotion = promotionRepository.findById(promotionId)
				.orElseThrow(() -> new IllegalArgumentException(messageResolver.msg("err.promotion.couponUnavailable")));
		if (!isAvailable(promotion, LocalDateTime.now())) {
			throw new IllegalArgumentException(messageResolver.msg("err.promotion.couponUnavailable"));
		}
		promotion.setRedemptionCount(promotion.getRedemptionCount() + 1);
	}

	private void validate(PromotionFormDTO form) {
		if (form == null || form.getName() == null || form.getName().isBlank() || form.getName().trim().length() > 100) {
			throw new IllegalArgumentException(messageResolver.msg("err.promotion.nameRequired"));
		}
		if (form.getKind() == null || form.getDiscountType() == null
				|| form.getDiscountValue() == null || form.getDiscountValue().signum() <= 0
				|| form.getStartsAt() == null || form.getEndsAt() == null
				|| !form.getStartsAt().isBefore(form.getEndsAt())) {
			throw new IllegalArgumentException(messageResolver.msg("err.promotion.invalidConfiguration"));
		}
		if (form.getKind() == PromotionKind.COUPON
				&& (form.getCode() == null || !normalizeCode(form.getCode()).matches("[A-Z0-9_-]{3,40}"))) {
			throw new IllegalArgumentException(messageResolver.msg("err.promotion.invalidCode"));
		}
		if (form.getKind() == PromotionKind.AUTOMATIC && form.getCode() != null && !form.getCode().isBlank()) {
			throw new IllegalArgumentException(messageResolver.msg("err.promotion.automaticCodeNotAllowed"));
		}
		if (form.getDiscountType() == DiscountType.PERCENTAGE
				&& form.getDiscountValue().compareTo(ONE_HUNDRED) > 0) {
			throw new IllegalArgumentException(messageResolver.msg("err.promotion.invalidPercentage"));
		}
		if (form.getMinimumOrderAmount() != null && form.getMinimumOrderAmount().signum() < 0) {
			throw new IllegalArgumentException(messageResolver.msg("err.promotion.invalidConfiguration"));
		}
		if (form.getMaximumDiscount() != null && form.getMaximumDiscount().signum() <= 0) {
			throw new IllegalArgumentException(messageResolver.msg("err.promotion.invalidConfiguration"));
		}
		if (form.getMaximumRedemptions() != null && form.getMaximumRedemptions() <= 0) {
			throw new IllegalArgumentException(messageResolver.msg("err.promotion.invalidConfiguration"));
		}
	}

	private boolean isAvailable(Promotion promotion, LocalDateTime now) {
		return promotion.isActive()
				&& promotion.getStartsAt() != null && !now.isBefore(promotion.getStartsAt())
				&& promotion.getEndsAt() != null && !now.isAfter(promotion.getEndsAt())
				&& (promotion.getMaximumRedemptions() == null
						|| promotion.getRedemptionCount() < promotion.getMaximumRedemptions());
	}

	private BigDecimal calculateDiscount(Promotion promotion, BigDecimal amount) {
		BigDecimal discount = promotion.getDiscountType() == DiscountType.PERCENTAGE
				? amount.multiply(promotion.getDiscountValue()).divide(ONE_HUNDRED, 2, RoundingMode.HALF_UP)
				: promotion.getDiscountValue().min(amount);
		if (promotion.getMaximumDiscount() != null) {
			discount = discount.min(promotion.getMaximumDiscount());
		}
		return discount.min(amount).max(BigDecimal.ZERO);
	}

	private String normalizeCode(String code) {
		return code.trim().toUpperCase(Locale.ROOT);
	}

	public record PromotionQuote(
			Long couponId,
			String couponName,
			String couponCode,
			BigDecimal couponDiscount,
			Long automaticPromotionId,
			String automaticPromotionName,
			BigDecimal automaticDiscount,
			BigDecimal totalDiscount,
			BigDecimal totalAmount) {
	}
}
