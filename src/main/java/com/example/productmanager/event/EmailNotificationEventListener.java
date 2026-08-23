package com.example.productmanager.event;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import com.example.productmanager.service.emailservice.EmailService;

import lombok.AllArgsConstructor;

@Component
@AllArgsConstructor
public class EmailNotificationEventListener {

	private final EmailService emailService;
	private final SpringTemplateEngine templateEngine;

	@Async("emailTaskExecutor")
	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void handleUserRegisteredEmailEvent(UserRegisteredEmailEvent event) {
		Context context = new Context();
		context.setVariable("recipientName", event.recipientName());
		context.setVariable("username", event.username());
		String htmlContent = templateEngine.process("email/welcome-account", context);
		emailService.sendHtmlEmail(event.recipientEmail(), "Chào mừng bạn đến với hệ thống", htmlContent);
	}

	@Async("emailTaskExecutor")
	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void handleOrderCancelledEmailEvent(OrderCancelledEmailEvent event) {
		Context context = new Context();
		context.setVariable("recipientName", event.recipientName());
		context.setVariable("order", new OrderEmailView(event.orderId(), event.orderStatus()));
		String htmlContent = templateEngine.process("email/order-cancelled", context);
		emailService.sendHtmlEmail(event.recipientEmail(), "Đơn hàng #" + event.orderId() + " đã được hủy", htmlContent);
	}

	public record OrderEmailView(Long id, String status) {
	}
}
