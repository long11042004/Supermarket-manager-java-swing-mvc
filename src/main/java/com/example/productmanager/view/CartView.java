package com.example.productmanager.view;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import com.example.productmanager.entity.CartItem;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
public class CartView {

	@Getter
    @Setter
    private List<CartItem> items = new ArrayList<>();

    public int getItemCount() {
		return items.stream().mapToInt(item -> item.getQuantity()).sum();
	}

	public BigDecimal getGrandTotal() {
		return items.stream()
				.map(item -> item.getLineTotal())
				.reduce(BigDecimal.ZERO, (left, right) -> left.add(right));
	}
}
