package com.example.productmanager.service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import com.example.productmanager.model.Role;
import com.example.productmanager.model.RoleName;
import com.example.productmanager.model.User;
import com.example.productmanager.model.UserActivity;
import com.example.productmanager.repository.RoleRepository;
import com.example.productmanager.repository.UserActivityRepository;
import com.example.productmanager.repository.UserRepository;

import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class UserService {

	private final UserRepository userRepository;
	private final RoleRepository roleRepository;
	private final UserActivityRepository userActivityRepository;
	private final MessageSource messageSource;
	private final PasswordEncoder passwordEncoder;
	private final EmailService emailService;
	private final SpringTemplateEngine templateEngine;

	private String msg(String key, Object... args) {
		return messageSource.getMessage(key, args, LocaleContextHolder.getLocale());
	}

	@Transactional
	public User registerUser(String username, String password, String email, String fullName) {
		return registerUser(username, password, email, fullName, RoleName.STAFF);
	}

	@Transactional
	public User registerCustomer(String username, String password, String email, String fullName) {
		return registerUser(username, password, email, fullName, RoleName.CUSTOMER);
	}

	@Transactional
	public User registerInternalUser(String username,
			String password,
			String email,
			String fullName,
			RoleName defaultRoleName) {
		return registerUser(username, password, email, fullName, defaultRoleName);
	}

	private User registerUser(String username,
			String password,
			String email,
			String fullName,
			RoleName defaultRoleName) {
		if (userRepository.existsByUsername(username)) {
			throw new IllegalArgumentException(msg("err.user.usernameExists"));
		}
		if (userRepository.existsByEmail(email)) {
			throw new IllegalArgumentException(msg("err.user.emailExists"));
		}

		Role defaultRole = roleRepository.findByName(defaultRoleName)
				.orElseThrow(() -> new RuntimeException(msg("err.role.defaultNotFound", defaultRoleName)));

		User user = User.builder()
				.username(username)
				.password(passwordEncoder.encode(password))
				.email(email)
				.fullName(fullName)
				.enabled(true)
				.roles(new HashSet<>(Set.of(defaultRole)))
				.build();

		User savedUser = userRepository.save(user);
		logActivity(savedUser, "Tạo tài khoản", "Tài khoản được khởi tạo trong hệ thống");
		if (savedUser.getEmail() != null && !savedUser.getEmail().isBlank()) {
			String recipientName = savedUser.getFullName() == null ? savedUser.getUsername() : savedUser.getFullName();
			Context context = new Context();
			context.setVariable("recipientName", recipientName);
			context.setVariable("username", savedUser.getUsername());
			String htmlContent = templateEngine.process("email/welcome-account", context);
			emailService.sendHtmlEmail(
					savedUser.getEmail(),
					"Chào mừng bạn đến với hệ thống",
					htmlContent);
		}
		return savedUser;
	}

	@Transactional
	public User login(String username, String password) {
		User user = userRepository.findByUsername(username)
				.orElseThrow(() -> new RuntimeException(msg("err.auth.userNotFound", username)));

		boolean passwordMatched = passwordEncoder.matches(password, user.getPassword()) || user.getPassword().equals(password);
		if (!passwordMatched) {
			throw new IllegalArgumentException(msg("err.auth.invalidPassword"));
		}

		if (!isEncodedPassword(user.getPassword())) {
			user.setPassword(passwordEncoder.encode(password));
			userRepository.save(user);
		}

		if (!user.isEnabled()) {
			throw new IllegalStateException(msg("err.auth.accountLocked"));
		}

		logActivity(user, "Đăng nhập", "Người dùng đăng nhập vào hệ thống");

		return user;
	}

	private boolean isEncodedPassword(String password) {
		if (password == null || password.isBlank()) {
			return false;
		}
		return password.startsWith("{") || password.startsWith("$2a$") || password.startsWith("$2b$") || password.startsWith("$2y$");
	}

	@Transactional(readOnly = true)
	public List<User> getAllUsers() {
		return userRepository.findAll();
	}

	@Transactional(readOnly = true)
	public User getUserById(Long id) {
		return userRepository.findById(id)
				.orElseThrow(() -> new RuntimeException(msg("err.user.notFoundById", id)));
	}

	@Transactional
	public User updateUserRoles(Long userId, Set<RoleName> roleNames) {
		User user = getUserById(userId);
		Set<Role> roles = new HashSet<>();
		for (RoleName roleName : roleNames) {
			Role role = roleRepository.findByName(roleName)
					.orElseThrow(() -> new RuntimeException(msg("err.role.notFound", roleName)));
			roles.add(role);
		}
		user.setRoles(roles);
		User savedUser = userRepository.save(user);
		String details = new StringBuffer("Vai trò hiện tại: ")
				.append(roleNames)
				.toString();
		logActivity(savedUser, "Cập nhật phân quyền", details);
		return savedUser;
	}

	@Transactional(readOnly = true)
	public List<User> searchUsers(String keyword) {
		if (keyword == null || keyword.trim().isEmpty()) {
			return getAllUsers();
		}
		return userRepository.searchByFullName(keyword.trim());
	}

	@Transactional(readOnly = true)
	public List<User> searchUsersForManagement(String keyword, boolean manageableOnly, boolean isAdmin) {
		String normalizedKeyword = keyword == null ? "" : keyword.trim();
		if (isAdmin || !manageableOnly) {
			return userRepository.searchByKeywordExcludingRole(normalizedKeyword, RoleName.CUSTOMER);
		}
		return userRepository.searchByKeywordExcludingRoles(
				normalizedKeyword,
				List.of(RoleName.CUSTOMER, RoleName.ADMIN, RoleName.MANAGER));
	}

	@Transactional
	public User updateProfile(Long userId,
			String fullName,
			String email,
			String phoneNumber,
			String address) {
		User user = getUserById(userId);
		String normalizedEmail = email == null ? "" : email.trim();
		if (normalizedEmail.isEmpty()) {
			throw new IllegalArgumentException(msg("err.user.emailRequired"));
		}
		if (userRepository.existsByEmailAndIdNot(normalizedEmail, userId)) {
			throw new IllegalArgumentException(msg("err.user.emailExists"));
		}

		user.setFullName(fullName == null ? null : fullName.trim());
		user.setEmail(normalizedEmail);
		user.setPhoneNumber(phoneNumber == null ? null : phoneNumber.trim());
		user.setAddress(address == null ? null : address.trim());

		User savedUser = userRepository.save(user);
		logActivity(savedUser, "Cập nhật hồ sơ", "Người dùng đã cập nhật thông tin cá nhân");
		return savedUser;
	}

	@Transactional
	public User changePassword(Long userId, String currentPassword, String newPassword, String confirmPassword) {
		User user = getUserById(userId);
		if (!user.getPassword().equals(currentPassword)) {
			throw new IllegalArgumentException(msg("err.user.currentPasswordInvalid"));
		}
		if (newPassword == null || newPassword.length() < 6) {
			throw new IllegalArgumentException(msg("err.user.newPasswordMin"));
		}
		if (!newPassword.equals(confirmPassword)) {
			throw new IllegalArgumentException(msg("err.user.passwordConfirmMismatch"));
		}
		if (newPassword.equals(currentPassword)) {
			throw new IllegalArgumentException(msg("err.user.passwordMustDiffer"));
		}

		user.setPassword(newPassword);
		User savedUser = userRepository.save(user);
		logActivity(savedUser, "Đổi mật khẩu", "Người dùng đã thay đổi mật khẩu");
		return savedUser;
	}

	@Transactional
	public User updateAvatar(Long userId, String avatarUrl) {
		User user = getUserById(userId);
		String normalizedAvatarUrl = avatarUrl == null ? null : avatarUrl.trim();
		user.setAvatarUrl((normalizedAvatarUrl == null || normalizedAvatarUrl.isEmpty()) ? null : normalizedAvatarUrl);
		User savedUser = userRepository.save(user);
		logActivity(savedUser, "Cập nhật avatar", "Người dùng đã cập nhật ảnh đại diện");
		return savedUser;
	}

	@Transactional
	public void logLogout(Long userId) {
		User user = getUserById(userId);
		logActivity(user, "Đăng xuất", "Người dùng đăng xuất khỏi hệ thống");
	}

	@Transactional
	public void recordActivity(Long userId, String action, String details) {
		User user = getUserById(userId);
		logActivity(user, action, details);
	}

	@Transactional(readOnly = true)
	public List<UserActivity> getRecentActivities(Long userId) {
		return userActivityRepository.findRecentActivitiesByUserId(userId, PageRequest.of(0, 10));
	}

	private void logActivity(User user, String action, String details) {
		userActivityRepository.save(UserActivity.builder()
				.user(user)
				.action(action)
				.details(details)
				.build());
	}
}
