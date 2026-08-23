package com.example.productmanager.controller.support;

import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import com.example.productmanager.model.Role;
import com.example.productmanager.model.RoleName;
import com.example.productmanager.model.User;
import com.example.productmanager.security.SecurityUserPrincipal;

import jakarta.servlet.http.HttpSession;

public abstract class SessionController {

	protected final User getCurrentUser(HttpSession session) {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null || !authentication.isAuthenticated()
				|| authentication instanceof AnonymousAuthenticationToken) {
			return null;
		}
		Object principal = authentication.getPrincipal();
		if (!(principal instanceof SecurityUserPrincipal userPrincipal)) {
			return null;
		}
		Set<Role> roles = userPrincipal.getAuthorities().stream()
				.map(authority -> authority.getAuthority())
				.filter(value -> value.startsWith("ROLE_"))
				.map(value -> value.substring(5))
				.map(RoleName::valueOf)
				.map(roleName -> Role.builder().name(roleName).build())
				.collect(Collectors.toSet());
		return User.builder()
				.id(userPrincipal.getId())
				.username(userPrincipal.getUsername())
				.email(userPrincipal.getEmail())
				.fullName(userPrincipal.getFullName())
				.phoneNumber(userPrincipal.getPhoneNumber())
				.address(userPrincipal.getAddress())
				.avatarUrl(userPrincipal.getAvatarUrl())
				.enabled(userPrincipal.isEnabled())
				.roles(roles)
				.build();
	}

	protected final boolean isAuthenticated(HttpSession session) {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		return authentication != null
				&& authentication.isAuthenticated()
				&& !(authentication instanceof AnonymousAuthenticationToken);
	}

	protected final boolean isGuestSession(HttpSession session) {
		return Boolean.TRUE.equals(session.getAttribute("guestCheckout"));
	}

	protected final Set<RoleName> currentRoleNames(HttpSession session) {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null || !authentication.isAuthenticated()
				|| authentication instanceof AnonymousAuthenticationToken) {
			return Set.of();
		}
		return authentication.getAuthorities().stream()
				.map(authority -> authority.getAuthority())
				.filter(value -> value.startsWith("ROLE_"))
				.map(value -> value.substring(5))
				.map(RoleName::valueOf)
				.collect(Collectors.toSet());
	}

	protected final boolean hasPermission(HttpSession session, RoleName... allowedRoles) {
		Set<RoleName> roleNames = currentRoleNames(session);
		for (RoleName role : allowedRoles) {
			if (roleNames.contains(role)) {
				return true;
			}
		}
		return false;
	}

	protected final boolean hasRole(HttpSession session, RoleName roleName) {
		return currentRoleNames(session).contains(roleName);
	}
}
