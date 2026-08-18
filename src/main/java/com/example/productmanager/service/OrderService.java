package com.example.productmanager.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.productmanager.model.CustomerOrder;
import com.example.productmanager.model.CustomerOrderItem;
import com.example.productmanager.model.OrderStatus;
import com.example.productmanager.model.Product;
import com.example.productmanager.model.User;
import com.example.productmanager.repository.CustomerOrderRepository;
import com.example.productmanager.repository.ProductRepository;
import com.example.productmanager.repository.UserRepository;
import com.example.productmanager.service.CartService.CartItem;
import com.example.productmanager.service.CartService.CartView;

@Service
public class OrderService {

	private final CustomerOrderRepository customerOrderRepository;
	private final ProductRepository productRepository;
	private final UserRepository userRepository;
	private final UserService userService;

	public OrderService(CustomerOrderRepository customerOrderRepository,
			ProductRepository productRepository,
			UserRepository userRepository,
			UserService userService) {
		this.customerOrderRepository = customerOrderRepository;
		this.productRepository = productRepository;
		this.userRepository = userRepository;
		this.userService = userService;
	}

	@Transactional
	public CustomerOrder checkout(Long userId, CartView cart, String deliveryAddress, String contactPhone, String note) {
		return checkout(userId, null, null, cart, deliveryAddress, contactPhone, note);
	}

	@Transactional
	public CustomerOrder checkoutAsGuest(String guestName,
			String guestEmail,
			CartView cart,
			String deliveryAddress,
			String contactPhone,
			String note) {
		return checkout(null, guestName, guestEmail, cart, deliveryAddress, contactPhone, note);
	}

	private CustomerOrder checkout(Long userId,
			String guestName,
			String guestEmail,
			CartView cart,
			String deliveryAddress,
			String contactPhone,
			String note) {
		if (cart == null || cart.getItems().isEmpty()) {
			throw new IllegalArgumentException("Giỏ hàng đang trống");
		}
		if (deliveryAddress == null || deliveryAddress.trim().isEmpty()) {
			throw new IllegalArgumentException("Địa chỉ giao hàng không được để trống");
		}
		if (userId == null && (guestName == null || guestName.trim().isEmpty())) {
			throw new IllegalArgumentException("Vui lòng nhập tên người nhận");
		}
		if (userId == null && (guestEmail == null || guestEmail.trim().isEmpty())) {
			throw new IllegalArgumentException("Vui lòng nhập email để xác nhận đơn hàng");
		}

		User user = null;
		if (userId != null) {
			user = userRepository.findById(userId)
					.orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng với id: " + userId));
		}

		List<CustomerOrderItem> orderItems = new ArrayList<>();
		BigDecimal totalAmount = BigDecimal.ZERO;

		CustomerOrder order = CustomerOrder.builder()
				.user(user)
				.guestName(user == null ? guestName.trim() : null)
				.guestEmail(user == null ? guestEmail.trim() : null)
				.deliveryAddress(deliveryAddress.trim())
				.contactPhone(contactPhone == null ? null : contactPhone.trim())
				.note(note == null ? null : note.trim())
				.status(OrderStatus.PENDING)
				.totalAmount(BigDecimal.ZERO)
				.build();

		for (CartItem cartItem : cart.getItems()) {
			Product product = productRepository.findById(cartItem.getProductId())
					.orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm với id: " + cartItem.getProductId()));
			if (product.getQuantity() < cartItem.getQuantity()) {
				throw new IllegalArgumentException("Sản phẩm " + product.getName() + " không đủ số lượng trong kho");
			}

			BigDecimal lineTotal = product.getPrice().multiply(BigDecimal.valueOf(cartItem.getQuantity()));
			CustomerOrderItem item = CustomerOrderItem.builder()
					.order(order)
					.product(product)
					.quantity(cartItem.getQuantity())
					.unitPrice(product.getPrice())
					.lineTotal(lineTotal)
					.build();
			orderItems.add(item);
			totalAmount = totalAmount.add(lineTotal);

			product.setQuantity(product.getQuantity() - cartItem.getQuantity());
			productRepository.save(product);
		}

		order.setItems(orderItems);
		order.setTotalAmount(totalAmount);
		CustomerOrder savedOrder = customerOrderRepository.save(order);
		if (userId != null) {
			userService.recordActivity(userId, "Đặt hàng", "Đơn hàng #" + savedOrder.getId() + " đã được tạo");
		}
		return savedOrder;
	}

	@Transactional(readOnly = true)
	public List<CustomerOrder> getOrdersForUser(Long userId) {
		return customerOrderRepository.findByUserIdOrderByCreatedAtDesc(userId);
	}
}