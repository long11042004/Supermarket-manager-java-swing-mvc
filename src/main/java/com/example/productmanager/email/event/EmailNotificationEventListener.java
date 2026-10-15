package com.example.productmanager.email.event;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.example.productmanager.entity.RoleName;
import com.example.productmanager.entity.User;
import com.example.productmanager.repository.UserRepository;
import com.example.productmanager.email.jms.EmailNotificationMessage;
import com.example.productmanager.email.jms.EmailNotificationType;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.jms.enabled", havingValue = "true")
public class EmailNotificationEventListener {

	private final JmsTemplate jmsTemplate;
	private final UserRepository userRepository;

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
				null,
				null));
	}

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void handleOrderConfirmedEmailEvent(OrderConfirmedEmailEvent event) {
		sendToQueue(new EmailNotificationMessage(
				EmailNotificationType.ORDER_CONFIRMED,
				event.recipientEmail(),
				event.recipientName(),
				null,
				new EmailNotificationMessage.OrderNotice(
						event.orderId(),
						event.orderStatus(),
						event.deliveryAddress(),
						event.totalAmount(),
						event.subtotalAmount(),
						event.discountAmount(),
						event.shippingFee(),
						event.couponCode(),
						event.automaticPromotionName()),
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
				new EmailNotificationMessage.OrderNotice(
						event.orderId(),
						event.orderStatus(),
						null,
						null,
						null,
						null,
						null,
						null,
						null),
				null,
				null));
	}

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void handleOrderStatusUpdatedEmailEvent(OrderStatusUpdatedEmailEvent event) {
		sendToQueue(new EmailNotificationMessage(
				EmailNotificationType.ORDER_STATUS_UPDATED,
				event.recipientEmail(),
				event.recipientName(),
				null,
				new EmailNotificationMessage.OrderNotice(
						event.orderId(),
						event.orderStatus(),
						event.deliveryAddress(),
						null,
						null,
						null,
						null,
						null,
						null),
				null,
				null));
	}

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void handleAccountSecurityEmailEvent(AccountSecurityEmailEvent event) {
		sendToQueue(new EmailNotificationMessage(
				EmailNotificationType.ACCOUNT_SECURITY,
				event.recipientEmail(),
				event.recipientName(),
				null,
				null,
				new EmailNotificationMessage.AccountSecurityNotice(
						event.change().name(),
						event.previousEmail(),
						event.newEmail()),
				null));
	}

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void handlePromotionAnnouncedEmailEvent(PromotionAnnouncedEmailEvent event) {
		EmailNotificationMessage.PromotionNotice notice = new EmailNotificationMessage.PromotionNotice(
				event.name(),
				event.code(),
				event.kind(),
				event.discountType(),
				event.discountValue(),
				event.minimumOrderAmount(),
				event.startsAt(),
				event.endsAt());
		for (User user : userRepository.findPromotionEmailSubscribers(RoleName.CUSTOMER)) {
			String recipientName = user.getFullName() == null || user.getFullName().isBlank()
					? user.getUsername()
					: user.getFullName();
			sendToQueue(new EmailNotificationMessage(
					EmailNotificationType.PROMOTION_ANNOUNCED,
					user.getEmail(),
					recipientName,
					null,
					null,
					null,
					notice));
		}
	}

	private void sendToQueue(EmailNotificationMessage payload) {
		jmsTemplate.convertAndSend(emailQueue, payload);
	}
}
