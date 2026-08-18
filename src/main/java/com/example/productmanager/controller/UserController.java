package com.example.productmanager.controller;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

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

	private Set<RoleName> currentUserRoles(HttpSession session) {
		if (!isAuthenticated(session)) {
			return Set.of();
		}
		User currentUser = (User) session.getAttribute("loggedInUser");
		return currentUser.getRoles() == null ? Set.of() : currentUser.getRoles().stream()
				.map(role -> role.getName())
				.collect(Collectors.toSet());
	}

	private boolean isAdmin(HttpSession session) {
		return currentUserRoles(session).contains(RoleName.ADMIN);
	}

	private boolean canEditTargetUser(HttpSession session, User targetUser) {
		Set<RoleName> actorRoles = currentUserRoles(session);
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
			return "Tài khoản này nằm ngoài phạm vi thao tác hiện tại.";
		}
		return "Manager chỉ có thể chỉnh sửa tài khoản thuộc nhóm STAFF hoặc CUSTOMER.";
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
		List<User> users = userService.searchUsers(keyword);
		if (manageableOnly) {
			users = users.stream()
					.filter(user -> canEditTargetUser(session, user))
					.toList();
		}
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
			redirectAttributes.addFlashAttribute("errorMessage", "Bạn không có quyền tạo tài khoản với vai trò này.");
			return "redirect:/users";
		}

		userService.registerInternalUser(user.getUsername(), user.getPassword(), user.getEmail(), user.getFullName(), selectedRole);
		redirectAttributes.addFlashAttribute("successMessage", "Tạo người dùng thành công.");
		return "redirect:/users";
	}

	@GetMapping("/{id}/edit")
	public String editUser(@PathVariable Long id, Model model, HttpSession session) {
		if (!hasPermission(session, RoleName.ADMIN, RoleName.MANAGER)) {
			return "redirect:/login";
		}
		User user = userService.getUserById(id);
		if (!canEditTargetUser(session, user)) {
			return "redirect:/users";
		}
		List<User> users = userService.getAllUsers();
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
			redirectAttributes.addFlashAttribute("errorMessage", "Bạn không có quyền cập nhật tài khoản này.");
			return "redirect:/users";
		}

		Set<RoleName> selectedRoles = Arrays.stream(roleNames == null ? new String[0] : roleNames)
				.map(RoleName::valueOf)
				.collect(Collectors.toSet());
		if (selectedRoles.isEmpty()) {
			redirectAttributes.addFlashAttribute("errorMessage", "Người dùng phải có ít nhất một vai trò.");
			return "redirect:/users";
		}

		Set<RoleName> allowedRoles = isAdmin(session)
				? EnumSet.allOf(RoleName.class)
				: EnumSet.of(RoleName.STAFF, RoleName.CUSTOMER);
		if (!allowedRoles.containsAll(selectedRoles)) {
			redirectAttributes.addFlashAttribute("errorMessage", "Bạn chỉ được phép gán vai trò phù hợp với quyền hiện tại.");
			return "redirect:/users";
		}

		userService.updateUserRoles(id, selectedRoles);
		redirectAttributes.addFlashAttribute("successMessage", "Cập nhật phân quyền thành công.");
		return "redirect:/users";
	}
}
