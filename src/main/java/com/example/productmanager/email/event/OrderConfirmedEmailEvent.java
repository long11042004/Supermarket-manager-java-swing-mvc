package com.example.productmanager.email.event;

import java.math.BigDecimal;

public record OrderConfirmedEmailEvent(
		String recipientEmail,
		String recipientName,
		Long orderId,
		String orderStatus,
		String deliveryAddress,
		BigDecimal totalAmount,
		BigDecimal subtotalAmount,
		BigDecimal discountAmount,
		BigDecimal shippingFee,
		String couponCode,
		String automaticPromotionName) {
}
