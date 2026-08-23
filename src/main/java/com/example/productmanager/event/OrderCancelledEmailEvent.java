package com.example.productmanager.event;

public record OrderCancelledEmailEvent(
		String recipientEmail,
		String recipientName,
		Long orderId,
		String orderStatus) {
}
