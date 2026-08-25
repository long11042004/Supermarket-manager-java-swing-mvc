package com.example.productmanager.service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.productmanager.email.event.UserRegisteredEmailEvent;
import com.example.productmanager.entity.Role;
import com.example.productmanager.entity.RoleName;
import com.example.productmanager.entity.User;
import com.example.productmanager.entity.UserActivity;
import com.example.productmanager.exception.BadCredentialsException;
import com.example.productmanager.exception.ConflictException;
import com.example.productmanager.exception.ForbiddenException;
import com.example.productmanager.exception.NotFoundException;
import com.example.productmanager.multilanguage.MessageResolver;
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
	private final MessageResolver messageResolver;
	private final PasswordEncoder passwordEncoder;
	private final ApplicationEventPublisher applicationEventPublisher;

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
		String normalizedUsername = normalizeRequiredText(username, messageResolver.msg("err.user.usernameRequired"));
		String normalizedEmail = normalizeRequiredText(email, messageResolver.msg("err.user.emailRequired"));
		String normalizedPassword = normalizeRequiredText(password, messageResolver.msg("err.user.passwordRequired"));
		if (normalizedPassword.length() < 6) {
			throw new IllegalArgumentException(messageResolver.msg("err.user.newPasswordMin"));
		}
		if (!normalizedEmail.contains("@")) {
			throw new IllegalArgumentException(messageResolver.msg("err.user.emailInvalid"));
		}
		if (userRepository.existsByUsername(normalizedUsername)) {
			throw new ConflictException(messageResolver.msg("err.user.usernameExists"));
		}
		if (userRepository.existsByEmail(normalizedEmail)) {
			throw new ConflictException(messageResolver.msg("err.user.emailExists"));
		}

		Role defaultRole = roleRepository.findByName(defaultRoleName)
				.orElseThrow(() -> new NotFoundException(messageResolver.msg("err.role.defaultNotFound", defaultRoleName)));

		User user = User.builder()
				.username(normalizedUsername)
				.password(passwordEncoder.encode(normalizedPassword))
				.email(normalizedEmail)
				.fullName(fullName == null ? null : fullName.trim())
				.enabled(true)
				.roles(new HashSet<>(Set.of(defaultRole)))
				.build();

		User savedUser = userRepository.save(user);
		logActivity(savedUser, messageResolver.msg("activity.account.created"), messageResolver.msg("activity.account.createdDetail"));
		if (savedUser.getEmail() != null && !savedUser.getEmail().isBlank()) {
			String recipientName = savedUser.getFullName() == null ? savedUser.getUsername() : savedUser.getFullName();
			applicationEventPublisher.publishEvent(new UserRegisteredEmailEvent(
					savedUser.getEmail(),
					recipientName,
					savedUser.getUsername()));
		}
		return savedUser;
	}

	@Transactional
	public User login(String username, String password) {
		User user = userRepository.findByUsername(username)
				.orElseThrow(() -> new BadCredentialsException(messageResolver.msg("err.auth.userNotFound", username)));

		boolean passwordMatched = passwordEncoder.matches(password, user.getPassword());
		if (!passwordMatched) {
			throw new BadCredentialsException(messageResolver.msg("err.auth.invalidPassword"));
		}

		if (!user.isEnabled()) {
			throw new ForbiddenException(messageResolver.msg("err.auth.accountLocked"));
		}

		logActivity(user, messageResolver.msg("activity.auth.login"), messageResolver.msg("activity.auth.loginDetail"));

		return user;
	}

	@Transactional(readOnly = true)
	public User getUserById(Long id) {
		return userRepository.findById(id)
				.orElseThrow(() -> new NotFoundException(messageResolver.msg("err.user.notFoundById", id)));
	}

	@Transactional
	public User updateUserRoles(Long userId, Set<RoleName> roleNames) {
		User user = getUserById(userId);
		Set<Role> roles = new HashSet<>();
		for (RoleName roleName : roleNames) {
			Role role = roleRepository.findByName(roleName)
					.orElseThrow(() -> new NotFoundException(messageResolver.msg("err.role.notFound", roleName)));
			roles.add(role);
		}
		user.setRoles(roles);
		User savedUser = userRepository.save(user);
		String details = new StringBuffer(messageResolver.msg("activity.role.currentRoles"))
				.append(": ")
				.append(roleNames)
				.toString();
		logActivity(savedUser, messageResolver.msg("activity.role.updated"), details);
		return savedUser;
	}

	@Transactional(readOnly = true)
	public List<User> searchUsers(String keyword) {
		String normalizedKeyword = keyword == null ? "" : keyword.trim();
		return userRepository.searchByFullName(normalizedKeyword);
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
			throw new IllegalArgumentException(messageResolver.msg("err.user.emailRequired"));
		}
		if (!normalizedEmail.contains("@")) {
			throw new IllegalArgumentException(messageResolver.msg("err.user.emailInvalid"));
		}
		if (userRepository.existsByEmailAndIdNot(normalizedEmail, userId)) {
			throw new ConflictException(messageResolver.msg("err.user.emailExists"));
		}

		user.setFullName(fullName == null ? null : fullName.trim());
		user.setEmail(normalizedEmail);
		user.setPhoneNumber(phoneNumber == null ? null : phoneNumber.trim());
		user.setAddress(address == null ? null : address.trim());

		User savedUser = userRepository.save(user);
		logActivity(savedUser, messageResolver.msg("activity.profile.updated"), messageResolver.msg("activity.profile.updatedDetail"));
		return savedUser;
	}

	@Transactional
	public User changePassword(Long userId, String currentPassword, String newPassword, String confirmPassword) {
		User user = getUserById(userId);
		boolean currentPasswordMatched = passwordEncoder.matches(currentPassword, user.getPassword());
		if (!currentPasswordMatched) {
			throw new BadCredentialsException(messageResolver.msg("err.user.currentPasswordInvalid"));
		}
		if (newPassword == null || newPassword.length() < 6) {
			throw new IllegalArgumentException(messageResolver.msg("err.user.newPasswordMin"));
		}
		if (!newPassword.equals(confirmPassword)) {
			throw new IllegalArgumentException(messageResolver.msg("err.user.passwordConfirmMismatch"));
		}
		if (passwordEncoder.matches(newPassword, user.getPassword())) {
			throw new IllegalArgumentException(messageResolver.msg("err.user.passwordMustDiffer"));
		}

		user.setPassword(passwordEncoder.encode(newPassword));
		User savedUser = userRepository.save(user);
		logActivity(savedUser, messageResolver.msg("activity.password.changed"), messageResolver.msg("activity.password.changedDetail"));
		return savedUser;
	}

	@Transactional
	public User updateAvatar(Long userId, String avatarUrl) {
		User user = getUserById(userId);
		String normalizedAvatarUrl = avatarUrl == null ? null : avatarUrl.trim();
		user.setAvatarUrl((normalizedAvatarUrl == null || normalizedAvatarUrl.isEmpty()) ? null : normalizedAvatarUrl);
		User savedUser = userRepository.save(user);
		logActivity(savedUser, messageResolver.msg("activity.avatar.updated"), messageResolver.msg("activity.avatar.updatedDetail"));
		return savedUser;
	}

	@Transactional
	public void logLogout(Long userId) {
		User user = getUserById(userId);
		logActivity(user, messageResolver.msg("activity.auth.logout"), messageResolver.msg("activity.auth.logoutDetail"));
	}

	@Transactional
	public void recordActivity(Long userId, String action, String details) {
		User user = getUserById(userId);
		logActivity(user, action, details);
	}

	@Transactional(readOnly = true)
	public Page<UserActivity> getActivities(Long userId, int page, int size) {
		int normalizedPage = Math.max(page, 0);
		int normalizedSize = (size == 5 || size == 10 || size == 20) ? size : 10;
		return userActivityRepository.findRecentActivitiesByUserId(userId, PageRequest.of(normalizedPage, normalizedSize));
	}

	private void logActivity(User user, String action, String details) {
		userActivityRepository.save(UserActivity.builder()
				.user(user)
				.action(action)
				.details(details)
				.build());
	}

	private String normalizeRequiredText(String value, String errorMessage) {
		if (value == null || value.trim().isEmpty()) {
			throw new IllegalArgumentException(errorMessage);
		}
		return value.trim();
	}
}
