package com.example.productmanager.controller;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.example.productmanager.model.Product;
import com.example.productmanager.model.RoleName;
import com.example.productmanager.model.User;
import com.example.productmanager.service.ProductService;
import com.example.productmanager.service.UserService;

import jakarta.servlet.http.HttpSession;

@Controller
public class DashboardController {

	private final ProductService productService;
	private final UserService userService;

	public DashboardController(ProductService productService, UserService userService) {
		this.productService = productService;
		this.userService = userService;
	}

	@GetMapping("/dashboard")
	public String dashboard(Model model, HttpSession session) {
		User currentUser = (User) session.getAttribute("loggedInUser");
		if (currentUser == null) {
			return "redirect:/login";
		}

		Set<RoleName> roles = currentUser.getRoles() == null ? Set.of() : currentUser.getRoles().stream()
				.map(role -> role.getName())
				.collect(Collectors.toSet());

		List<Product> products = productService.getAllProducts();
		List<User> users = userService.getAllUsers();
		List<Product> lowStockProducts = productService.getLowStockProducts(10);

		model.addAttribute("currentUser", currentUser);
		model.addAttribute("roles", roles);
		model.addAttribute("isAdmin", roles.contains(RoleName.ADMIN));
		model.addAttribute("isManager", roles.contains(RoleName.MANAGER));
		model.addAttribute("isStaff", roles.contains(RoleName.STAFF));
		model.addAttribute("isCustomer", roles.contains(RoleName.CUSTOMER));
		model.addAttribute("canManageProducts", roles.contains(RoleName.ADMIN) || roles.contains(RoleName.MANAGER));
		model.addAttribute("canManageUsers", roles.contains(RoleName.ADMIN) || roles.contains(RoleName.MANAGER));
		model.addAttribute("totalProducts", products.size());
		model.addAttribute("totalUsers", users.size());
		model.addAttribute("lowStockCount", lowStockProducts.size());
		model.addAttribute("products", products);
		model.addAttribute("users", users);
		model.addAttribute("lowStockProducts", lowStockProducts);

		if (roles.contains(RoleName.CUSTOMER)) {
			return "customer-dashboard";
		}
		return "dashboard";
	}
}
