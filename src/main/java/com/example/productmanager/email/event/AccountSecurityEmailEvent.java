package com.example.productmanager.email.event;

public record AccountSecurityEmailEvent(
		String recipientEmail,
		String recipientName,
		Change change,
		String previousEmail,
		String newEmail) {

	public enum Change {
		PASSWORD_CHANGED,
		EMAIL_CHANGED
	}
}
