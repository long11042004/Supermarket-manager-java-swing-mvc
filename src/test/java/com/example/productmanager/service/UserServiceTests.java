package com.example.productmanager.service;

import java.util.Locale;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.example.productmanager.email.event.UserRegisteredEmailEvent;
import com.example.productmanager.entity.Role;
import com.example.productmanager.entity.RoleName;
import com.example.productmanager.entity.User;
import com.example.productmanager.exception.BadCredentialsException;
import com.example.productmanager.exception.ConflictException;
import com.example.productmanager.i18n.MessageResolver;
import com.example.productmanager.repository.RoleRepository;
import com.example.productmanager.repository.UserActivityRepository;
import com.example.productmanager.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class UserServiceTests {

	@Mock
	private UserRepository userRepository;

	@Mock
	private RoleRepository roleRepository;

	@Mock
	private UserActivityRepository userActivityRepository;

	@Mock
	private PasswordEncoder passwordEncoder;

	@Mock
	private ApplicationEventPublisher applicationEventPublisher;

	private UserService userService;

	private ResourceBundleMessageSource createMessageSource() {
		LocaleContextHolder.setLocale(Locale.forLanguageTag("vi-VN"));
		ResourceBundleMessageSource source = new ResourceBundleMessageSource();
		source.setBasename("messages");
		source.setDefaultEncoding("UTF-8");
		return source;
	}

	@Test
	void registerCustomerShouldAssignCustomerRole() {
		Role customerRole = Role.builder().name(RoleName.CUSTOMER).build();
		userService = new UserService(
			userRepository, 
			roleRepository, 
			userActivityRepository, 
			new MessageResolver(createMessageSource()), 
			passwordEncoder,
			applicationEventPublisher);
		when(userRepository.existsByUsername("new-customer")).thenReturn(false);
		when(userRepository.existsByEmail("customer@example.com")).thenReturn(false);
		when(roleRepository.findByName(RoleName.CUSTOMER)).thenReturn(Optional.of(customerRole));
		when(passwordEncoder.encode("secret123")).thenReturn("encoded-secret123");
		when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

		User savedUser = userService.registerCustomer("new-customer", "secret123", "customer@example.com", "Khách mới");

		assertEquals(RoleName.CUSTOMER, savedUser.getRoles().stream().findFirst().orElseThrow().getName());
		verify(userActivityRepository).save(any());
		verify(applicationEventPublisher).publishEvent(any(UserRegisteredEmailEvent.class));
	}

	@Test
	void registerCustomerShouldRejectBlankUsernameOrEmail() {
		userService = new UserService(
			userRepository,
			roleRepository,
			userActivityRepository,
			new MessageResolver(createMessageSource()),
			passwordEncoder,
			applicationEventPublisher);

		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
				() -> userService.registerCustomer("   ", "secret123", "customer@example.com", "Khách mới"));
		assertEquals("Tên đăng nhập không được để trống", ex.getMessage());
		verify(userRepository, never()).save(any(User.class));
	}

	@Test
	void updateProfileShouldPersistFieldsAndLogActivity() {
		User existingUser = buildExistingUser();
		userService = new UserService(
			userRepository, 
			roleRepository, 
			userActivityRepository, 
			new MessageResolver(createMessageSource()), 
			passwordEncoder,
			applicationEventPublisher);
		when(userRepository.findById(1L)).thenReturn(Optional.of(existingUser));
		when(userRepository.existsByEmailAndIdNot("new@demo.com", 1L)).thenReturn(false);
		when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

		User updatedUser = userService.updateProfile(1L, "Tên mới", "new@demo.com", "0909000000", "Hà Nội");

		assertEquals("Tên mới", updatedUser.getFullName());
		assertEquals("new@demo.com", updatedUser.getEmail());
		assertEquals("0909000000", updatedUser.getPhoneNumber());
		assertEquals("Hà Nội", updatedUser.getAddress());
		verify(userRepository).save(existingUser);
		verify(userActivityRepository).save(any());
	}

	@Test
	void updateProfileShouldRejectDuplicateEmail() {
		User existingUser = buildExistingUser();
		userService = new UserService(
			userRepository, 
			roleRepository, 
			userActivityRepository, 
			new MessageResolver(createMessageSource()), 
			passwordEncoder,
			applicationEventPublisher);
		when(userRepository.findById(1L)).thenReturn(Optional.of(existingUser));
		when(userRepository.existsByEmailAndIdNot("admin@demo.com", 1L)).thenReturn(true);

		ConflictException ex = assertThrows(ConflictException.class,
				() -> userService.updateProfile(1L, "Tên mới", "admin@demo.com", null, null));

		assertEquals("Email đã tồn tại", ex.getMessage());
		verify(userRepository, never()).save(any(User.class));
	}

	@Test
	void changePasswordShouldUpdatePasswordAndLogActivity() {
		User existingUser = buildExistingUser();
		userService = new UserService(
			userRepository, 
			roleRepository, 
			userActivityRepository, 
			new MessageResolver(createMessageSource()), 
			passwordEncoder,
			applicationEventPublisher);
		when(userRepository.findById(1L)).thenReturn(Optional.of(existingUser));
		when(passwordEncoder.matches("old-pass", "old-pass")).thenReturn(true);
		when(passwordEncoder.matches("new-pass-123", "old-pass")).thenReturn(false);
		when(passwordEncoder.encode("new-pass-123")).thenReturn("encoded-new-pass-123");
		when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

		User updatedUser = userService.changePassword(1L, "old-pass", "new-pass-123", "new-pass-123");

		assertEquals("encoded-new-pass-123", updatedUser.getPassword());
		verify(userRepository).save(existingUser);
		verify(userActivityRepository).save(any());
	}

	@Test
	void changePasswordShouldRejectWrongCurrentPassword() {
		User existingUser = buildExistingUser();
		userService = new UserService(
			userRepository, 
			roleRepository, 
			userActivityRepository, 
			new MessageResolver(createMessageSource()), 
			passwordEncoder,
			applicationEventPublisher);
		when(userRepository.findById(1L)).thenReturn(Optional.of(existingUser));
		when(passwordEncoder.matches("wrong-pass", "old-pass")).thenReturn(false);

		BadCredentialsException ex = assertThrows(BadCredentialsException.class,
				() -> userService.changePassword(1L, "wrong-pass", "new-pass-123", "new-pass-123"));

		assertEquals("Mật khẩu hiện tại không đúng", ex.getMessage());
		verify(userRepository, never()).save(any(User.class));
	}

	private User buildExistingUser() {
		return User.builder()
				.id(1L)
				.username("staff")
				.password("old-pass")
				.email("staff@demo.com")
				.fullName("Nhân viên Demo")
				.roles(Set.of(Role.builder().name(RoleName.STAFF).build()))
				.build();
	}
}