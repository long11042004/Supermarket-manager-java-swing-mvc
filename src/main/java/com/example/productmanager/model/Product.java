package com.example.productmanager.model;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "products")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Version
	private Long version;

	@NotBlank(message = "Ten san pham khong duoc de trong")
	@Size(max = 120, message = "Ten san pham toi da 120 ky tu")
	@Column(nullable = false, length = 120)
	private String name;

	@NotNull(message = "Danh muc khong duoc de trong")
	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 40)
	private ProductCategory category;

	@NotNull(message = "Gia khong duoc de trong")
	@DecimalMin(value = "0.0", inclusive = false, message = "Gia phai lon hon 0")
	@Column(nullable = false, precision = 12, scale = 2)
	private BigDecimal price;

	@NotNull(message = "So luong khong duoc de trong")
	@Min(value = 0, message = "So luong khong duoc am")
	@Column(nullable = false)
	private Integer quantity;

	@Size(max = 30, message = "Don vi toi da 30 ky tu")
	@Column(length = 30)
	private String unit;

	private LocalDate expiryDate;
}