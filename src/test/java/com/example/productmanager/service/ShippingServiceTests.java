package com.example.productmanager.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

class ShippingServiceTests {

	private final ShippingService shippingService = new ShippingService(
			new BigDecimal("30000"), new BigDecimal("300000"));

	@Test
	void chargesFixedFeeBelowFreeShippingThreshold() {
		assertEquals(new BigDecimal("30000"), shippingService.calculateFee(new BigDecimal("299999.99")));
	}

	@Test
	void waivesFeeAtFreeShippingThreshold() {
		assertEquals(BigDecimal.ZERO, shippingService.calculateFee(new BigDecimal("300000")));
	}

	@Test
	void chargesFeeWhenOrderIsNullOrEmpty() {
		assertEquals(new BigDecimal("30000"), shippingService.calculateFee(null));
		assertEquals(new BigDecimal("30000"), shippingService.calculateFee(BigDecimal.ZERO));
	}
}
