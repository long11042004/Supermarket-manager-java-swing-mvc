package com.example.productmanager.config;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.example.productmanager.model.Product;
import com.example.productmanager.model.Role;
import com.example.productmanager.model.RoleName;
import com.example.productmanager.model.User;
import com.example.productmanager.repository.ProductRepository;
import com.example.productmanager.repository.RoleRepository;
import com.example.productmanager.repository.UserRepository;

@Configuration
public class DataInitializer {

	@Bean
	CommandLineRunner initData(RoleRepository roleRepository, ProductRepository productRepository, UserRepository userRepository) {
		return args -> {
			Arrays.stream(RoleName.values()).forEach(roleName -> {
				boolean exists = roleRepository.findByName(roleName).isPresent();
				if (!exists) {
					Role role = Role.builder()
						.name(roleName)
						.description(switch (roleName) {
							case ADMIN -> "Quản trị hệ thống";
							case MANAGER -> "Quản lý cửa hàng";
							case STAFF -> "Nhân viên bán hàng";
							case CUSTOMER -> "Khách hàng";
						})
						.build();
					roleRepository.save(role);
				}
			});

			if (productRepository.count() == 0) {
				List<Product> products = List.of(
						Product.builder().name("Sữa tươi Vinamilk").category("Sữa").price(new BigDecimal("42000")).quantity(120).unit("Hộp").build(),
						Product.builder().name("Gạo ST25").category("Thực phẩm").price(new BigDecimal("52000")).quantity(80).unit("Kg").build(),
						Product.builder().name("Trứng gà ta").category("Thực phẩm").price(new BigDecimal("35000")).quantity(200).unit("Hộp").build(),
						Product.builder().name("Nước ngọt Coca Cola").category("Đồ uống").price(new BigDecimal("18000")).quantity(300).unit("Chai").build(),
						Product.builder().name("Bánh quy Oreo").category("Bánh kẹo").price(new BigDecimal("25000")).quantity(150).unit("Gói").build(),
						Product.builder().name("Rau cải ngọt").category("Rau củ").price(new BigDecimal("15000")).quantity(70).unit("Kg").build()
				);
				productRepository.saveAll(products);
			}

			if (userRepository.count() == 0) {
				Role adminRole = roleRepository.findByName(RoleName.ADMIN)
						.orElseThrow(() -> new IllegalStateException("Role ADMIN not found"));
				Role managerRole = roleRepository.findByName(RoleName.MANAGER)
						.orElseThrow(() -> new IllegalStateException("Role MANAGER not found"));
				Role staffRole = roleRepository.findByName(RoleName.STAFF)
						.orElseThrow(() -> new IllegalStateException("Role STAFF not found"));
				Role customerRole = roleRepository.findByName(RoleName.CUSTOMER)
						.orElseThrow(() -> new IllegalStateException("Role CUSTOMER not found"));

				List<User> users = List.of(
						User.builder()
							.username("admin")
							.password("admin123")
							.email("admin@demo.com")
							.fullName("Quản trị viên")
							.enabled(true)
							.roles(new HashSet<>(Set.of(adminRole, managerRole)))
							.build(),
						User.builder()
							.username("manager")
							.password("manager")
							.email("manager@demo.com")
							.fullName("Nguyễn Văn Quản lý")
							.enabled(true)
							.roles(new HashSet<>(Set.of(managerRole, staffRole)))
							.build(),
						User.builder()
							.username("staff")
							.password("staff123")
							.email("staff@demo.com")
							.fullName("Trần Thị Nhân viên")
							.enabled(true)
							.roles(new HashSet<>(Set.of(staffRole)))
							.build(),
						User.builder()
							.username("customer")
							.password("customer123")
							.email("customer@demo.com")
							.fullName("Lê Văn Khách")
							.enabled(true)
							.roles(new HashSet<>(Set.of(customerRole)))
							.build()
				);
				userRepository.saveAll(users);
			}
		};
	}
}
