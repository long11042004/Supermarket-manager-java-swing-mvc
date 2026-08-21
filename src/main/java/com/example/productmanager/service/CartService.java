package com.example.productmanager.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.example.productmanager.i18n.MessageResolver;
import com.example.productmanager.model.CartItem;
import com.example.productmanager.model.Product;

import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class CartService {
	private final MessageResolver messageResolver;

	public CartView addItem(CartView cart, Product product, int quantity) {
		if (quantity <= 0) {
			throw new IllegalArgumentException(messageResolver.msg("err.cart.quantityPositive"));
		}
		CartView workingCart = cart == null ? new CartView() : cart;
		Map<Long, CartItem> indexedItems = new LinkedHashMap<>();
		for (CartItem item : workingCart.getItems()) {
			indexedItems.put(item.getProductId(), item);
		}

		CartItem existingItem = indexedItems.get(product.getId());
		int newQuantity = quantity;
		if (existingItem != null) {
			newQuantity += existingItem.getQuantity();
		}
		if (newQuantity > product.getQuantity()) {
			throw new IllegalArgumentException(messageResolver.msg("err.cart.quantityExceedsStock"));
		}

		indexedItems.put(product.getId(), new CartItem(
				product.getId(),
				product.getName(),
				product.getPrice(),
				quantityLabel(product),
				newQuantity));

		workingCart.setItems(toSortedItems(indexedItems));
		return workingCart;
	}

	public CartView updateItemQuantity(CartView cart, Long productId, int quantity, int availableQuantity) {
		CartView workingCart = cart == null ? new CartView() : cart;
		if (quantity <= 0) {
			return removeItem(workingCart, productId);
		}
		if (quantity > availableQuantity) {
			throw new IllegalArgumentException(messageResolver.msg("err.cart.updateExceedsStock"));
		}

		for (CartItem item : workingCart.getItems()) {
			if (item.getProductId().equals(productId)) {
				item.setQuantity(quantity);
				break;
			}
		}
		return workingCart;
	}

	public CartView removeItem(CartView cart, Long productId) {
		CartView workingCart = cart == null ? new CartView() : cart;
		workingCart.setItems(workingCart.getItems().stream()
				.filter(item -> !item.getProductId().equals(productId))
				.toList());
		return workingCart;
	}

	public CartView clear(CartView cart) {
		CartView workingCart = cart == null ? new CartView() : cart;
		workingCart.setItems(new ArrayList<>());
		return workingCart;
	}

	private List<CartItem> toSortedItems(Map<Long, CartItem> items) {
		return items.values().stream()
				.sorted(Comparator.comparing(item -> item.getProductName(), String.CASE_INSENSITIVE_ORDER))
				.toList();
	}

	private String quantityLabel(Product product) {
		return product.getUnit() == null || product.getUnit().isBlank() ? "sp" : product.getUnit();
	}

	public static class CartView {

		private List<CartItem> items = new ArrayList<>();

		public List<CartItem> getItems() {
			return items;
		}

		public void setItems(List<CartItem> items) {
			this.items = new ArrayList<>(items);
		}

		public int getItemCount() {
			return items.stream().mapToInt(item -> item.getQuantity()).sum();
		}

		public BigDecimal getGrandTotal() {
			return items.stream()
					.map(item -> item.getLineTotal())
					.reduce(BigDecimal.ZERO, (left, right) -> left.add(right));
		}
	}
}