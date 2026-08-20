package com.example.productmanager.controller;

import java.math.BigDecimal;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.server.ResponseStatusException;

import com.example.productmanager.model.CustomerOrder;
import com.example.productmanager.model.User;
import com.example.productmanager.repository.CustomerOrderRepository;
import com.example.productmanager.repository.UserRepository;

import lombok.AllArgsConstructor;

@Controller
@RequestMapping("/mail-preview")
@AllArgsConstructor
public class MailPreviewController {

	private final CustomerOrderRepository customerOrderRepository;
	private final UserRepository userRepository;

	@GetMapping("/order-confirmation/{orderId}")
	public String orderConfirmationPreview(@PathVariable Long orderId, Model model) {
		CustomerOrder order = customerOrderRepository.findById(orderId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));
		model.addAttribute("order", order);
		model.addAttribute("recipientName", resolveOrderRecipientName(order));
		model.addAttribute("formattedTotal", formatMoney(order.getTotalAmount()));
		return "email/order-confirmation";
	}

	@GetMapping("/order-cancelled/{orderId}")
	public String orderCancelledPreview(@PathVariable Long orderId, Model model) {
		CustomerOrder order = customerOrderRepository.findById(orderId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));
		model.addAttribute("order", order);
		model.addAttribute("recipientName", resolveOrderRecipientName(order));
		return "email/order-cancelled";
	}

	@GetMapping("/welcome-account/{userId}")
	public String welcomeAccountPreview(@PathVariable Long userId, Model model) {
		User user = userRepository.findById(userId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
		String recipientName = user.getFullName() == null || user.getFullName().isBlank() ? user.getUsername() : user.getFullName();
		model.addAttribute("recipientName", recipientName);
		model.addAttribute("username", user.getUsername());
		return "email/welcome-account";
	}

	private String resolveOrderRecipientName(CustomerOrder order) {
		if (order.getUser() != null && order.getUser().getFullName() != null && !order.getUser().getFullName().isBlank()) {
			return order.getUser().getFullName();
		}
		if (order.getGuestName() != null && !order.getGuestName().isBlank()) {
			return order.getGuestName();
		}
		return "Khách hàng";
	}

	private String formatMoney(BigDecimal amount) {
		if (amount == null) {
			return "0";
		}
		return amount.stripTrailingZeros().toPlainString();
	}
}
