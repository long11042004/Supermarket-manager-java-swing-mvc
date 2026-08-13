package com.example.productmanager.service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.productmanager.model.Role;
import com.example.productmanager.model.RoleName;
import com.example.productmanager.model.User;
import com.example.productmanager.repository.RoleRepository;
import com.example.productmanager.repository.UserRepository;

@Service
public class UserService {

	private final UserRepository userRepository;
	private final RoleRepository roleRepository;

	public UserService(UserRepository userRepository, RoleRepository roleRepository) {
		this.userRepository = userRepository;
		this.roleRepository = roleRepository;
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

		return userRepository.save(user);
	}

	@Transactional(readOnly = true)
	public User login(String username, String password) {
		User user = userRepository.findByUsername(username)
				.orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng: " + username));

		if (!user.getPassword().equals(password)) {
			throw new IllegalArgumentException("Mật khẩu không đúng");
		}

		if (!user.isEnabled()) {
			throw new IllegalStateException("Tài khoản đang bị khóa");
		}

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
		return userRepository.save(user);
	}

	@Transactional(readOnly = true)
	public List<User> searchUsers(String keyword) {
		if (keyword == null || keyword.trim().isEmpty()) {
			return getAllUsers();
		}
		return userRepository.searchByFullName(keyword.trim());
	}
}
