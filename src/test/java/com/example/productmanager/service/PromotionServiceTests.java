package com.example.productmanager.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.context.support.ResourceBundleMessageSource;

import com.example.productmanager.dto.promotion.PromotionFormDTO;
import com.example.productmanager.email.event.PromotionAnnouncedEmailEvent;
import org.springframework.context.ApplicationEventPublisher;

import com.example.productmanager.entity.DiscountType;
import com.example.productmanager.entity.Promotion;
import com.example.productmanager.entity.PromotionKind;
import com.example.productmanager.multilanguage.MessageResolver;
import com.example.productmanager.repository.PromotionRepository;

class PromotionServiceTests {

	private final PromotionRepository promotionRepository = org.mockito.Mockito.mock(PromotionRepository.class);
	private final ApplicationEventPublisher applicationEventPublisher = org.mockito.Mockito.mock(ApplicationEventPublisher.class);
	private final PromotionService promotionService = new PromotionService(
			promotionRepository,
			new MessageResolver(createMessageSource()),
			applicationEventPublisher);

	private ResourceBundleMessageSource createMessageSource() {
		LocaleContextHolder.setLocale(Locale.forLanguageTag("vi-VN"));
		ResourceBundleMessageSource source = new ResourceBundleMessageSource();
		source.setBasename("messages");
		source.setDefaultEncoding("UTF-8");
		return source;
	}

	@BeforeEach
	void setUp() {
		LocalDateTime now = LocalDateTime.now();
		Promotion coupon = promotion(1L, PromotionKind.COUPON, DiscountType.PERCENTAGE,
				new BigDecimal("10"), null, BigDecimal.ZERO, now);
		coupon.setCode("SAVE10");
		Promotion automaticBest = promotion(2L, PromotionKind.AUTOMATIC, DiscountType.PERCENTAGE,
				new BigDecimal("15"), null, BigDecimal.ZERO, now);
		Promotion automaticLower = promotion(3L, PromotionKind.AUTOMATIC, DiscountType.PERCENTAGE,
				new BigDecimal("5"), new BigDecimal("100"), BigDecimal.ZERO, now);

		when(promotionRepository.findByCodeIgnoreCase("SAVE10")).thenReturn(Optional.of(coupon));
		when(promotionRepository.findByKindAndActiveTrueAndStartsAtLessThanEqualAndEndsAtGreaterThanEqual(
				org.mockito.ArgumentMatchers.eq(PromotionKind.AUTOMATIC),
				org.mockito.ArgumentMatchers.any(LocalDateTime.class),
				org.mockito.ArgumentMatchers.any(LocalDateTime.class)))
				.thenReturn(List.of(automaticBest, automaticLower));
	}

	@Test
	void quoteStacksCouponWithOnlyTheBestAutomaticPromotion() {
		PromotionService.PromotionQuote quote = promotionService.quote(new BigDecimal("2000"), " save10 ");

		assertEquals(new BigDecimal("200.00"), quote.couponDiscount());
		assertEquals("Automatic promotion 2", quote.automaticPromotionName());
		assertEquals(new BigDecimal("270.00"), quote.automaticDiscount());
		assertEquals(new BigDecimal("470.00"), quote.totalDiscount());
		assertEquals(new BigDecimal("1530.00"), quote.totalAmount());
	}

	@Test
	void createPromotionPublishesAnnouncementDetails() {
		LocalDateTime startsAt = LocalDateTime.now().withSecond(0).withNano(0);
		LocalDateTime endsAt = startsAt.plusDays(7);
		PromotionFormDTO form = new PromotionFormDTO();
		form.setName("Weekend offer");
		form.setKind(PromotionKind.COUPON);
		form.setCode("WEEKEND10");
		form.setDiscountType(DiscountType.PERCENTAGE);
		form.setDiscountValue(new BigDecimal("10"));
		form.setMinimumOrderAmount(new BigDecimal("100000"));
		form.setStartsAt(startsAt);
		form.setEndsAt(endsAt);
		when(promotionRepository.existsByCodeIgnoreCase("WEEKEND10")).thenReturn(false);
		when(promotionRepository.save(any(Promotion.class))).thenAnswer(invocation -> invocation.getArgument(0));

		Promotion created = promotionService.createPromotion(form);

		assertNotNull(created);
		ArgumentCaptor<PromotionAnnouncedEmailEvent> eventCaptor =
				ArgumentCaptor.forClass(PromotionAnnouncedEmailEvent.class);
		verify(applicationEventPublisher).publishEvent(eventCaptor.capture());
		assertEquals("Weekend offer", eventCaptor.getValue().name());
		assertEquals("WEEKEND10", eventCaptor.getValue().code());
		assertEquals("PERCENTAGE", eventCaptor.getValue().discountType());
		assertEquals(startsAt, eventCaptor.getValue().startsAt());
		assertEquals(endsAt, eventCaptor.getValue().endsAt());
	}

	@Test
	void quoteRejectsCouponWhenMinimumOrderIsNotReached() {
		Promotion coupon = promotion(4L, PromotionKind.COUPON, DiscountType.FIXED_AMOUNT,
				new BigDecimal("50"), null, new BigDecimal("500"), LocalDateTime.now());
		coupon.setCode("MIN500");
		when(promotionRepository.findByCodeIgnoreCase("MIN500")).thenReturn(Optional.of(coupon));

		IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
				() -> promotionService.quote(new BigDecimal("499"), "MIN500"));

		assertEquals("Đơn hàng cần đạt tối thiểu 500 ₫ để dùng ưu đãi này.", error.getMessage());
	}

	@Test
	void quoteRejectsCouponThatHasReachedItsUsageLimit() {
		Promotion coupon = promotion(5L, PromotionKind.COUPON, DiscountType.FIXED_AMOUNT,
				new BigDecimal("50"), null, BigDecimal.ZERO, LocalDateTime.now());
		coupon.setCode("LIMITED");
		coupon.setMaximumRedemptions(1);
		coupon.setRedemptionCount(1);
		when(promotionRepository.findByCodeIgnoreCase("LIMITED")).thenReturn(Optional.of(coupon));

		assertThrows(IllegalArgumentException.class,
				() -> promotionService.quote(new BigDecimal("1000"), "LIMITED"));
	}

	private Promotion promotion(Long id, PromotionKind kind, DiscountType discountType,
			BigDecimal discountValue, BigDecimal maximumDiscount, BigDecimal minimumOrder, LocalDateTime now) {
		return Promotion.builder()
				.id(id)
				.name(kind == PromotionKind.AUTOMATIC ? "Automatic promotion " + id : "Coupon " + id)
				.kind(kind)
				.discountType(discountType)
				.discountValue(discountValue)
				.maximumDiscount(maximumDiscount)
				.minimumOrderAmount(minimumOrder)
				.startsAt(now.minusHours(1))
				.endsAt(now.plusHours(1))
				.active(true)
				.redemptionCount(0)
				.build();
	}
}
