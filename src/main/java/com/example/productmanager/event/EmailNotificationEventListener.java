package com.example.productmanager.event;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.example.productmanager.jms.EmailNotificationMessage;
import com.example.productmanager.jms.EmailNotificationType;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.jms.enabled", havingValue = "true")
public class EmailNotificationEventListener {

	private final JmsTemplate jmsTemplate;

	@Value("${app.jms.queue.email-notification:email.notification.queue}")
	private String emailQueue;

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void handleUserRegisteredEmailEvent(UserRegisteredEmailEvent event) {
		sendToQueue(new EmailNotificationMessage(
				EmailNotificationType.USER_REGISTERED,
				event.recipientEmail(),
				event.recipientName(),
				event.username(),
				null,
				null));
	}

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void handleOrderCancelledEmailEvent(OrderCancelledEmailEvent event) {
		sendToQueue(new EmailNotificationMessage(
				EmailNotificationType.ORDER_CANCELLED,
				event.recipientEmail(),
				event.recipientName(),
				null,
				event.orderId(),
				event.orderStatus()));
	}

	private void sendToQueue(EmailNotificationMessage payload) {
		jmsTemplate.convertAndSend(emailQueue, payload);
	}
}
