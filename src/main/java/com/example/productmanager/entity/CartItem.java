package com.example.productmanager.entity;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Getter
@AllArgsConstructor
@EqualsAndHashCode(of = "productId")
public class CartItem {

	private final Long productId;
	private final String productName;
	private final BigDecimal unitPrice;
	private final String unitLabel;

	@Setter
	private int quantity;

	public BigDecimal getLineTotal() {
		return unitPrice.multiply(BigDecimal.valueOf(quantity));
	}
}