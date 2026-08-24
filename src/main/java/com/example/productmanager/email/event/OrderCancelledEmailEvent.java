package com.example.productmanager.email.event;

public record OrderCancelledEmailEvent(
		String recipientEmail,
		String recipientName,
		Long orderId,
		String orderStatus) {
}
