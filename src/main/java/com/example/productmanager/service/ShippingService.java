package com.example.productmanager.service;

import java.math.BigDecimal;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class ShippingService {

	private final BigDecimal fee;
	private final BigDecimal freeShippingThreshold;

	public ShippingService(
			@Value("${app.shipping.fee:30000}") BigDecimal fee,
			@Value("${app.shipping.free-threshold:300000}") BigDecimal freeShippingThreshold) {
		if (fee == null || fee.signum() < 0 || freeShippingThreshold == null || freeShippingThreshold.signum() <= 0) {
			throw new IllegalArgumentException("Shipping fee must be non-negative and free-shipping threshold must be positive.");
		}
		this.fee = fee;
		this.freeShippingThreshold = freeShippingThreshold;
	}

	public BigDecimal calculateFee(BigDecimal orderAmountAfterDiscount) {
		BigDecimal amount = orderAmountAfterDiscount == null ? BigDecimal.ZERO : orderAmountAfterDiscount;
		return amount.compareTo(freeShippingThreshold) >= 0 ? BigDecimal.ZERO : fee;
	}

	public BigDecimal getFreeShippingThreshold() {
		return freeShippingThreshold;
	}
}
