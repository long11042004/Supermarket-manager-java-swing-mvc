package com.example.productmanager.email.event;

public record OrderConfirmedEmailEvent(
		String recipientEmail,
		String recipientName,
		Long orderId,
		String orderStatus,
		String deliveryAddress) {
}
