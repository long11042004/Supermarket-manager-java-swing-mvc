package com.example.productmanager.email.event;

public record OrderStatusUpdatedEmailEvent(
		String recipientEmail,
		String recipientName,
		Long orderId,
		String orderStatus,
		String deliveryAddress) {
}
