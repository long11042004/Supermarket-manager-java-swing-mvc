package com.example.productmanager.controller;

import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.productmanager.model.CustomerOrder;
import com.example.productmanager.model.RoleName;
import com.example.productmanager.model.User;
import com.example.productmanager.service.CartService;
import com.example.productmanager.service.CartService.CartView;
import com.example.productmanager.service.OrderService;

import jakarta.servlet.http.HttpSession;
import lombok.AllArgsConstructor;

@Controller
@RequestMapping("/orders")
@AllArgsConstructor
public class CustomerOrderController {
	private static final Logger log = LoggerFactory.getLogger(CustomerOrderController.class);

	private final OrderService orderService;
	private final CartService cartService;
	private final MessageSource messageSource;

	private String msg(String key, Object... args) {
		return messageSource.getMessage(key, args, LocaleContextHolder.getLocale());
	}

	@GetMapping
	public String myOrders(Model model, HttpSession session) {
		User currentUser = getCustomer(session);
		if (currentUser == null) {
			return "redirect:/products";
		}

		model.addAttribute("currentUser", currentUser);
		model.addAttribute("orders", orderService.getOrdersForUser(currentUser.getId()));
		model.addAttribute("cart", getOrCreateCart(session));
		return "orders";
	}

	@PostMapping("/checkout")
	public String checkout(@RequestParam(required = false) String guestName,
			@RequestParam(required = false) String guestEmail,
			@RequestParam String deliveryAddress,
			@RequestParam(required = false) String contactPhone,
			@RequestParam(required = false) String note,
			HttpSession session,
			RedirectAttributes redirectAttributes) {
		User currentUser = getCustomer(session);
		boolean guestCheckout = Boolean.TRUE.equals(session.getAttribute("guestCheckout"));
		if (currentUser == null && !guestCheckout) {
			return "redirect:/login";
		}

		try {
			CustomerOrder createdOrder;
			if (currentUser != null) {
				createdOrder = orderService.checkout(currentUser.getId(), getOrCreateCart(session), deliveryAddress, contactPhone, note);
			} else {
				createdOrder = orderService.checkoutAsGuest(guestName, guestEmail, getOrCreateCart(session), deliveryAddress, contactPhone, note);
			}
			session.setAttribute("shoppingCart", cartService.clear(getOrCreateCart(session)));
			redirectAttributes.addFlashAttribute("successMessage", msg("msg.order.created"));
			redirectAttributes.addFlashAttribute("previewUrl", "/mail-preview/order-confirmation/" + createdOrder.getId());
		} catch (IllegalArgumentException ex) {
			redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
		} catch (DataIntegrityViolationException ex) {
			log.warn("Checkout failed due to data integrity issue", ex);
			redirectAttributes.addFlashAttribute("errorMessage", msg("msg.order.checkoutInvalidData"));
		} catch (RuntimeException ex) {
			log.error("Unexpected checkout failure", ex);
			redirectAttributes.addFlashAttribute("errorMessage", msg("msg.order.checkoutUnexpected"));
		}
		return "redirect:/products";
	}

	@PostMapping("/{orderId}/cancel")
	public String cancelOrder(@org.springframework.web.bind.annotation.PathVariable Long orderId,
			HttpSession session,
			RedirectAttributes redirectAttributes) {
		User currentUser = getCustomer(session);
		if (currentUser == null) {
			return "redirect:/login";
		}

		try {
			orderService.cancelOrderForUser(currentUser.getId(), orderId);
			redirectAttributes.addFlashAttribute("successMessage", msg("msg.order.cancelled"));
			redirectAttributes.addFlashAttribute("previewUrl", "/mail-preview/order-cancelled/" + orderId);
		} catch (IllegalArgumentException ex) {
			redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
		} catch (RuntimeException ex) {
			log.error("Unexpected cancel order failure", ex);
			redirectAttributes.addFlashAttribute("errorMessage", msg("msg.order.cancelUnexpected"));
		}
		return "redirect:/orders";
	}

	private User getCustomer(HttpSession session) {
		User currentUser = (User) session.getAttribute("loggedInUser");
		if (currentUser == null) {
			return null;
		}
		Set<RoleName> roleNames = currentUser.getRoles() == null ? Set.of() : currentUser.getRoles().stream()
				.map(role -> role.getName())
				.collect(Collectors.toSet());
		return roleNames.contains(RoleName.CUSTOMER) ? currentUser : null;
	}

	private CartView getOrCreateCart(HttpSession session) {
		CartView cart = (CartView) session.getAttribute("shoppingCart");
		if (cart == null) {
			cart = cartService.clear(new CartView());
			session.setAttribute("shoppingCart", cart);
		}
		return cart;
	}
}