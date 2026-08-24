package com.example.productmanager.email.jms;

import java.io.Serializable;

public record EmailNotificationMessage(
		EmailNotificationType type,
		String recipientEmail,
		String recipientName,
		String username,
		Long orderId,
		String orderStatus,
		String deliveryAddress) implements Serializable {
}
