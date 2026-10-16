package com.example.productmanager.controller;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.productmanager.controller.support.SessionController;
import com.example.productmanager.entity.CustomerOrder;
import com.example.productmanager.entity.OrderStatus;
import com.example.productmanager.entity.OrderStatusHistory;
import com.example.productmanager.entity.User;
import com.example.productmanager.exception.ConflictException;
import com.example.productmanager.exception.NotFoundException;
import com.example.productmanager.multilanguage.MessageResolver;
import com.example.productmanager.service.OrderService;

import jakarta.servlet.http.HttpSession;
import lombok.AllArgsConstructor;

@Controller
@RequestMapping("/order-management")
@AllArgsConstructor
public class OrderManagementController extends SessionController {

	private final OrderService orderService;
	private final MessageResolver messageResolver;

	@GetMapping
	public String listOrders(@RequestParam(value = "statusFilter", required = false) OrderStatus statusFilter,
			@RequestParam(value = "keyword", required = false) String keyword,
			@RequestParam(value = "fromDate", required = false) LocalDate fromDate,
			@RequestParam(value = "toDate", required = false) LocalDate toDate,
			Model model) {
		List<CustomerOrder> orders = List.of();
		if (fromDate == null || toDate == null || !fromDate.isAfter(toDate)) {
			Long orderId = parseOrderId(keyword);
			orders = orderService.getOrdersForManagement(statusFilter, keyword, orderId, fromDate, toDate);
			List<Long> orderIds = orders.stream().map(CustomerOrder::getId).collect(Collectors.toList());
			Map<Long, List<OrderStatusHistory>> statusHistory = orderService.getStatusHistoryForOrders(orderIds);
			model.addAttribute("statusHistory", statusHistory);
		} else {
			model.addAttribute("errorMessage", messageResolver.msg("err.order.invalidDateRange"));
		}
		model.addAttribute("orders", orders);
		model.addAttribute("orderStatuses", OrderStatus.values());
		model.addAttribute("statusFilter", statusFilter);
		model.addAttribute("keyword", keyword == null ? "" : keyword);
		model.addAttribute("fromDate", fromDate);
		model.addAttribute("toDate", toDate);
		return "order-management";
	}

	@PostMapping("/{orderId}/status")
	public String updateStatus(@PathVariable Long orderId,
			@RequestParam OrderStatus status,
			@RequestParam(value = "statusFilter", required = false) OrderStatus statusFilter,
			@RequestParam(value = "keyword", required = false) String keyword,
			@RequestParam(value = "fromDate", required = false) LocalDate fromDate,
			@RequestParam(value = "toDate", required = false) LocalDate toDate,
			RedirectAttributes redirectAttributes,
			HttpSession session) {
		try {
			User actor = getCurrentUser(session);
			if (actor == null || actor.getId() == null) {
				redirectAttributes.addFlashAttribute("errorMessage", messageResolver.msg("err.order.userInfoNotFound"));
				return redirectToFilteredList(redirectAttributes, statusFilter, keyword, fromDate, toDate);
			}
			orderService.updateOrderStatus(orderId, status, actor.getId());
			redirectAttributes.addFlashAttribute("successMessage", messageResolver.msg("msg.order.statusUpdated"));
		} catch (ConflictException | NotFoundException ex) {
			redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
		}
		return redirectToFilteredList(redirectAttributes, statusFilter, keyword, fromDate, toDate);
	}

	private String redirectToFilteredList(RedirectAttributes redirectAttributes,
			OrderStatus statusFilter,
			String keyword,
			LocalDate fromDate,
			LocalDate toDate) {
		if (statusFilter != null) {
			redirectAttributes.addAttribute("statusFilter", statusFilter);
		}
		if (keyword != null && !keyword.isBlank()) {
			redirectAttributes.addAttribute("keyword", keyword);
		}
		if (fromDate != null) {
			redirectAttributes.addAttribute("fromDate", fromDate);
		}
		if (toDate != null) {
			redirectAttributes.addAttribute("toDate", toDate);
		}
		return "redirect:/order-management";
	}

	private Long parseOrderId(String keyword) {
		if (keyword == null || keyword.isBlank()) {
			return null;
		}
		try {
			return Long.valueOf(keyword.trim());
		} catch (NumberFormatException ex) {
			return null;
		}
	}
}
