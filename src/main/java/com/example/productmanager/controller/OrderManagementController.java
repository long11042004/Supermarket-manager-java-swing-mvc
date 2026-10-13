package com.example.productmanager.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.productmanager.entity.OrderStatus;
import com.example.productmanager.exception.ConflictException;
import com.example.productmanager.exception.NotFoundException;
import com.example.productmanager.multilanguage.MessageResolver;
import com.example.productmanager.service.OrderService;

import lombok.AllArgsConstructor;

@Controller
@RequestMapping("/order-management")
@AllArgsConstructor
public class OrderManagementController {

	private final OrderService orderService;
	private final MessageResolver messageResolver;

	@GetMapping
	public String listOrders(Model model) {
		model.addAttribute("orders", orderService.getOrdersForManagement());
		return "order-management";
	}

	@PostMapping("/{orderId}/status")
	public String updateStatus(@PathVariable Long orderId,
			@RequestParam OrderStatus status,
			RedirectAttributes redirectAttributes) {
		try {
			orderService.updateOrderStatus(orderId, status);
			redirectAttributes.addFlashAttribute("successMessage", messageResolver.msg("msg.order.statusUpdated"));
		} catch (ConflictException | NotFoundException ex) {
			redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
		}
		return "redirect:/order-management";
	}
}
