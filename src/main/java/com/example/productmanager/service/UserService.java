package com.example.productmanager.service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.productmanager.model.Role;
import com.example.productmanager.model.RoleName;
import com.example.productmanager.model.User;
import com.example.productmanager.model.UserActivity;
import com.example.productmanager.repository.RoleRepository;
import com.example.productmanager.repository.UserActivityRepository;
import com.example.productmanager.repository.UserRepository;

@Service
public class UserService {

	private final UserRepository userRepository;
	private final RoleRepository roleRepository;
	private final UserActivityRepository userActivityRepository;

	public UserService(UserRepository userRepository,
			RoleRepository roleRepository,
			UserActivityRepository userActivityRepository) {
		this.userRepository = userRepository;
		this.roleRepository = roleRepository;
		this.userActivityRepository = userActivityRepository;
	}

	@Transactional
	public User registerUser(String username, String password, String email, String fullName) {
		if (userRepository.existsByUsername(username)) {
			throw new IllegalArgumentException("Tên đăng nhập đã tồn tại");
		}
		if (userRepository.existsByEmail(email)) {
			throw new IllegalArgumentException("Email đã tồn tại");
		}

		Role defaultRole = roleRepository.findByName(RoleName.STAFF)
				.orElseThrow(() -> new RuntimeException("Không tìm thấy quyền mặc định: STAFF"));

		User user = User.builder()
				.username(username)
				.password(password)
				.email(email)
				.fullName(fullName)
				.enabled(true)
				.roles(new HashSet<>(Set.of(defaultRole)))
				.build();

		User savedUser = userRepository.save(user);
		logActivity(savedUser, "Tạo tài khoản", "Tài khoản được khởi tạo trong hệ thống");
		return savedUser;
	}

	@Transactional
	public User login(String username, String password) {
		User user = userRepository.findByUsername(username)
				.orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng: " + username));

		if (!user.getPassword().equals(password)) {
			throw new IllegalArgumentException("Mật khẩu không đúng");
		}

		if (!user.isEnabled()) {
			throw new IllegalStateException("Tài khoản đang bị khóa");
		}

		logActivity(user, "Đăng nhập", "Người dùng đăng nhập vào hệ thống");

		return user;
	}

	@Transactional(readOnly = true)
	public List<User> getAllUsers() {
		return userRepository.findAll();
	}

	@Transactional(readOnly = true)
	public User getUserById(Long id) {
		return userRepository.findById(id)
				.orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng với id: " + id));
	}

	@Transactional
	public User updateUserRoles(Long userId, Set<RoleName> roleNames) {
		User user = getUserById(userId);
		Set<Role> roles = new HashSet<>();
		for (RoleName roleName : roleNames) {
			Role role = roleRepository.findByName(roleName)
					.orElseThrow(() -> new RuntimeException("Không tìm thấy quyền: " + roleName));
			roles.add(role);
		}
		user.setRoles(roles);
		User savedUser = userRepository.save(user);
		logActivity(savedUser, "Cập nhật phân quyền", "Vai trò hiện tại: " + roleNames);
		return savedUser;
	}

	@Transactional(readOnly = true)
	public List<User> searchUsers(String keyword) {
		if (keyword == null || keyword.trim().isEmpty()) {
			return getAllUsers();
		}
		return userRepository.searchByFullName(keyword.trim());
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
			throw new IllegalArgumentException("Email không được để trống");
		}
		if (userRepository.existsByEmailAndIdNot(normalizedEmail, userId)) {
			throw new IllegalArgumentException("Email đã tồn tại");
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
			throw new IllegalArgumentException("Mật khẩu hiện tại không đúng");
		}
		if (newPassword == null || newPassword.length() < 6) {
			throw new IllegalArgumentException("Mật khẩu mới phải có ít nhất 6 ký tự");
		}
		if (!newPassword.equals(confirmPassword)) {
			throw new IllegalArgumentException("Xác nhận mật khẩu không khớp");
		}
		if (newPassword.equals(currentPassword)) {
			throw new IllegalArgumentException("Mật khẩu mới phải khác mật khẩu hiện tại");
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
