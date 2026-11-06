package com.example.productmanager.controller;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
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
import com.example.productmanager.entity.PaymentMethod;
import com.example.productmanager.entity.Product;
import com.example.productmanager.entity.RoleName;
import com.example.productmanager.entity.User;
import com.example.productmanager.exception.ConflictException;
import com.example.productmanager.exception.NotFoundException;
import com.example.productmanager.multilanguage.MessageResolver;
import com.example.productmanager.service.OrderService;
import com.example.productmanager.service.ProductService;
import com.example.productmanager.view.CounterSaleForm;

import jakarta.servlet.http.HttpSession;
import lombok.AllArgsConstructor;

@Controller
@RequestMapping("/order-management")
@AllArgsConstructor
public class OrderManagementController extends SessionController {
	private static final Logger log = LoggerFactory.getLogger(OrderManagementController.class);

	private final OrderService orderService;
	private final ProductService productService;
	private final MessageResolver messageResolver;

	@GetMapping("/new")
	public String newCounterSale(@RequestParam(value = "keyword", required = false) String keyword,
			@RequestParam(value = "page", defaultValue = "0") int page,
			@RequestParam(value = "productIds", required = false) List<Long> productIds,
			@RequestParam(value = "quantities", required = false) List<Integer> quantities,
			@RequestParam(value = "customerName", required = false) String customerName,
			@RequestParam(value = "contactPhone", required = false) String contactPhone,
			@RequestParam(value = "note", required = false) String note,
			@RequestParam(value = "paymentMethod", required = false) PaymentMethod paymentMethod,
			Model model) {
		CounterSaleForm form = new CounterSaleForm();
		form.setKeyword(keyword);
		form.setPage(Math.max(page, 0));
		if (productIds != null) {
			form.setProductIds(new ArrayList<>(productIds));
		}
		if (quantities != null) {
			form.setQuantities(new ArrayList<>(quantities));
		}
		form.setCustomerName(customerName);
		form.setContactPhone(contactPhone);
		form.setNote(note);
		if (paymentMethod != null) {
			form.setPaymentMethod(paymentMethod);
		}
		return showCounterSaleForm(form, model, null);
	}

	@PostMapping("/counter-sale")
	public String createCounterSale(@org.springframework.web.bind.annotation.ModelAttribute("counterSaleForm") CounterSaleForm form,
			BindingResult bindingResult,
			Model model,
			HttpSession session) {
		if (bindingResult.hasErrors()) {
			return showCounterSaleForm(form, model, messageResolver.msg("err.order.counterSaleInvalidInput"));
		}
		User actor = getCurrentUser(session);
		if (actor == null || actor.getId() == null) {
			return showCounterSaleForm(form, model, messageResolver.msg("err.order.userInfoNotFound"));
		}
		try {
			CustomerOrder order = orderService.createCounterSale(
					actor.getId(),
					form.getProductIds(),
					form.getQuantities(),
					form.getCustomerName(),
					form.getContactPhone(),
					form.getNote(),
					form.getPaymentMethod());
			model.addAttribute("createdOrderId", order.getId());
			model.addAttribute("counterSaleSuccessMessage", messageResolver.msg("msg.order.counterSaleCreated"));
			CounterSaleForm emptyForm = new CounterSaleForm();
			return showCounterSaleForm(emptyForm, model, null);
		} catch (IllegalArgumentException | NotFoundException ex) {
			return showCounterSaleForm(form, model, ex.getMessage());
		} catch (OptimisticLockingFailureException ex) {
			log.warn("Counter sale failed due to concurrent product update", ex);
			return showCounterSaleForm(form, model, messageResolver.msg("msg.order.checkoutConcurrentUpdate"));
		} catch (DataIntegrityViolationException ex) {
			log.warn("Counter sale failed due to data integrity issue", ex);
			return showCounterSaleForm(form, model, messageResolver.msg("msg.order.checkoutInvalidData"));
		}
	}

	@GetMapping
	public String listOrders(@RequestParam(value = "statusFilter", required = false) OrderStatus statusFilter,
			@RequestParam(value = "keyword", required = false) String keyword,
			@RequestParam(value = "fromDate", required = false) LocalDate fromDate,
			@RequestParam(value = "toDate", required = false) LocalDate toDate,
			Model model,
			HttpSession session) {
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
		model.addAttribute("canCreateCounterSale", hasRole(session, RoleName.STAFF)
				&& !hasPermission(session, RoleName.MANAGER, RoleName.ADMIN));
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

	private String showCounterSaleForm(CounterSaleForm form, Model model, String errorMessage) {
		if (form.getProductIds() == null) {
			form.setProductIds(new java.util.ArrayList<>());
		}
		if (form.getQuantities() == null) {
			form.setQuantities(new java.util.ArrayList<>());
		}
		int lineCount = Math.max(1, Math.max(form.getProductIds().size(), form.getQuantities().size()));
		while (form.getProductIds().size() < lineCount) {
			form.getProductIds().add(null);
		}
		while (form.getQuantities().size() < lineCount) {
			form.getQuantities().add(1);
		}
		int page = Math.max(form.getPage(), 0);
		Page<Product> productPage = productService.getFilteredProducts(form.getKeyword(), null, page, 20);
		Map<Long, Product> selectableProducts = new LinkedHashMap<>();
		productPage.getContent().forEach(product -> selectableProducts.put(product.getId(), product));
		form.getProductIds().stream()
				.filter(productId -> productId != null && !selectableProducts.containsKey(productId))
				.distinct()
				.forEach(productId -> {
					try {
						Product product = productService.getProductById(productId);
						selectableProducts.put(productId, product);
					} catch (NotFoundException ex) {
						log.info("Selected product {} no longer exists in counter sale form", productId);
					}
				});
		model.addAttribute("counterSaleForm", form);
		model.addAttribute("products", productPage.getContent());
		model.addAttribute("selectableProducts", selectableProducts.values());
		model.addAttribute("totalProducts", productPage.getTotalElements());
		model.addAttribute("currentPage", productPage.getNumber());
		model.addAttribute("totalPages", productPage.getTotalPages());
		model.addAttribute("paymentMethods", PaymentMethod.values());
		model.addAttribute("errorMessage", errorMessage);
		return "counter-sale";
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
