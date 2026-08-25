package com.example.productmanager.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.context.support.ResourceBundleMessageSource;

import com.example.productmanager.email.event.OrderCancelledEmailEvent;
import com.example.productmanager.entity.CustomerOrder;
import com.example.productmanager.entity.CustomerOrderItem;
import com.example.productmanager.entity.OrderStatus;
import com.example.productmanager.entity.Product;
import com.example.productmanager.entity.User;
import com.example.productmanager.exception.ConflictException;
import com.example.productmanager.exception.NotFoundException;
import com.example.productmanager.multilanguage.MessageResolver;
import com.example.productmanager.repository.CustomerOrderRepository;
import com.example.productmanager.repository.ProductRepository;
import com.example.productmanager.repository.UserRepository;
import com.example.productmanager.service.CartService.CartView;

class OrderServiceTests {

	private final CustomerOrderRepository customerOrderRepository = org.mockito.Mockito.mock(CustomerOrderRepository.class);
	private final ProductRepository productRepository = org.mockito.Mockito.mock(ProductRepository.class);
	private final UserRepository userRepository = org.mockito.Mockito.mock(UserRepository.class);
	private final UserService userService = org.mockito.Mockito.mock(UserService.class);
	private final ApplicationEventPublisher applicationEventPublisher = org.mockito.Mockito.mock(ApplicationEventPublisher.class);
	private final MessageResolver messageResolver = new MessageResolver(createMessageSource());
	private final OrderService orderService = new OrderService(
			customerOrderRepository,
			productRepository,
			userRepository,
			userService,
			messageResolver,
			applicationEventPublisher);
	private final CartService cartService = new CartService(messageResolver);

	private ResourceBundleMessageSource createMessageSource() {
		LocaleContextHolder.setLocale(Locale.forLanguageTag("vi-VN"));
		ResourceBundleMessageSource source = new ResourceBundleMessageSource();
		source.setBasename("messages");
		source.setDefaultEncoding("UTF-8");
		return source;
	}

	@Test
	void checkoutAsGuestShouldCreateOrderWithoutUser() {
		CartView cart = new CartView();
		Product product = Product.builder()
				.id(21L)
				.nameVi("Cam vàng")
				.nameEn("Yellow orange")
				.price(new BigDecimal("50000"))
				.quantity(10)
				.unitVi("kg")
				.unitEn("kg")
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
				.nameVi("Nho Mỹ")
				.nameEn("American grapes")
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

	@Test
	void checkoutAsGuestShouldFailWhenProductNoLongerExists() {
		CartView cart = new CartView();
		Product product = Product.builder()
				.id(23L)
				.nameVi("Bánh gạo")
				.nameEn("Rice cake")
				.price(new BigDecimal("35000"))
				.quantity(5)
				.build();
		cartService.addItem(cart, product, 1);
		when(productRepository.findById(23L)).thenReturn(Optional.empty());

		NotFoundException ex = assertThrows(NotFoundException.class,
				() -> orderService.checkoutAsGuest("Khách", "guest@demo.com", cart, "789 Đường DEF", "0909000000", null));

		assertEquals("Sản phẩm trong giỏ không còn tồn tại. Vui lòng tải lại danh sách.", ex.getMessage());
		verify(customerOrderRepository, never()).save(any(CustomerOrder.class));
	}

	@Test
	void checkoutAsGuestShouldFailWhenProductPriceIsMissing() {
		CartView cart = new CartView();
		Product product = Product.builder()
				.id(24L)
				.nameVi("Sua hop")
				.nameEn("Milk box")
				.quantity(5)
				.build();
		cartService.addItem(cart, product, 1);
		when(productRepository.findById(24L)).thenReturn(Optional.of(product));

		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
				() -> orderService.checkoutAsGuest("Khach", "guest@demo.com", cart, "12 Duong Moi", "0909000000", null));

		assertEquals("Sản phẩm Sua hop chưa có giá bán hợp lệ.", ex.getMessage());
		verify(customerOrderRepository, never()).save(any(CustomerOrder.class));
	}

	@Test
	void cancelOrderForUserShouldSetCancelledAndRollbackStock() {
		Product product = Product.builder()
				.id(50L)
				.nameVi("Nuoc suoi")
				.nameEn("Mineral water")
				.quantity(7)
				.build();

		CustomerOrder order = CustomerOrder.builder()
				.id(101L)
				.user(User.builder().id(9L).email("customer@demo.com").build())
				.status(OrderStatus.PENDING)
				.items(List.of(CustomerOrderItem.builder().product(product).quantity(3).build()))
				.build();

		when(customerOrderRepository.findDetailByIdAndUserId(101L, 9L)).thenReturn(Optional.of(order));
		when(productRepository.findById(50L)).thenReturn(Optional.of(product));

		orderService.cancelOrderForUser(9L, 101L);

		assertEquals(OrderStatus.CANCELLED, order.getStatus());
		assertEquals(10, product.getQuantity());
		verify(customerOrderRepository).save(order);
		verify(userService).recordActivity(9L, "Hủy đơn", "Đơn hàng #101 đã được hủy");
		verify(applicationEventPublisher).publishEvent(any(OrderCancelledEmailEvent.class));
	}

	@Test
	void cancelOrderForUserShouldRejectCompletedOrder() {
		CustomerOrder order = CustomerOrder.builder()
				.id(102L)
				.user(User.builder().id(9L).build())
				.status(OrderStatus.COMPLETED)
				.items(List.of())
				.build();

		when(customerOrderRepository.findDetailByIdAndUserId(102L, 9L)).thenReturn(Optional.of(order));

		ConflictException ex = assertThrows(ConflictException.class,
				() -> orderService.cancelOrderForUser(9L, 102L));

		assertEquals("Đơn hàng đang giao hoặc đã hoàn tất nên không thể hủy.", ex.getMessage());
		verify(customerOrderRepository, never()).save(any(CustomerOrder.class));
	}
}