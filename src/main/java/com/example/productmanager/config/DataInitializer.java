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

import com.example.productmanager.entity.Product;
import com.example.productmanager.entity.ProductCategory;
import com.example.productmanager.entity.Role;
import com.example.productmanager.entity.RoleName;
import com.example.productmanager.entity.User;
import com.example.productmanager.repository.ProductRepository;
import com.example.productmanager.repository.RoleRepository;
import com.example.productmanager.repository.UserRepository;

@Configuration
public class DataInitializer {

	@Bean
    @SuppressWarnings("unused")
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
							case ADMIN -> "System administrator";
							case MANAGER -> "Store manager";
							case STAFF -> "Sales staff";
							case CUSTOMER -> "Customer";
						})
						.build();
					roleRepository.save(role);
				}
			});

			if (productRepository.count() == 0) {
				List<Product> products = List.of(

    Product.builder().nameVi("Sữa tươi Vinamilk").nameEn("Vinamilk fresh milk").category(ProductCategory.SUA)
        .price(new BigDecimal("42000")).quantity(120).unitVi("Hộp").unitEn("Box").build(),

    Product.builder().nameVi("Gạo ST25").nameEn("ST25 rice").category(ProductCategory.THUC_PHAM)
        .price(new BigDecimal("52000")).quantity(80).unitVi("Kg").unitEn("kg").build(),

    Product.builder().nameVi("Trứng gà ta").nameEn("Free-range chicken eggs").category(ProductCategory.THUC_PHAM)
        .price(new BigDecimal("35000")).quantity(200).unitVi("Hộp").unitEn("Box").build(),

    Product.builder().nameVi("Nước ngọt Coca Cola").nameEn("Coca Cola soft drink").category(ProductCategory.DO_UONG)
        .price(new BigDecimal("18000")).quantity(300).unitVi("Chai").unitEn("Bottle").build(),

    Product.builder().nameVi("Bánh quy Oreo").nameEn("Oreo cookies").category(ProductCategory.BANH_KEO)
        .price(new BigDecimal("25000")).quantity(150).unitVi("Gói").unitEn("Pack").build(),

    Product.builder().nameVi("Rau cải ngọt").nameEn("Sweet mustard greens").category(ProductCategory.RAU_CU)
        .price(new BigDecimal("15000")).quantity(70).unitVi("Kg").unitEn("kg").build(),

    Product.builder().nameVi("Mì Hảo Hảo tôm chua cay").nameEn("Hảo Hảo spicy shrimp noodles").category(ProductCategory.THUC_PHAM)
        .price(new BigDecimal("5000")).quantity(500).unitVi("Gói").unitEn("Pack").build(),

    Product.builder().nameVi("Dầu ăn Neptune").nameEn("Neptune cooking oil").category(ProductCategory.GIA_VI)
        .price(new BigDecimal("48000")).quantity(100).unitVi("Chai").unitEn("Bottle").build(),

    Product.builder().nameVi("Nước mắm Nam Ngư").nameEn("Nam Ngu fish sauce").category(ProductCategory.GIA_VI)
        .price(new BigDecimal("35000")).quantity(120).unitVi("Chai").unitEn("Bottle").build(),

    Product.builder().nameVi("Đường trắng Biên Hòa").nameEn("Bien Hoa white sugar").category(ProductCategory.GIA_VI)
        .price(new BigDecimal("28000")).quantity(90).unitVi("Kg").unitEn("kg").build(),

    Product.builder().nameVi("Muối i-ốt").nameEn("Iodized salt").category(ProductCategory.GIA_VI)
        .price(new BigDecimal("10000")).quantity(150).unitVi("Gói").unitEn("Pack").build(),

    Product.builder().nameVi("Cà phê hòa tan G7").nameEn("G7 instant coffee").category(ProductCategory.DO_UONG)
        .price(new BigDecimal("65000")).quantity(100).unitVi("Hộp").unitEn("Box").build(),

    Product.builder().nameVi("Trà xanh 0 độ").nameEn("0-degree green tea").category(ProductCategory.DO_UONG)
        .price(new BigDecimal("12000")).quantity(250).unitVi("Chai").unitEn("Bottle").build(),

    Product.builder().nameVi("Nước suối Lavie").nameEn("Lavie mineral water").category(ProductCategory.DO_UONG)
        .price(new BigDecimal("7000")).quantity(400).unitVi("Chai").unitEn("Bottle").build(),

    Product.builder().nameVi("Bánh mì sandwich").nameEn("Sandwich bread").category(ProductCategory.BANH)
        .price(new BigDecimal("28000")).quantity(80).unitVi("Ổ").unitEn("Loaf").build(),

    Product.builder().nameVi("Xúc xích tiệt trùng").nameEn("Sterilized sausage").category(ProductCategory.THUC_PHAM)
        .price(new BigDecimal("45000")).quantity(120).unitVi("Gói").unitEn("Pack").build(),

    Product.builder().nameVi("Thịt heo ba chỉ").nameEn("Pork shoulder").category(ProductCategory.THIT)
        .price(new BigDecimal("145000")).quantity(50).unitVi("Kg").unitEn("kg").build(),

    Product.builder().nameVi("Thịt gà").nameEn("Chicken meat").category(ProductCategory.THIT)
        .price(new BigDecimal("85000")).quantity(60).unitVi("Kg").unitEn("kg").build(),

    Product.builder().nameVi("Cá basa phi lê").nameEn("Basa fillet").category(ProductCategory.THUY_SAN)
        .price(new BigDecimal("75000")).quantity(45).unitVi("Kg").unitEn("kg").build(),

    Product.builder().nameVi("Cà chua").nameEn("Tomato").category(ProductCategory.RAU_CU)
        .price(new BigDecimal("25000")).quantity(80).unitVi("Kg").unitEn("kg").build(),

    Product.builder().nameVi("Khoai tây").nameEn("Potato").category(ProductCategory.RAU_CU)
        .price(new BigDecimal("30000")).quantity(70).unitVi("Kg").unitEn("kg").build(),

    Product.builder().nameVi("Cà rốt").nameEn("Carrot").category(ProductCategory.RAU_CU)
        .price(new BigDecimal("22000")).quantity(75).unitVi("Kg").unitEn("kg").build(),

    Product.builder().nameVi("Táo Fuji").nameEn("Fuji apple").category(ProductCategory.TRAI_CAY)
        .price(new BigDecimal("55000")).quantity(60).unitVi("Kg").unitEn("kg").build(),

    Product.builder().nameVi("Chuối").nameEn("Banana").category(ProductCategory.TRAI_CAY)
        .price(new BigDecimal("25000")).quantity(80).unitVi("Kg").unitEn("kg").build(),

    Product.builder().nameVi("Cam sành").nameEn("Vietnamese orange").category(ProductCategory.TRAI_CAY)
        .price(new BigDecimal("35000")).quantity(70).unitVi("Kg").unitEn("kg").build(),

    Product.builder().nameVi("Dầu gội Clear").nameEn("Clear shampoo").category(ProductCategory.CHAM_SOC_CA_NHAN)
        .price(new BigDecimal("85000")).quantity(60).unitVi("Chai").unitEn("Bottle").build(),

    Product.builder().nameVi("Kem đánh răng P/S").nameEn("P/S toothpaste").category(ProductCategory.CHAM_SOC_CA_NHAN)
        .price(new BigDecimal("32000")).quantity(100).unitVi("Tuýp").unitEn("Tube").build(),

    Product.builder().nameVi("Nước rửa chén Sunlight").nameEn("Sunlight dishwashing liquid").category(ProductCategory.DO_GIA_DUNG)
        .price(new BigDecimal("42000")).quantity(90).unitVi("Chai").unitEn("Bottle").build(),

    Product.builder().nameVi("Bột giặt OMO").nameEn("OMO laundry powder").category(ProductCategory.DO_GIA_DUNG)
        .price(new BigDecimal("125000")).quantity(70).unitVi("Túi").unitEn("Bag").build(),

    Product.builder().nameVi("Khăn giấy ăn Pulppy").nameEn("Pulppy paper towels").category(ProductCategory.DO_GIA_DUNG)
        .price(new BigDecimal("30000")).quantity(100).unitVi("Gói").unitEn("Pack").build()

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
        .password(passwordEncoder.encode("customer"))
        .email("customer@demo.com")
        .fullName("Phạm Văn D")
        .enabled(true)
        .roles(new HashSet<>(Set.of(customerRole)))
        .build(),

    User.builder()
        .username("customer2")
        .password(passwordEncoder.encode("customer2"))
        .email("customer2@demo.com")
        .fullName("Vũ Văn E")
        .enabled(true)
        .roles(new HashSet<>(Set.of(customerRole)))
        .build(),

    User.builder()
        .username("customer3")
        .password(passwordEncoder.encode("customer3"))
        .email("customer3@demo.com")
        .fullName("Bùi Văn F")
        .enabled(true)
        .roles(new HashSet<>(Set.of(customerRole)))
        .build(),

    User.builder()
        .username("customer4")
        .password(passwordEncoder.encode("customer4"))
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
