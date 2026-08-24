package com.example.productmanager.controller;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.example.productmanager.controller.support.SessionController;
import com.example.productmanager.entity.Product;
import com.example.productmanager.entity.RoleName;
import com.example.productmanager.entity.User;
import com.example.productmanager.lifecycle.PrototypeRequestMarker;
import com.example.productmanager.lifecycle.SessionLifecycleBean;
import com.example.productmanager.service.ProductService;
import com.example.productmanager.service.UserService;

import jakarta.servlet.http.HttpSession;
import lombok.AllArgsConstructor;

@Controller
@AllArgsConstructor
public class DashboardController extends SessionController {

	private final ProductService productService;
	private final UserService userService;
	private final SessionLifecycleBean sessionLifecycleBean;
	private final ObjectProvider<PrototypeRequestMarker> prototypeRequestMarkerProvider;

	@GetMapping("/dashboard")
	public String dashboard(Model model, HttpSession session) {
		User currentUser = getCurrentUser(session);
		if (currentUser == null) {
			return "redirect:/login";
		}
		PrototypeRequestMarker requestMarker = prototypeRequestMarkerProvider.getObject();
		int visitCount = sessionLifecycleBean.increaseAndGetVisitCount();

		Set<RoleName> roles = currentUser.getRoles() == null ? Set.of() : currentUser.getRoles().stream()
				.map(role -> role.getName())
				.collect(Collectors.toSet());

		List<Product> products = productService.getFeaturedProducts(5);
		List<User> users = userService.searchUsers(null);
		List<Product> lowStockProducts = productService.getLowStockProducts(10);
		List<Product> expiringSoonProducts = productService.getExpiringSoonProducts(30);

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
		model.addAttribute("expiringSoonCount", expiringSoonProducts.size());
		model.addAttribute("products", products);
		model.addAttribute("users", users);
		model.addAttribute("lowStockProducts", lowStockProducts);
		model.addAttribute("expiringSoonProducts", expiringSoonProducts);
		model.addAttribute("lifecycleSessionToken", sessionLifecycleBean.getSessionToken());
		model.addAttribute("lifecycleVisitCount", visitCount);
		model.addAttribute("lifecycleRequestMarker", requestMarker.getMarkerId());

		if (roles.contains(RoleName.CUSTOMER)) {
			return "customer-dashboard";
		}
		return "dashboard";
	}
}
