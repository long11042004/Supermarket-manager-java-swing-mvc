package com.example.productmanager.email.event;

public record UserRegisteredEmailEvent(
		String recipientEmail,
		String recipientName,
		String username) {
}
