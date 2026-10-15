package com.example.productmanager.email.jms;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import com.example.productmanager.email.service.EmailService;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.jms.enabled", havingValue = "true")
public class EmailNotificationJmsListener {

	private static final DateTimeFormatter EMAIL_DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

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
			case ORDER_STATUS_UPDATED -> sendOrderStatusUpdatedEmail(message);
			case ACCOUNT_SECURITY -> sendAccountSecurityEmail(message);
			case PROMOTION_ANNOUNCED -> sendPromotionAnnouncementEmail(message);
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
		EmailNotificationMessage.OrderNotice order = message.order();
		context.setVariable("order", new OrderEmailView(
				order.id(),
				order.status(),
				order.deliveryAddress(),
				order.discountAmount(),
				order.shippingFee(),
				order.couponCode(),
				order.automaticPromotionName()));
		context.setVariable("formattedSubtotal", formatMoney(
				order.subtotalAmount() == null ? order.totalAmount() : order.subtotalAmount()));
		context.setVariable("formattedDiscount", formatMoney(order.discountAmount()));
		context.setVariable("formattedShippingFee", formatMoney(order.shippingFee()));
		context.setVariable("formattedTotal", formatMoney(order.totalAmount()));
		String htmlContent = templateEngine.process("email/order-confirmation", context);
		emailService.sendHtmlEmail(message.recipientEmail(), "Xác nhận đơn hàng #" + order.id(), htmlContent);
	}

	private void sendOrderCancelledEmail(EmailNotificationMessage message) {
		Context context = new Context();
		context.setVariable("recipientName", message.recipientName());
		EmailNotificationMessage.OrderNotice order = message.order();
		context.setVariable("order", new OrderEmailView(
				order.id(),
				order.status(),
				order.deliveryAddress(),
				order.discountAmount(),
				order.shippingFee(),
				order.couponCode(),
				order.automaticPromotionName()));
		String htmlContent = templateEngine.process("email/order-cancelled", context);
		emailService.sendHtmlEmail(message.recipientEmail(), "Đơn hàng #" + order.id() + " đã được hủy", htmlContent);
	}

	private void sendOrderStatusUpdatedEmail(EmailNotificationMessage message) {
		Context context = new Context();
		EmailNotificationMessage.OrderNotice order = message.order();
		context.setVariable("recipientName", message.recipientName());
		context.setVariable("order", order);
		String htmlContent = templateEngine.process("email/order-status-updated", context);
		emailService.sendHtmlEmail(
				message.recipientEmail(),
				"Cập nhật đơn hàng #" + order.id(),
				htmlContent);
	}

	private void sendAccountSecurityEmail(EmailNotificationMessage message) {
		Context context = new Context();
		context.setVariable("recipientName", message.recipientName());
		context.setVariable("security", message.security());
		String htmlContent = templateEngine.process("email/account-security", context);
		emailService.sendHtmlEmail(message.recipientEmail(), "Thông báo bảo mật tài khoản", htmlContent);
	}

	private void sendPromotionAnnouncementEmail(EmailNotificationMessage message) {
		Context context = new Context();
		EmailNotificationMessage.PromotionNotice promotion = message.promotion();
		context.setVariable("recipientName", message.recipientName());
		context.setVariable("promotion", promotion);
		context.setVariable("discountDescription", discountDescription(promotion));
		context.setVariable("minimumOrder", formatMoney(promotion.minimumOrderAmount()));
		context.setVariable("startsAt", formatDateTime(promotion.startsAt()));
		context.setVariable("endsAt", formatDateTime(promotion.endsAt()));
		String htmlContent = templateEngine.process("email/promotion-announcement", context);
		emailService.sendHtmlEmail(message.recipientEmail(), "Ưu đãi mới: " + promotion.name(), htmlContent);
	}

	private String formatMoney(BigDecimal amount) {
		if (amount == null) {
			return "0";
		}
		return amount.stripTrailingZeros().toPlainString();
	}

	private String discountDescription(EmailNotificationMessage.PromotionNotice promotion) {
		String value = promotion.discountValue().stripTrailingZeros().toPlainString();
		return "PERCENTAGE".equals(promotion.discountType()) ? value + "%" : formatMoney(promotion.discountValue()) + " ₫";
	}

	private String formatDateTime(LocalDateTime value) {
		return value == null ? "" : EMAIL_DATE_FORMAT.format(value);
	}

	private record OrderEmailView(
			Long id,
			String status,
			String deliveryAddress,
			BigDecimal discountAmount,
			BigDecimal shippingFee,
			String couponCode,
			String automaticPromotionName) {
	}
}
