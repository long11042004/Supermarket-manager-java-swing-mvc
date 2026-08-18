package com.example.productmanager.service;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.productmanager.model.CustomerOrder;
import com.example.productmanager.model.Product;
import com.example.productmanager.repository.CustomerOrderRepository;
import com.example.productmanager.repository.ProductRepository;
import com.example.productmanager.repository.UserRepository;
import com.example.productmanager.service.CartService.CartView;

class OrderServiceTests {

	private final CustomerOrderRepository customerOrderRepository = org.mockito.Mockito.mock(CustomerOrderRepository.class);
	private final ProductRepository productRepository = org.mockito.Mockito.mock(ProductRepository.class);
	private final UserRepository userRepository = org.mockito.Mockito.mock(UserRepository.class);
	private final UserService userService = org.mockito.Mockito.mock(UserService.class);
	private final OrderService orderService = new OrderService(customerOrderRepository, productRepository, userRepository, userService);
	private final CartService cartService = new CartService();

	@Test
	void checkoutAsGuestShouldCreateOrderWithoutUser() {
		CartView cart = new CartView();
		Product product = Product.builder()
				.id(21L)
				.name("Cam vàng")
				.price(new BigDecimal("50000"))
				.quantity(10)
				.unit("kg")
				.build();
		cartService.addItem(cart, product, 2);

		when(productRepository.findById(21L)).thenReturn(Optional.of(product));
		when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));
		when(customerOrderRepository.save(any(CustomerOrder.class))).thenAnswer(invocation -> {
			CustomerOrder order = invocation.getArgument(0);
			order.setId(99L);
			return order;
		});

		CustomerOrder order = orderService.checkoutAsGuest(
				"Khách lẻ",
				"guest@example.com",
				cart,
				"123 Đường ABC",
				"0909000000",
				"Giao giờ chiều");

		assertNull(order.getUser());
		assertEquals("Khách lẻ", order.getGuestName());
		assertEquals("guest@example.com", order.getGuestEmail());
		assertEquals(new BigDecimal("100000"), order.getTotalAmount());
		assertEquals(8, product.getQuantity());
		assertEquals(1, order.getItems().size());
		verify(userService, never()).recordActivity(any(), any(), any());
	}

	@Test
	void checkoutAsGuestShouldRequireGuestEmail() {
		CartView cart = new CartView();
		Product product = Product.builder()
				.id(22L)
				.name("Nho Mỹ")
				.price(new BigDecimal("80000"))
				.quantity(6)
				.build();
		cartService.addItem(cart, product, 1);
		when(productRepository.findById(22L)).thenReturn(Optional.of(product));

		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
				() -> orderService.checkoutAsGuest("Khách", "", cart, "456 Đường XYZ", "0988000000", null));

		assertEquals("Vui lòng nhập email để xác nhận đơn hàng", ex.getMessage());
		verify(customerOrderRepository, never()).save(any(CustomerOrder.class));
	}
}