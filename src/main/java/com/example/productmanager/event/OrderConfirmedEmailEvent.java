package com.example.productmanager.event;

public record OrderConfirmedEmailEvent(
		String recipientEmail,
		String recipientName,
		Long orderId,
		String orderStatus,
		String deliveryAddress) {
}
