package com.example.productmanager.controller;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.example.productmanager.controller.support.SessionController;
import com.example.productmanager.dto.product.ProductFormDTO;
import com.example.productmanager.entity.Product;
import com.example.productmanager.entity.RoleName;
import com.example.productmanager.entity.User;
import com.example.productmanager.lifecycle.PrototypeRequestMarker;
import com.example.productmanager.lifecycle.SessionLifecycleBean;
import com.example.productmanager.modelmapper.ProductMapper;
import com.example.productmanager.multilanguage.MessageResolver;
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
public class ProductController extends SessionController {

	private final ProductService productService;
	private final CartService cartService;
	private final UserService userService;
	private final ProductMapper productMapper;
	private final MessageResolver messageResolver;
	private final SessionLifecycleBean sessionLifecycleBean;
	private final ObjectProvider<PrototypeRequestMarker> prototypeRequestMarkerProvider;

	private boolean canShop(HttpSession session) {
		return isAuthenticated(session) || isGuestSession(session);
	}

	@GetMapping
	public String showProductsPage(
			@RequestParam(value = "keyword", required = false) String keyword,
			@RequestParam(value = "category", required = false) String category,
			@RequestParam(value = "page", defaultValue = "0") int page,
			@RequestParam(value = "size", defaultValue = "12") int size,
			HttpSession session,
			Model model) {
		if (isAuthenticated(session)
				&& !hasPermission(session, RoleName.ADMIN, RoleName.MANAGER, RoleName.STAFF, RoleName.CUSTOMER)) {
			return "redirect:/login";
		}
		if (!canShop(session)) {
			session.setAttribute("guestCheckout", true);
		}
		PrototypeRequestMarker requestMarker = prototypeRequestMarkerProvider.getObject();
		int visitCount = sessionLifecycleBean.increaseAndGetVisitCount();
		Page<Product> productsPage = productService.getFilteredProducts(keyword, category, page, size);
		model.addAttribute("products", productsPage.getContent());
		model.addAttribute("currentPage", productsPage.getNumber());
		model.addAttribute("totalPages", productsPage.getTotalPages());
		model.addAttribute("pageSize", productsPage.getSize());
		model.addAttribute("totalProducts", productsPage.getTotalElements());
		model.addAttribute("product", new ProductFormDTO());
		model.addAttribute("keyword", keyword == null ? "" : keyword);
		model.addAttribute("category", category == null ? "" : category);
		model.addAttribute("categories", productService.getAllCategories());
		model.addAttribute("canManageProducts", hasPermission(session, RoleName.ADMIN, RoleName.MANAGER));
		model.addAttribute("isAdmin", hasPermission(session, RoleName.ADMIN));
		model.addAttribute("isManager", hasPermission(session, RoleName.MANAGER));
		model.addAttribute("isStaff", hasPermission(session, RoleName.STAFF));
		model.addAttribute("isCustomer", hasPermission(session, RoleName.CUSTOMER));
		model.addAttribute("isGuest", !isAuthenticated(session));
		model.addAttribute("cart", getOrCreateCart(session));
		model.addAttribute("lifecycleSessionToken", sessionLifecycleBean.getSessionToken());
		model.addAttribute("lifecycleVisitCount", visitCount);
		model.addAttribute("lifecycleRequestMarker", requestMarker.getMarkerId());
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
			User currentUser = getCurrentUser(session);
			if (currentUser != null) {
				String action = messageResolver.msg("activity.cart.added");
				String details = messageResolver.msg("activity.cart.addedDetail", product.getDisplayName());
				userService.recordActivity(currentUser.getId(), action, details);
			}
			redirectAttributes.addFlashAttribute("successMessage", messageResolver.msg("msg.cart.added"));
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
			redirectAttributes.addFlashAttribute("successMessage", messageResolver.msg("msg.cart.updated"));
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
	public String createProduct(@ModelAttribute("product") @Valid ProductFormDTO product, HttpSession session) {
		if (!hasPermission(session, RoleName.ADMIN, RoleName.MANAGER)) {
			return "redirect:/login";
		}
		productService.createProduct(productMapper.toEntity(product));
		return "redirect:/products";
	}

	@PostMapping("/{id}/delete")
	public String deleteProduct(@PathVariable Long id,
			HttpSession session,
			org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes) {
		if (!hasPermission(session, RoleName.ADMIN, RoleName.MANAGER)) {
			return "redirect:/login";
		}
		try {
			productService.deleteProduct(id);
			redirectAttributes.addFlashAttribute("successMessage", messageResolver.msg("msg.product.deleted"));
		} catch (IllegalArgumentException ex) {
			redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
		}
		return "redirect:/products";
	}

	@GetMapping("/{id}/edit")
	public String editProduct(@PathVariable Long id, HttpSession session, Model model) {
		if (!hasPermission(session, RoleName.ADMIN, RoleName.MANAGER)) {
			return "redirect:/login";
		}
		Product product = productService.getProductById(id);
		Page<Product> productsPage = productService.getFilteredProducts(null, null, 0, 12);
		model.addAttribute("product", productMapper.toForm(product));
		model.addAttribute("products", productsPage.getContent());
		model.addAttribute("currentPage", productsPage.getNumber());
		model.addAttribute("totalPages", productsPage.getTotalPages());
		model.addAttribute("pageSize", productsPage.getSize());
		model.addAttribute("totalProducts", productsPage.getTotalElements());
		model.addAttribute("keyword", "");
		model.addAttribute("category", "");
		model.addAttribute("categories", productService.getAllCategories());
		model.addAttribute("canManageProducts", true);
		model.addAttribute("isAdmin", hasPermission(session, RoleName.ADMIN));
		model.addAttribute("isManager", hasPermission(session, RoleName.MANAGER));
		model.addAttribute("isStaff", hasPermission(session, RoleName.STAFF));
		model.addAttribute("isCustomer", hasPermission(session, RoleName.CUSTOMER));
		model.addAttribute("isGuest", false);
		model.addAttribute("cart", getOrCreateCart(session));
		return "products";
	}

	@PostMapping("/{id}/update")
	public String updateProduct(@PathVariable Long id, @ModelAttribute("product") @Valid ProductFormDTO product, HttpSession session) {
		if (!hasPermission(session, RoleName.ADMIN, RoleName.MANAGER)) {
			return "redirect:/login";
		}
		productService.updateProduct(id, productMapper.toEntity(product));
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

