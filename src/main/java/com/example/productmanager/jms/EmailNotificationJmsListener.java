package com.example.productmanager.jms;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import com.example.productmanager.service.emailservice.EmailService;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.jms.enabled", havingValue = "true")
public class EmailNotificationJmsListener {

	private final EmailService emailService;
	private final SpringTemplateEngine templateEngine;

	@Async("emailTaskExecutor")
	@JmsListener(destination = "${app.jms.queue.email-notification:email.notification.queue}")
	public void handle(EmailNotificationMessage message) {
		if (message == null || message.type() == null) {
			return;
		}
		switch (message.type()) {
			case USER_REGISTERED -> sendWelcomeEmail(message);
			case ORDER_CONFIRMED -> sendOrderConfirmedEmail(message);
			case ORDER_CANCELLED -> sendOrderCancelledEmail(message);
			default -> {
			}
		}
	}

	private void sendWelcomeEmail(EmailNotificationMessage message) {
		Context context = new Context();
		context.setVariable("recipientName", message.recipientName());
		context.setVariable("username", message.username());
		String htmlContent = templateEngine.process("email/welcome-account", context);
		emailService.sendHtmlEmail(message.recipientEmail(), "Chào mừng bạn đến với hệ thống", htmlContent);
	}

	private void sendOrderConfirmedEmail(EmailNotificationMessage message) {
		Context context = new Context();
		context.setVariable("recipientName", message.recipientName());
		context.setVariable("order", new OrderEmailView(message.orderId(), message.orderStatus(), message.deliveryAddress()));
		context.setVariable("formattedTotal", "0");
		String htmlContent = templateEngine.process("email/order-confirmation", context);
		emailService.sendHtmlEmail(message.recipientEmail(), "Xác nhận đơn hàng #" + message.orderId(), htmlContent);
	}

	private void sendOrderCancelledEmail(EmailNotificationMessage message) {
		Context context = new Context();
		context.setVariable("recipientName", message.recipientName());
		context.setVariable("order", new OrderEmailView(message.orderId(), message.orderStatus(), message.deliveryAddress()));
		String htmlContent = templateEngine.process("email/order-cancelled", context);
		emailService.sendHtmlEmail(message.recipientEmail(), "Đơn hàng #" + message.orderId() + " đã được hủy", htmlContent);
	}

	private record OrderEmailView(Long id, String status, String deliveryAddress) {
}
}
