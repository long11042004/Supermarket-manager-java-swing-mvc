package com.example.productmanager.controller;

import java.util.Locale;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import com.example.productmanager.model.Role;
import com.example.productmanager.model.RoleName;
import com.example.productmanager.model.User;
import com.example.productmanager.service.UserService;

class UserControllerTests {

	private final UserService userService = org.mockito.Mockito.mock(UserService.class);
	private final ResourceBundleMessageSource messageSource = createMessageSource();
	private final UserController userController = new UserController(userService, messageSource);

	private ResourceBundleMessageSource createMessageSource() {
		LocaleContextHolder.setLocale(Locale.forLanguageTag("vi-VN"));
		ResourceBundleMessageSource source = new ResourceBundleMessageSource();
		source.setBasename("messages");
		source.setDefaultEncoding("UTF-8");
		return source;
	}

	@Test
	void managerCannotUpdateAdminRoles() {
		MockHttpSession session = new MockHttpSession();
		session.setAttribute("loggedInUser", buildUser(10L, "manager", Set.of(RoleName.MANAGER)));
		when(userService.getUserById(1L)).thenReturn(buildUser(1L, "admin", Set.of(RoleName.ADMIN)));

		RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();
		String view = userController.updateUserRoles(1L, new String[] { "STAFF" }, session, redirectAttributes);

		assertEquals("redirect:/users", view);
		assertEquals("Bạn không có quyền cập nhật tài khoản này.", redirectAttributes.getFlashAttributes().get("errorMessage"));
		verify(userService, never()).updateUserRoles(eq(1L), anySet());
	}

	@Test
	void managerCannotCreateManagerAccount() {
		MockHttpSession session = new MockHttpSession();
		session.setAttribute("loggedInUser", buildUser(10L, "manager", Set.of(RoleName.MANAGER)));

		User newUser = User.builder()
				.username("new-manager")
				.password("secret123")
				.email("new-manager@demo.com")
				.fullName("Manager mới")
				.build();
		RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

		String view = userController.register(newUser, "MANAGER", session, redirectAttributes);

		assertEquals("redirect:/users", view);
		assertEquals("Bạn không có quyền tạo tài khoản với vai trò này.", redirectAttributes.getFlashAttributes().get("errorMessage"));
		verify(userService, never()).registerInternalUser(eq("new-manager"), eq("secret123"), eq("new-manager@demo.com"), eq("Manager mới"), eq(RoleName.MANAGER));
	}

	@Test
	void adminCanAssignManagerRole() {
		MockHttpSession session = new MockHttpSession();
		session.setAttribute("loggedInUser", buildUser(99L, "admin", Set.of(RoleName.ADMIN)));
		when(userService.getUserById(2L)).thenReturn(buildUser(2L, "staff", Set.of(RoleName.STAFF)));

		RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();
		String view = userController.updateUserRoles(2L, new String[] { "MANAGER" }, session, redirectAttributes);

		assertEquals("redirect:/users", view);
		assertEquals("Cập nhật phân quyền thành công.", redirectAttributes.getFlashAttributes().get("successMessage"));
		verify(userService).updateUserRoles(eq(2L), eq(Set.of(RoleName.MANAGER)));
	}

	private User buildUser(Long id, String username, Set<RoleName> roleNames) {
		Set<Role> roles = roleNames.stream()
				.map(roleName -> Role.builder().name(roleName).build())
				.collect(java.util.stream.Collectors.toSet());
		return User.builder()
				.id(id)
				.username(username)
				.password("secret")
				.email(username + "@demo.com")
				.roles(roles)
				.enabled(true)
				.build();
	}
}
