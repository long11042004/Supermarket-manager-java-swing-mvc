package com.example.productmanager.controller;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.productmanager.controller.support.SessionController;
import com.example.productmanager.entity.User;
import com.example.productmanager.entity.UserActivity;
import com.example.productmanager.lifecycle.PrototypeRequestMarker;
import com.example.productmanager.lifecycle.SessionLifecycleBean;
import com.example.productmanager.multilanguage.MessageResolver;
import com.example.productmanager.service.UserService;

import jakarta.servlet.http.HttpSession;
import lombok.AllArgsConstructor;

@Controller
@RequestMapping("/profile")
@AllArgsConstructor
public class ProfileController extends SessionController {

	private final UserService userService;
	private final MessageResolver messageResolver;
	private final SessionLifecycleBean sessionLifecycleBean;
	private final ObjectProvider<PrototypeRequestMarker> prototypeRequestMarkerProvider;

	@GetMapping
	public String profile(Model model,
			@RequestParam(value = "page", defaultValue = "0") int page,
			@RequestParam(value = "size", defaultValue = "10") int size,
			HttpSession session) {
		User currentUser = getAuthenticatedUser(session);
		if (currentUser == null) {
			return "redirect:/login";
		}

		User freshUser = userService.getUserById(currentUser.getId());
		PrototypeRequestMarker requestMarker = prototypeRequestMarkerProvider.getObject();
		int visitCount = sessionLifecycleBean.increaseAndGetVisitCount();
		Page<UserActivity> activitiesPage = userService.getActivities(freshUser.getId(), page, size);
		model.addAttribute("currentUser", freshUser);
		model.addAttribute("activities", activitiesPage.getContent());
		model.addAttribute("currentPage", activitiesPage.getNumber());
		model.addAttribute("totalPages", activitiesPage.getTotalPages());
		model.addAttribute("pageSize", activitiesPage.getSize());
		model.addAttribute("lifecycleSessionToken", sessionLifecycleBean.getSessionToken());
		model.addAttribute("lifecycleVisitCount", visitCount);
		model.addAttribute("lifecycleRequestMarker", requestMarker.getMarkerId());
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
			userService.updateProfile(currentUser.getId(), fullName, email, phoneNumber, address);
			redirectAttributes.addFlashAttribute("successMessage", messageResolver.msg("msg.profile.updated"));
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
			userService.changePassword(currentUser.getId(), currentPassword, newPassword, confirmPassword);
			redirectAttributes.addFlashAttribute("successMessage", messageResolver.msg("msg.profile.passwordChanged"));
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

		userService.updateAvatar(currentUser.getId(), avatarUrl);
		redirectAttributes.addFlashAttribute("successMessage", messageResolver.msg("msg.profile.avatarUpdated"));
		return "redirect:/profile";
	}

	private User getAuthenticatedUser(HttpSession session) {
		return getCurrentUser(session);
	}
}