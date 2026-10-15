package com.example.productmanager.email.jms;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record EmailNotificationMessage(
		EmailNotificationType type,
		String recipientEmail,
		String recipientName,
		String username,
		OrderNotice order,
		AccountSecurityNotice security,
		PromotionNotice promotion) implements Serializable {

	public record OrderNotice(
			Long id,
			String status,
			String deliveryAddress,
			BigDecimal totalAmount,
			BigDecimal subtotalAmount,
			BigDecimal discountAmount,
			BigDecimal shippingFee,
			String couponCode,
			String automaticPromotionName) implements Serializable {
	}

	public record AccountSecurityNotice(
			String change,
			String previousEmail,
			String newEmail) implements Serializable {
	}

	public record PromotionNotice(
			String name,
			String code,
			String kind,
			String discountType,
			BigDecimal discountValue,
			BigDecimal minimumOrderAmount,
			LocalDateTime startsAt,
			LocalDateTime endsAt) implements Serializable {
	}
}
