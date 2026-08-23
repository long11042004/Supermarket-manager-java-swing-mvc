package com.example.productmanager.event;

public record UserRegisteredEmailEvent(
		String recipientEmail,
		String recipientName,
		String username) {
}
