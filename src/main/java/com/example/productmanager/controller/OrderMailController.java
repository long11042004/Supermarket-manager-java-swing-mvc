package com.example.productmanager.controller;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import com.example.productmanager.model.CustomerOrder;
import com.example.productmanager.repository.CustomerOrderRepository;
import com.example.productmanager.service.EmailService;

import lombok.AllArgsConstructor;

@RestController
@RequestMapping("/api/orders")
@AllArgsConstructor
public class OrderMailController {

	private final CustomerOrderRepository customerOrderRepository;
	private final EmailService emailService;
	private final SpringTemplateEngine templateEngine;

	@PostMapping("/{orderId}/send-confirmation-email")
	public Map<String, String> sendConfirmationEmail(@PathVariable Long orderId,
			@RequestParam(required = false) String email) {
		CustomerOrder order = customerOrderRepository.findById(orderId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));

		String recipient = resolveRecipient(order, email);
		String recipientName = resolveRecipientName(order);
		String htmlBody = renderConfirmationHtml(order, recipientName);
		emailService.sendHtmlEmail(recipient, "Xác nhận đơn hàng #" + order.getId(), htmlBody);

		return Map.of(
				"status", "sent",
				"orderId", String.valueOf(order.getId()),
				"recipient", recipient);
	}

	private String resolveRecipient(CustomerOrder order, String email) {
		String candidate = email;
		if ((candidate == null || candidate.isBlank()) && order.getUser() != null) {
			candidate = order.getUser().getEmail();
		}
		if ((candidate == null || candidate.isBlank())) {
			candidate = order.getGuestEmail();
		}
		if (candidate == null || candidate.isBlank()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No email available for order confirmation.");
		}
		return candidate;
	}

	private String resolveRecipientName(CustomerOrder order) {
		if (order.getUser() != null && order.getUser().getFullName() != null && !order.getUser().getFullName().isBlank()) {
			return order.getUser().getFullName();
		}
		return order.getGuestName() != null ? order.getGuestName() : "Khách hàng";
	}

	private String renderConfirmationHtml(CustomerOrder order, String recipientName) {
		Context context = new Context();
		context.setVariable("order", order);
		context.setVariable("recipientName", recipientName);
		context.setVariable("formattedTotal", formatMoney(order.getTotalAmount()));
		return templateEngine.process("email/order-confirmation", context);
	}

	private String formatMoney(java.math.BigDecimal amount) {
		if (amount == null) {
			return "0";
		}
		return amount.stripTrailingZeros().toPlainString();
	}
}
