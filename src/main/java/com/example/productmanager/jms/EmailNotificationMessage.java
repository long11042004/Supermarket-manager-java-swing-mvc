package com.example.productmanager.jms;

import java.io.Serializable;

public record EmailNotificationMessage(
		EmailNotificationType type,
		String recipientEmail,
		String recipientName,
		String username,
		Long orderId,
		String orderStatus) implements Serializable {
}
