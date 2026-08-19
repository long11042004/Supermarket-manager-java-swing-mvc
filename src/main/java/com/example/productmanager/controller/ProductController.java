package com.example.productmanager.controller;

import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.example.productmanager.model.Product;
import com.example.productmanager.model.RoleName;
import com.example.productmanager.model.User;
import com.example.productmanager.service.CartService;
import com.example.productmanager.service.CartService.CartView;
import com.example.productmanager.service.ProductService;
import com.example.productmanager.service.UserService;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;

@Controller
@RequestMapping("/products")
@Validated
@AllArgsConstructor
public class ProductController {

	private final ProductService productService;
	private final CartService cartService;
	private final UserService userService;
	private final MessageSource messageSource;

	private String msg(String key, Object... args) {
		return messageSource.getMessage(key, args, LocaleContextHolder.getLocale());
	}

	private boolean isAuthenticated(HttpSession session) {
		return session.getAttribute("loggedInUser") != null;
	}

	private boolean isGuestSession(HttpSession session) {
		return Boolean.TRUE.equals(session.getAttribute("guestCheckout"));
	}

	private boolean canShop(HttpSession session) {
		return isAuthenticated(session) || isGuestSession(session);
	}

	private boolean hasPermission(HttpSession session, RoleName... allowedRoles) {
		if (!isAuthenticated(session)) {
			return false;
		}
		User currentUser = (User) session.getAttribute("loggedInUser");
		Set<RoleName> roleNames = currentUser.getRoles() == null ? Set.of() : currentUser.getRoles().stream()
				.map(role -> role.getName())
				.collect(Collectors.toSet());
		for (RoleName role : allowedRoles) {
			if (roleNames.contains(role)) {
				return true;
			}
		}
		return false;
	}

	@GetMapping
	public String showProductsPage(
			@RequestParam(value = "keyword", required = false) String keyword,
			@RequestParam(value = "category", required = false) String category,
			HttpSession session,
			Model model) {
		if (isAuthenticated(session)
				&& !hasPermission(session, RoleName.ADMIN, RoleName.MANAGER, RoleName.STAFF, RoleName.CUSTOMER)) {
			return "redirect:/login";
		}
		if (!canShop(session) && session.getAttribute("loggedInUser") == null) {
			session.setAttribute("guestCheckout", true);
		}
		model.addAttribute("products", productService.getFilteredProducts(keyword, category));
		model.addAttribute("product", new Product());
		model.addAttribute("keyword", keyword == null ? "" : keyword);
		model.addAttribute("category", category == null ? "" : category);
		model.addAttribute("categories", productService.getAllCategories());
		model.addAttribute("canManageProducts", hasPermission(session, RoleName.ADMIN, RoleName.MANAGER));
		model.addAttribute("isCustomer", hasPermission(session, RoleName.CUSTOMER));
		model.addAttribute("isGuest", !isAuthenticated(session));
		model.addAttribute("cart", getOrCreateCart(session));
		return "products";
	}

	@PostMapping("/{id}/cart")
	public String addToCart(@PathVariable Long id,
			@RequestParam(defaultValue = "1") int quantity,
			HttpSession session,
			org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes) {
		if (!hasPermission(session, RoleName.CUSTOMER) && !isGuestSession(session)) {
			return "redirect:/login";
		}
		try {
			Product product = productService.getProductById(id);
			CartView updatedCart = cartService.addItem(getOrCreateCart(session), product, quantity);
			session.setAttribute("shoppingCart", updatedCart);
			User currentUser = (User) session.getAttribute("loggedInUser");
			if (currentUser != null) {
				String details = new StringBuilder("Đã thêm ")
						.append(product.getName())
						.append(" vào giỏ hàng")
						.toString();
				userService.recordActivity(currentUser.getId(), "Thêm vào giỏ", details);
			}
			redirectAttributes.addFlashAttribute("successMessage", msg("msg.cart.added"));
		} catch (IllegalArgumentException ex) {
			redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
		}
		return "redirect:/products";
	}

	@PostMapping("/cart/{productId}/quantity")
	public String updateCartItemQuantity(@PathVariable Long productId,
			@RequestParam int quantity,
			HttpSession session,
			org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes) {
		if (!hasPermission(session, RoleName.CUSTOMER) && !isGuestSession(session)) {
			return "redirect:/login";
		}
		try {
			Product product = productService.getProductById(productId);
			CartView updatedCart = cartService.updateItemQuantity(getOrCreateCart(session), productId, quantity, product.getQuantity());
			session.setAttribute("shoppingCart", updatedCart);
			redirectAttributes.addFlashAttribute("successMessage", msg("msg.cart.updated"));
		} catch (IllegalArgumentException ex) {
			redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
		}
		return "redirect:/products";
	}

	@PostMapping("/cart/{productId}/remove")
	public String removeCartItem(@PathVariable Long productId, HttpSession session) {
		if (!hasPermission(session, RoleName.CUSTOMER) && !isGuestSession(session)) {
			return "redirect:/login";
		}
		session.setAttribute("shoppingCart", cartService.removeItem(getOrCreateCart(session), productId));
		return "redirect:/products";
	}

	@PostMapping
	public String createProduct(@ModelAttribute("product") @Valid Product product, HttpSession session) {
		if (!hasPermission(session, RoleName.ADMIN, RoleName.MANAGER)) {
			return "redirect:/login";
		}
		productService.createProduct(product);
		return "redirect:/products";
	}

	@PostMapping("/{id}/delete")
	public String deleteProduct(@PathVariable Long id, HttpSession session) {
		if (!hasPermission(session, RoleName.ADMIN, RoleName.MANAGER)) {
			return "redirect:/login";
		}
		productService.deleteProduct(id);
		return "redirect:/products";
	}

	@GetMapping("/{id}/edit")
	public String editProduct(@PathVariable Long id, HttpSession session, Model model) {
		if (!hasPermission(session, RoleName.ADMIN, RoleName.MANAGER)) {
			return "redirect:/login";
		}
		Product product = productService.getProductById(id);
		model.addAttribute("product", product);
		model.addAttribute("products", productService.getFilteredProducts(null, null));
		model.addAttribute("keyword", "");
		model.addAttribute("category", "");
		model.addAttribute("categories", productService.getAllCategories());
		model.addAttribute("canManageProducts", true);
		model.addAttribute("isCustomer", hasPermission(session, RoleName.CUSTOMER));
		model.addAttribute("isGuest", false);
		model.addAttribute("cart", getOrCreateCart(session));
		return "products";
	}

	@PostMapping("/{id}/update")
	public String updateProduct(@PathVariable Long id, @ModelAttribute("product") @Valid Product product, HttpSession session) {
		if (!hasPermission(session, RoleName.ADMIN, RoleName.MANAGER)) {
			return "redirect:/login";
		}
		productService.updateProduct(id, product);
		return "redirect:/products";
	}

	private CartView getOrCreateCart(HttpSession session) {
		CartView cart = (CartView) session.getAttribute("shoppingCart");
		if (cart == null) {
			cart = new CartView();
			session.setAttribute("shoppingCart", cart);
		}
		return cart;
	}
}

