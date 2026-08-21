package com.example.productmanager.controller;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.productmanager.controller.support.SessionController;
import com.example.productmanager.i18n.MessageResolver;
import com.example.productmanager.lifecycle.PrototypeRequestMarker;
import com.example.productmanager.lifecycle.SessionLifecycleBean;
import com.example.productmanager.model.RoleName;
import com.example.productmanager.model.User;
import com.example.productmanager.service.UserService;

import jakarta.servlet.http.HttpSession;
import lombok.AllArgsConstructor;

@Controller
@RequestMapping("/users")
@AllArgsConstructor
public class UserController extends SessionController {

	private final UserService userService;
	private final MessageResolver messageResolver;
	private final SessionLifecycleBean sessionLifecycleBean;
	private final ObjectProvider<PrototypeRequestMarker> prototypeRequestMarkerProvider;

	private boolean isAdmin(HttpSession session) {
		return hasRole(session, RoleName.ADMIN);
	}

	private boolean canEditTargetUser(HttpSession session, User targetUser) {
		Set<RoleName> actorRoles = currentRoleNames(session);
		Set<RoleName> targetRoles = targetUser.getRoles() == null ? Set.of() : targetUser.getRoles().stream()
				.map(role -> role.getName())
				.collect(Collectors.toSet());

		if (actorRoles.contains(RoleName.ADMIN)) {
			return true;
		}
		if (actorRoles.contains(RoleName.MANAGER)) {
			return !targetRoles.contains(RoleName.ADMIN) && !targetRoles.contains(RoleName.MANAGER);
		}
		return false;
	}

	private List<RoleName> editableRoles(HttpSession session) {
		if (isAdmin(session)) {
			return Arrays.asList(RoleName.values());
		}
		return List.of(RoleName.STAFF, RoleName.CUSTOMER);
	}

	private String restrictedReason(HttpSession session) {
		if (isAdmin(session)) {
			return messageResolver.msg("msg.user.restrictedReasonAdmin");
		}
		return messageResolver.msg("msg.user.restrictedReasonManager");
	}

	private Map<Long, Set<RoleName>> assignedRolesByUserId(List<User> users) {
		Map<Long, Set<RoleName>> assignedRoles = new HashMap<>();
		for (User user : users) {
			Set<RoleName> roleNames = user.getRoles() == null ? Set.of() : user.getRoles().stream()
					.map(role -> role.getName())
					.collect(Collectors.toSet());
			assignedRoles.put(user.getId(), roleNames);
		}
		return assignedRoles;
	}

	@GetMapping
	public String listUsers(Model model,
			@RequestParam(value = "keyword", required = false) String keyword,
			@RequestParam(value = "manageableOnly", required = false, defaultValue = "false") boolean manageableOnly,
			HttpSession session) {
		if (!hasPermission(session, RoleName.ADMIN, RoleName.MANAGER)) {
			return "redirect:/login";
		}
		PrototypeRequestMarker requestMarker = prototypeRequestMarkerProvider.getObject();
		int visitCount = sessionLifecycleBean.increaseAndGetVisitCount();
		List<User> users = userService.searchUsersForManagement(keyword, manageableOnly, isAdmin(session));
		Set<Long> restrictedUserIds = users.stream()
				.filter(user -> !canEditTargetUser(session, user))
				.map(user -> user.getId())
				.collect(Collectors.toSet());
		Map<Long, String> restrictedReasons = new HashMap<>();
		for (User user : users) {
			if (restrictedUserIds.contains(user.getId())) {
				restrictedReasons.put(user.getId(), restrictedReason(session));
			}
		}

		model.addAttribute("users", users);
		model.addAttribute("keyword", keyword);
		model.addAttribute("manageableOnly", manageableOnly);
		model.addAttribute("roles", editableRoles(session));
		model.addAttribute("registerRoles", editableRoles(session));
		model.addAttribute("isAdmin", isAdmin(session));
		model.addAttribute("restrictedUserIds", restrictedUserIds);
		model.addAttribute("restrictedReasons", restrictedReasons);
		model.addAttribute("assignedRoles", assignedRolesByUserId(users));
		model.addAttribute("newUser", new User());
		model.addAttribute("lifecycleSessionToken", sessionLifecycleBean.getSessionToken());
		model.addAttribute("lifecycleVisitCount", visitCount);
		model.addAttribute("lifecycleRequestMarker", requestMarker.getMarkerId());
		return "users";
	}

	@PostMapping("/register")
	public String register(@ModelAttribute("newUser") User user,
			@RequestParam(value = "defaultRole", required = false) String defaultRole,
			HttpSession session,
			RedirectAttributes redirectAttributes) {
		if (!hasPermission(session, RoleName.ADMIN, RoleName.MANAGER)) {
			return "redirect:/login";
		}

		RoleName selectedRole = RoleName.STAFF;
		if (defaultRole != null && !defaultRole.isBlank()) {
			selectedRole = RoleName.valueOf(defaultRole);
		}

		Set<RoleName> allowedCreateRoles = isAdmin(session)
				? EnumSet.of(RoleName.MANAGER, RoleName.STAFF, RoleName.CUSTOMER)
				: EnumSet.of(RoleName.STAFF, RoleName.CUSTOMER);
		if (!allowedCreateRoles.contains(selectedRole)) {
			redirectAttributes.addFlashAttribute("errorMessage", messageResolver.msg("msg.user.noPermissionCreateRole"));
			return "redirect:/users";
		}

		User createdUser = userService.registerInternalUser(
				user.getUsername(),
				user.getPassword(),
				user.getEmail(),
				user.getFullName(),
				selectedRole);
		redirectAttributes.addFlashAttribute("successMessage", messageResolver.msg("msg.user.created"));
		redirectAttributes.addFlashAttribute("previewUrl", "/mail-preview/welcome-account/" + createdUser.getId());
		return "redirect:/users";
	}

	@GetMapping("/{id}/edit")
	public String editUser(@PathVariable Long id, Model model, HttpSession session) {
		if (!hasPermission(session, RoleName.ADMIN, RoleName.MANAGER)) {
			return "redirect:/login";
		}
		PrototypeRequestMarker requestMarker = prototypeRequestMarkerProvider.getObject();
		int visitCount = sessionLifecycleBean.increaseAndGetVisitCount();
		User user = userService.getUserById(id);
		if (!canEditTargetUser(session, user)) {
			return "redirect:/users";
		}
		List<User> users = userService.searchUsersForManagement(null, false, isAdmin(session));
		Set<Long> restrictedUserIds = users.stream()
				.filter(item -> !canEditTargetUser(session, item))
				.map(item -> item.getId())
				.collect(Collectors.toSet());

		model.addAttribute("user", user);
		model.addAttribute("users", users);
		model.addAttribute("manageableOnly", false);
		model.addAttribute("roles", editableRoles(session));
		model.addAttribute("registerRoles", editableRoles(session));
		model.addAttribute("isAdmin", isAdmin(session));
		model.addAttribute("restrictedUserIds", restrictedUserIds);
		model.addAttribute("assignedRoles", assignedRolesByUserId(users));
		Map<Long, String> restrictedReasons = new HashMap<>();
		for (User item : users) {
			if (restrictedUserIds.contains(item.getId())) {
				restrictedReasons.put(item.getId(), restrictedReason(session));
			}
		}
		model.addAttribute("restrictedReasons", restrictedReasons);
		model.addAttribute("selectedRoles", user.getRoles().stream().map(role -> role.getName()).collect(Collectors.toSet()));
		model.addAttribute("newUser", new User());
		model.addAttribute("lifecycleSessionToken", sessionLifecycleBean.getSessionToken());
		model.addAttribute("lifecycleVisitCount", visitCount);
		model.addAttribute("lifecycleRequestMarker", requestMarker.getMarkerId());
		return "users";
	}

	@PostMapping("/{id}/roles")
	public String updateUserRoles(@PathVariable Long id,
			@RequestParam(value = "roleNames", required = false) String[] roleNames,
			HttpSession session,
			RedirectAttributes redirectAttributes) {
		if (!hasPermission(session, RoleName.ADMIN, RoleName.MANAGER)) {
			return "redirect:/login";
		}

		User targetUser = userService.getUserById(id);
		if (!canEditTargetUser(session, targetUser)) {
			redirectAttributes.addFlashAttribute("errorMessage", messageResolver.msg("msg.user.noPermissionUpdateAccount"));
			return "redirect:/users";
		}

		Set<RoleName> selectedRoles = Arrays.stream(roleNames == null ? new String[0] : roleNames)
				.map(RoleName::valueOf)
				.collect(Collectors.toSet());
		if (selectedRoles.isEmpty()) {
			redirectAttributes.addFlashAttribute("errorMessage", messageResolver.msg("msg.user.mustHaveAtLeastOneRole"));
			return "redirect:/users";
		}

		Set<RoleName> allowedRoles = isAdmin(session)
				? EnumSet.allOf(RoleName.class)
				: EnumSet.of(RoleName.STAFF, RoleName.CUSTOMER);
		if (!allowedRoles.containsAll(selectedRoles)) {
			redirectAttributes.addFlashAttribute("errorMessage", messageResolver.msg("msg.user.roleAssignmentNotAllowed"));
			return "redirect:/users";
		}

		userService.updateUserRoles(id, selectedRoles);
		redirectAttributes.addFlashAttribute("successMessage", messageResolver.msg("msg.user.rolesUpdated"));
		return "redirect:/users";
	}
}
