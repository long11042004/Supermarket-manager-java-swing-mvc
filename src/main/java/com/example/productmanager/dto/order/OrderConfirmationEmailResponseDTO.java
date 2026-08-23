package com.example.productmanager.dto.order;

public record OrderConfirmationEmailResponseDTO(
		String status,
		Long orderId,
		String recipient) {
}
