package com.example.productmanager.controller;

import java.util.Set;
import java.util.stream.Collectors;

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
import com.example.productmanager.service.ProductService;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

@Controller
@RequestMapping("/products")
@Validated
public class ProductController {

	private final ProductService productService;

	public ProductController(ProductService productService) {
		this.productService = productService;
	}

	private boolean isAuthenticated(HttpSession session) {
		return session.getAttribute("loggedInUser") != null;
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
		if (!isAuthenticated(session)) {
			return "redirect:/login";
		}
		if (!hasPermission(session, RoleName.ADMIN, RoleName.MANAGER, RoleName.STAFF, RoleName.CUSTOMER)) {
			return "redirect:/login";
		}
		model.addAttribute("products", productService.getFilteredProducts(keyword, category));
		model.addAttribute("product", new Product());
		model.addAttribute("keyword", keyword == null ? "" : keyword);
		model.addAttribute("category", category == null ? "" : category);
		model.addAttribute("categories", productService.getAllCategories());
		model.addAttribute("canManageProducts", hasPermission(session, RoleName.ADMIN, RoleName.MANAGER));
		return "products";
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
}

