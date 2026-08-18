package com.example.productmanager.service;

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

import com.example.productmanager.model.Role;
import com.example.productmanager.model.RoleName;
import com.example.productmanager.model.User;
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

	private UserService userService;

	@Test
	void updateProfileShouldPersistFieldsAndLogActivity() {
		User existingUser = buildExistingUser();
		userService = new UserService(userRepository, roleRepository, userActivityRepository);
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
		userService = new UserService(userRepository, roleRepository, userActivityRepository);
		when(userRepository.findById(1L)).thenReturn(Optional.of(existingUser));
		when(userRepository.existsByEmailAndIdNot("admin@demo.com", 1L)).thenReturn(true);

		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
				() -> userService.updateProfile(1L, "Tên mới", "admin@demo.com", null, null));

		assertEquals("Email đã tồn tại", ex.getMessage());
		verify(userRepository, never()).save(any(User.class));
	}

	@Test
	void changePasswordShouldUpdatePasswordAndLogActivity() {
		User existingUser = buildExistingUser();
		userService = new UserService(userRepository, roleRepository, userActivityRepository);
		when(userRepository.findById(1L)).thenReturn(Optional.of(existingUser));
		when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

		User updatedUser = userService.changePassword(1L, "old-pass", "new-pass-123", "new-pass-123");

		assertEquals("new-pass-123", updatedUser.getPassword());
		verify(userRepository).save(existingUser);
		verify(userActivityRepository).save(any());
	}

	@Test
	void changePasswordShouldRejectWrongCurrentPassword() {
		User existingUser = buildExistingUser();
		userService = new UserService(userRepository, roleRepository, userActivityRepository);
		when(userRepository.findById(1L)).thenReturn(Optional.of(existingUser));

		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
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