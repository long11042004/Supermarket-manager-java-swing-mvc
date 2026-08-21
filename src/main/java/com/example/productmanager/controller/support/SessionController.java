package com.example.productmanager.controller.support;

import java.util.Set;
import java.util.stream.Collectors;

import com.example.productmanager.model.RoleName;
import com.example.productmanager.model.User;

import jakarta.servlet.http.HttpSession;

public abstract class SessionController {

	protected final User getCurrentUser(HttpSession session) {
		return (User) session.getAttribute("loggedInUser");
	}

	protected final boolean isAuthenticated(HttpSession session) {
		return getCurrentUser(session) != null;
	}

	protected final boolean isGuestSession(HttpSession session) {
		return Boolean.TRUE.equals(session.getAttribute("guestCheckout"));
	}

	protected final Set<RoleName> currentRoleNames(HttpSession session) {
		User currentUser = getCurrentUser(session);
		if (currentUser == null || currentUser.getRoles() == null) {
			return Set.of();
		}
		return currentUser.getRoles().stream()
				.map(role -> role.getName())
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
