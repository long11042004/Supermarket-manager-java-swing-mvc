package com.example.productmanager.controller;

import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.productmanager.model.RoleName;
import com.example.productmanager.model.User;
import com.example.productmanager.service.CartService;
import com.example.productmanager.service.CartService.CartView;
import com.example.productmanager.service.OrderService;

import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/orders")
public class CustomerOrderController {

	private final OrderService orderService;
	private final CartService cartService;

	public CustomerOrderController(OrderService orderService, CartService cartService) {
		this.orderService = orderService;
		this.cartService = cartService;
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
			if (currentUser != null) {
				orderService.checkout(currentUser.getId(), getOrCreateCart(session), deliveryAddress, contactPhone, note);
			} else {
				orderService.checkoutAsGuest(guestName, guestEmail, getOrCreateCart(session), deliveryAddress, contactPhone, note);
			}
			session.setAttribute("shoppingCart", cartService.clear(getOrCreateCart(session)));
			redirectAttributes.addFlashAttribute("successMessage", "Đơn hàng đã được tạo thành công.");
		} catch (IllegalArgumentException ex) {
			redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
		}
		return "redirect:/products";
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