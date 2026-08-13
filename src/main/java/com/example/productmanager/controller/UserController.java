package com.example.productmanager.controller;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.example.productmanager.model.RoleName;
import com.example.productmanager.model.User;
import com.example.productmanager.service.UserService;

import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/users")
public class UserController {

	private final UserService userService;

	public UserController(UserService userService) {
		this.userService = userService;
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
	public String listUsers(Model model,
			@RequestParam(value = "keyword", required = false) String keyword,
			HttpSession session) {
		if (!hasPermission(session, RoleName.ADMIN, RoleName.MANAGER)) {
			return "redirect:/login";
		}
		model.addAttribute("users", userService.searchUsers(keyword));
		model.addAttribute("keyword", keyword);
		model.addAttribute("roles", RoleName.values());
		model.addAttribute("newUser", new User());
		return "users";
	}

	@PostMapping("/register")
	public String register(@ModelAttribute("newUser") User user, HttpSession session) {
		if (!hasPermission(session, RoleName.ADMIN, RoleName.MANAGER)) {
			return "redirect:/login";
		}
		userService.registerUser(user.getUsername(), user.getPassword(), user.getEmail(), user.getFullName());
		return "redirect:/users";
	}

	@GetMapping("/{id}/edit")
	public String editUser(@PathVariable Long id, Model model, HttpSession session) {
		if (!hasPermission(session, RoleName.ADMIN, RoleName.MANAGER)) {
			return "redirect:/login";
		}
		User user = userService.getUserById(id);
		model.addAttribute("user", user);
		model.addAttribute("users", userService.getAllUsers());
		model.addAttribute("allRoles", RoleName.values());
		model.addAttribute("selectedRoles", user.getRoles().stream().map(role -> role.getName()).collect(Collectors.toSet()));
		return "users";
	}

	@PostMapping("/{id}/roles")
	public String updateUserRoles(@PathVariable Long id,
			@RequestParam(value = "roleNames", required = false) String[] roleNames,
			HttpSession session) {
		if (!hasPermission(session, RoleName.ADMIN, RoleName.MANAGER)) {
			return "redirect:/login";
		}
		Set<RoleName> selectedRoles = Arrays.stream(roleNames == null ? new String[0] : roleNames)
				.map(RoleName::valueOf)
				.collect(Collectors.toSet());
		userService.updateUserRoles(id, selectedRoles);
		return "redirect:/users";
	}
}
