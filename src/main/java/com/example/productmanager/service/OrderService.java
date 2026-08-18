package com.example.productmanager.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.productmanager.model.CartItem;
import com.example.productmanager.model.CustomerOrder;
import com.example.productmanager.model.CustomerOrderItem;
import com.example.productmanager.model.OrderStatus;
import com.example.productmanager.model.Product;
import com.example.productmanager.model.User;
import com.example.productmanager.repository.CustomerOrderRepository;
import com.example.productmanager.repository.ProductRepository;
import com.example.productmanager.repository.UserRepository;
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
					.orElseThrow(() -> new IllegalArgumentException("Không tìm thấy người dùng đặt hàng."));
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
			if (cartItem.getProductId() == null) {
				throw new IllegalArgumentException("Giỏ hàng chứa sản phẩm không hợp lệ. Vui lòng tải lại trang.");
			}
			if (cartItem.getQuantity() <= 0) {
				throw new IllegalArgumentException("Số lượng sản phẩm trong giỏ không hợp lệ.");
			}
			Product product = productRepository.findById(cartItem.getProductId())
					.orElseThrow(() -> new IllegalArgumentException("Sản phẩm trong giỏ không còn tồn tại. Vui lòng tải lại danh sách."));
			if (product.getPrice() == null) {
				throw new IllegalArgumentException("Sản phẩm " + product.getName() + " chưa có giá bán hợp lệ.");
			}
			if (product.getQuantity() == null) {
				throw new IllegalArgumentException("Sản phẩm " + product.getName() + " có dữ liệu tồn kho không hợp lệ.");
			}
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

	@Transactional
	public void cancelOrderForUser(Long userId, Long orderId) {
		if (userId == null) {
			throw new IllegalArgumentException("Không tìm thấy thông tin người dùng.");
		}
		CustomerOrder order = customerOrderRepository.findDetailByIdAndUserId(orderId, userId)
				.orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn hàng cần hủy."));

		if (order.getStatus() == OrderStatus.CANCELLED) {
			throw new IllegalArgumentException("Đơn hàng này đã được hủy trước đó.");
		}
		if (order.getStatus() == OrderStatus.COMPLETED || order.getStatus() == OrderStatus.DELIVERING) {
			throw new IllegalArgumentException("Đơn hàng đang giao hoặc đã hoàn tất nên không thể hủy.");
		}

		for (CustomerOrderItem item : order.getItems()) {
			Product product = item.getProduct();
			if (product == null || product.getId() == null) {
				continue;
			}
			Product managedProduct = productRepository.findById(product.getId()).orElse(null);
			if (managedProduct == null) {
				continue;
			}
			Integer currentQtyValue = managedProduct.getQuantity();
			Integer rollbackQtyValue = item.getQuantity();
			int currentQty = currentQtyValue == null ? 0 : currentQtyValue;
			int rollbackQty = rollbackQtyValue == null ? 0 : rollbackQtyValue;
			managedProduct.setQuantity(currentQty + rollbackQty);
			productRepository.save(managedProduct);
		}

		order.setStatus(OrderStatus.CANCELLED);
		customerOrderRepository.save(order);
		userService.recordActivity(userId, "Hủy đơn", "Đơn hàng #" + order.getId() + " đã được hủy");
	}
}