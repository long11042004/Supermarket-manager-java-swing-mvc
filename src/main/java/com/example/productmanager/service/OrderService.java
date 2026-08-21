package com.example.productmanager.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import com.example.productmanager.i18n.MessageResolver;
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
import com.example.productmanager.service.emailservice.EmailService;

import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class OrderService {

	private final CustomerOrderRepository customerOrderRepository;
	private final ProductRepository productRepository;
	private final UserRepository userRepository;
	private final UserService userService;
	private final MessageResolver messageResolver;
	private final EmailService emailService;
	private final SpringTemplateEngine templateEngine;

	@Transactional
	public CustomerOrder checkout(Long userId, 
		CartView cart, 
		String deliveryAddress, 
		String contactPhone, 
		String note) {
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
			throw new IllegalArgumentException(messageResolver.msg("err.order.cartEmpty"));
		}
		if (deliveryAddress == null || deliveryAddress.trim().isEmpty()) {
			throw new IllegalArgumentException(messageResolver.msg("err.order.deliveryAddressRequired"));
		}
		if (userId == null && (guestName == null || guestName.trim().isEmpty())) {
			throw new IllegalArgumentException(messageResolver.msg("err.order.guestNameRequired"));
		}
		if (userId == null && (guestEmail == null || guestEmail.trim().isEmpty())) {
			throw new IllegalArgumentException(messageResolver.msg("err.order.guestEmailRequired"));
		}

		User user = null;
		if (userId != null) {
			user = userRepository.findById(userId)
					.orElseThrow(() -> new IllegalArgumentException(messageResolver.msg("err.order.userNotFoundForOrder")));
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
				throw new IllegalArgumentException(messageResolver.msg("err.order.invalidCartProduct"));
			}
			if (cartItem.getQuantity() <= 0) {
				throw new IllegalArgumentException(messageResolver.msg("err.order.invalidCartQuantity"));
			}
			Product product = productRepository.findById(cartItem.getProductId())
					.orElseThrow(() -> new IllegalArgumentException(messageResolver.msg("err.order.productNotFoundInCart")));
			if (product.getPrice() == null) {
				throw new IllegalArgumentException(messageResolver.msg("err.order.productPriceInvalid", product.getName()));
			}
			if (product.getQuantity() == null) {
				throw new IllegalArgumentException(messageResolver.msg("err.order.productStockInvalid", product.getName()));
			}
			if (product.getQuantity() < cartItem.getQuantity()) {
				throw new IllegalArgumentException(messageResolver.msg("err.order.productNotEnoughStock", product.getName()));
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
			String details = new StringBuilder("Đơn hàng #")
					.append(savedOrder.getId())
					.append(" đã được tạo")
					.toString();
			userService.recordActivity(userId, "Đặt hàng", details);
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
			throw new IllegalArgumentException(messageResolver.msg("err.order.userInfoNotFound"));
		}
		CustomerOrder order = customerOrderRepository.findDetailByIdAndUserId(orderId, userId)
				.orElseThrow(() -> new IllegalArgumentException(messageResolver.msg("err.order.orderNotFoundToCancel")));

		if (order.getStatus() == OrderStatus.CANCELLED) {
			throw new IllegalArgumentException(messageResolver.msg("err.order.alreadyCancelled"));
		}
		if (order.getStatus() == OrderStatus.COMPLETED || order.getStatus() == OrderStatus.DELIVERING) {
			throw new IllegalArgumentException(messageResolver.msg("err.order.cannotCancelDeliveredCompleted"));
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
		String details = new StringBuffer("Đơn hàng #")
				.append(order.getId())
				.append(" đã được hủy")
				.toString();
		userService.recordActivity(userId, "Hủy đơn", details);
		if (order.getUser() != null && order.getUser().getEmail() != null && !order.getUser().getEmail().isBlank()) {
			Context context = new Context();
			context.setVariable("order", order);
			context.setVariable("recipientName", order.getUser().getFullName());
			String htmlContent = templateEngine.process("email/order-cancelled", context);
			emailService.sendHtmlEmail(
					order.getUser().getEmail(),
					"Đơn hàng #" + order.getId() + " đã được hủy",
					htmlContent);
		}
	}
}