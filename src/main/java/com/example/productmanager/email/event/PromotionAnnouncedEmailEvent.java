package com.example.productmanager.email.event;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PromotionAnnouncedEmailEvent(
		String name,
		String code,
		String kind,
		String discountType,
		BigDecimal discountValue,
		BigDecimal minimumOrderAmount,
		LocalDateTime startsAt,
		LocalDateTime endsAt) {
}
