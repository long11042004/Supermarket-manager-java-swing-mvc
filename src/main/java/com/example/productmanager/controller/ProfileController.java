package com.example.productmanager.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.productmanager.model.User;
import com.example.productmanager.service.UserService;

import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/profile")
public class ProfileController {

	private final UserService userService;

	public ProfileController(UserService userService) {
		this.userService = userService;
	}

	@GetMapping
	public String profile(Model model, HttpSession session) {
		User currentUser = getAuthenticatedUser(session);
		if (currentUser == null) {
			return "redirect:/login";
		}

		User freshUser = userService.getUserById(currentUser.getId());
		session.setAttribute("loggedInUser", freshUser);
		model.addAttribute("currentUser", freshUser);
		model.addAttribute("activities", userService.getRecentActivities(freshUser.getId()));
		return "profile";
	}

	@PostMapping("/update")
	public String updateProfile(@RequestParam String fullName,
			@RequestParam String email,
			@RequestParam(required = false) String phoneNumber,
			@RequestParam(required = false) String address,
			HttpSession session,
			RedirectAttributes redirectAttributes) {
		User currentUser = getAuthenticatedUser(session);
		if (currentUser == null) {
			return "redirect:/login";
		}

		try {
			User updatedUser = userService.updateProfile(currentUser.getId(), fullName, email, phoneNumber, address);
			session.setAttribute("loggedInUser", updatedUser);
			redirectAttributes.addFlashAttribute("successMessage", "Thông tin cá nhân đã được cập nhật.");
		} catch (IllegalArgumentException ex) {
			redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
		}
		return "redirect:/profile";
	}

	@PostMapping("/password")
	public String changePassword(@RequestParam String currentPassword,
			@RequestParam String newPassword,
			@RequestParam String confirmPassword,
			HttpSession session,
			RedirectAttributes redirectAttributes) {
		User currentUser = getAuthenticatedUser(session);
		if (currentUser == null) {
			return "redirect:/login";
		}

		try {
			User updatedUser = userService.changePassword(currentUser.getId(), currentPassword, newPassword, confirmPassword);
			session.setAttribute("loggedInUser", updatedUser);
			redirectAttributes.addFlashAttribute("successMessage", "Mật khẩu đã được thay đổi.");
		} catch (IllegalArgumentException ex) {
			redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
		}
		return "redirect:/profile";
	}

	@PostMapping("/avatar")
	public String updateAvatar(@RequestParam(required = false) String avatarUrl,
			HttpSession session,
			RedirectAttributes redirectAttributes) {
		User currentUser = getAuthenticatedUser(session);
		if (currentUser == null) {
			return "redirect:/login";
		}

		User updatedUser = userService.updateAvatar(currentUser.getId(), avatarUrl);
		session.setAttribute("loggedInUser", updatedUser);
		redirectAttributes.addFlashAttribute("successMessage", "Avatar đã được cập nhật.");
		return "redirect:/profile";
	}

	private User getAuthenticatedUser(HttpSession session) {
		return (User) session.getAttribute("loggedInUser");
	}
}