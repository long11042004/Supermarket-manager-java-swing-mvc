package com.example.productmanager.config;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

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
    CommandLineRunner initData(RoleRepository roleRepository,
            ProductRepository productRepository,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder) {
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

    Product.builder().name("Sữa tươi Vinamilk").category("Sữa")
        .price(new BigDecimal("42000")).quantity(120).unit("Hộp").build(),

    Product.builder().name("Gạo ST25").category("Thực phẩm")
        .price(new BigDecimal("52000")).quantity(80).unit("Kg").build(),

    Product.builder().name("Trứng gà ta").category("Thực phẩm")
        .price(new BigDecimal("35000")).quantity(200).unit("Hộp").build(),

    Product.builder().name("Nước ngọt Coca Cola").category("Đồ uống")
        .price(new BigDecimal("18000")).quantity(300).unit("Chai").build(),

    Product.builder().name("Bánh quy Oreo").category("Bánh kẹo")
        .price(new BigDecimal("25000")).quantity(150).unit("Gói").build(),

    Product.builder().name("Rau cải ngọt").category("Rau củ")
        .price(new BigDecimal("15000")).quantity(70).unit("Kg").build(),

    Product.builder().name("Mì Hảo Hảo tôm chua cay").category("Thực phẩm")
        .price(new BigDecimal("5000")).quantity(500).unit("Gói").build(),

    Product.builder().name("Dầu ăn Neptune").category("Gia vị")
        .price(new BigDecimal("48000")).quantity(100).unit("Chai").build(),

    Product.builder().name("Nước mắm Nam Ngư").category("Gia vị")
        .price(new BigDecimal("35000")).quantity(120).unit("Chai").build(),

    Product.builder().name("Đường trắng Biên Hòa").category("Gia vị")
        .price(new BigDecimal("28000")).quantity(90).unit("Kg").build(),

    Product.builder().name("Muối i-ốt").category("Gia vị")
        .price(new BigDecimal("10000")).quantity(150).unit("Gói").build(),

    Product.builder().name("Cà phê hòa tan G7").category("Đồ uống")
        .price(new BigDecimal("65000")).quantity(100).unit("Hộp").build(),

    Product.builder().name("Trà xanh 0 độ").category("Đồ uống")
        .price(new BigDecimal("12000")).quantity(250).unit("Chai").build(),

    Product.builder().name("Nước suối Lavie").category("Đồ uống")
        .price(new BigDecimal("7000")).quantity(400).unit("Chai").build(),

    Product.builder().name("Bánh mì sandwich").category("Bánh")
        .price(new BigDecimal("28000")).quantity(80).unit("Ổ").build(),

    Product.builder().name("Xúc xích tiệt trùng").category("Thực phẩm")
        .price(new BigDecimal("45000")).quantity(120).unit("Gói").build(),

    Product.builder().name("Thịt heo ba chỉ").category("Thịt")
        .price(new BigDecimal("145000")).quantity(50).unit("Kg").build(),

    Product.builder().name("Thịt gà").category("Thịt")
        .price(new BigDecimal("85000")).quantity(60).unit("Kg").build(),

    Product.builder().name("Cá basa phi lê").category("Thủy sản")
        .price(new BigDecimal("75000")).quantity(45).unit("Kg").build(),

    Product.builder().name("Cà chua").category("Rau củ")
        .price(new BigDecimal("25000")).quantity(80).unit("Kg").build(),

    Product.builder().name("Khoai tây").category("Rau củ")
        .price(new BigDecimal("30000")).quantity(70).unit("Kg").build(),

    Product.builder().name("Cà rốt").category("Rau củ")
        .price(new BigDecimal("22000")).quantity(75).unit("Kg").build(),

    Product.builder().name("Táo Fuji").category("Trái cây")
        .price(new BigDecimal("55000")).quantity(60).unit("Kg").build(),

    Product.builder().name("Chuối").category("Trái cây")
        .price(new BigDecimal("25000")).quantity(80).unit("Kg").build(),

    Product.builder().name("Cam sành").category("Trái cây")
        .price(new BigDecimal("35000")).quantity(70).unit("Kg").build(),

    Product.builder().name("Dầu gội Clear").category("Chăm sóc cá nhân")
        .price(new BigDecimal("85000")).quantity(60).unit("Chai").build(),

    Product.builder().name("Kem đánh răng P/S").category("Chăm sóc cá nhân")
        .price(new BigDecimal("32000")).quantity(100).unit("Tuýp").build(),

    Product.builder().name("Nước rửa chén Sunlight").category("Đồ gia dụng")
        .price(new BigDecimal("42000")).quantity(90).unit("Chai").build(),

    Product.builder().name("Bột giặt OMO").category("Đồ gia dụng")
        .price(new BigDecimal("125000")).quantity(70).unit("Túi").build(),

    Product.builder().name("Khăn giấy ăn Pulppy").category("Đồ gia dụng")
        .price(new BigDecimal("30000")).quantity(100).unit("Gói").build()

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
        .password(passwordEncoder.encode("admin123"))
        .email("admin@demo.com")
        .fullName("Nguyễn Văn A")
        .enabled(true)
        .roles(new HashSet<>(Set.of(adminRole, managerRole)))
        .build(),

    User.builder()
        .username("manager")
        .password(passwordEncoder.encode("manager"))
        .email("manager@demo.com")
        .fullName("Trần Văn B")
        .enabled(true)
        .roles(new HashSet<>(Set.of(managerRole)))
        .build(),

    User.builder()
        .username("staff")
        .password(passwordEncoder.encode("staff123"))
        .email("staff@demo.com")
        .fullName("Lê Văn C")
        .enabled(true)
        .roles(new HashSet<>(Set.of(staffRole)))
        .build(),

    User.builder()
        .username("customer")
        .password(passwordEncoder.encode("customer123"))
        .email("customer@demo.com")
        .fullName("Phạm Văn D")
        .enabled(true)
        .roles(new HashSet<>(Set.of(customerRole)))
        .build(),

    User.builder()
        .username("customer2")
        .password(passwordEncoder.encode("customer2123"))
        .email("customer2@demo.com")
        .fullName("Vũ Văn E")
        .enabled(true)
        .roles(new HashSet<>(Set.of(customerRole)))
        .build(),

    User.builder()
        .username("customer3")
        .password(passwordEncoder.encode("customer3123"))
        .email("customer3@demo.com")
        .fullName("Bùi Văn F")
        .enabled(true)
        .roles(new HashSet<>(Set.of(customerRole)))
        .build(),

    User.builder()
        .username("customer4")
        .password(passwordEncoder.encode("customer4123"))
        .email("customer4@demo.com")
        .fullName("Đỗ Văn G")
        .enabled(true)
        .roles(new HashSet<>(Set.of(customerRole)))
        .build()
);
				userRepository.saveAll(users);
			}

            List<User> usersNeedingPasswordMigration = userRepository.findAll().stream()
                    .filter(user -> !isEncodedPassword(user.getPassword()))
                    .toList();
            if (!usersNeedingPasswordMigration.isEmpty()) {
                for (User user : usersNeedingPasswordMigration) {
                    user.setPassword(passwordEncoder.encode(user.getPassword()));
                }
                userRepository.saveAll(usersNeedingPasswordMigration);
            }
		};
	}

    private boolean isEncodedPassword(String password) {
        if (password == null || password.isBlank()) {
            return false;
        }
        return password.startsWith("{") || password.startsWith("$2a$") || password.startsWith("$2b$") || password.startsWith("$2y$");
    }
}
