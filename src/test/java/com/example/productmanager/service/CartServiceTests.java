package com.example.productmanager.service;

import java.math.BigDecimal;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.context.support.ResourceBundleMessageSource;

import com.example.productmanager.entity.Product;
import com.example.productmanager.i18n.MessageResolver;
import com.example.productmanager.service.CartService.CartView;

class CartServiceTests {

	private final MessageResolver messageResolver = new MessageResolver(createMessageSource());
	private final CartService cartService = new CartService(messageResolver);

	private ResourceBundleMessageSource createMessageSource() {
		LocaleContextHolder.setLocale(Locale.forLanguageTag("vi-VN"));
		ResourceBundleMessageSource source = new ResourceBundleMessageSource();
		source.setBasename("messages");
		source.setDefaultEncoding("UTF-8");
		return source;
	}

	@Test
	void addItemShouldAccumulateQuantityAndTotal() {
		CartView cart = new CartView();
		Product product = Product.builder()
				.id(10L)
				.name("Táo Gala")
				.price(new BigDecimal("25000"))
				.quantity(20)
				.unit("kg")
				.build();

		cartService.addItem(cart, product, 2);
		cartService.addItem(cart, product, 3);

		assertEquals(5, cart.getItemCount());
		assertEquals(new BigDecimal("125000"), cart.getGrandTotal());
	}

	@Test
	void addItemShouldRejectQuantityAboveStock() {
		CartView cart = new CartView();
		Product product = Product.builder()
				.id(11L)
				.name("Sữa tươi")
				.price(new BigDecimal("42000"))
				.quantity(4)
				.unit("hộp")
				.build();

		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
				() -> cartService.addItem(cart, product, 5));

		assertEquals("Số lượng trong giỏ vượt quá tồn kho hiện có", ex.getMessage());
	}

	@Test
	void updateItemQuantityShouldRemoveItemWhenQuantityIsZero() {
		CartView cart = new CartView();
		Product product = Product.builder()
				.id(12L)
				.name("Bánh quy")
				.price(new BigDecimal("30000"))
				.quantity(8)
				.build();

		cartService.addItem(cart, product, 2);
		cartService.updateItemQuantity(cart, 12L, 0, 8);

		assertEquals(0, cart.getItemCount());
		assertEquals(BigDecimal.ZERO, cart.getGrandTotal());
	}
}