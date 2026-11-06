package com.example.productmanager.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.ui.ExtendedModelMap;

import com.example.productmanager.lifecycle.PrototypeRequestMarker;
import com.example.productmanager.lifecycle.SessionLifecycleBean;
import com.example.productmanager.service.ProductService;
import com.example.productmanager.service.PromotionService;
import com.example.productmanager.service.UserService;

import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class DashboardControllerTests {

	@Mock
	private ProductService productService;

	@Mock
	private PromotionService promotionService;

	@Mock
	private UserService userService;

	@Mock
	private SessionLifecycleBean sessionLifecycleBean;

	@Mock
	private ObjectProvider<PrototypeRequestMarker> prototypeRequestMarkerProvider;

	@AfterEach
	void clearSecurityContext() {
		SecurityContextHolder.clearContext();
	}

	@Test
	void guestDashboardEnablesGuestCheckoutAndLoadsPublicStorefrontData() {
		DashboardController controller = new DashboardController(
				productService,
				promotionService,
				userService,
				sessionLifecycleBean,
				prototypeRequestMarkerProvider);
		MockHttpSession session = new MockHttpSession();
		ExtendedModelMap model = new ExtendedModelMap();
		when(productService.getFilteredProducts(null, null, 0, 12))
				.thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 12), 8));
		when(productService.getFeaturedProducts(6)).thenReturn(List.of());
		when(promotionService.getFeaturedPromotions()).thenReturn(List.of());

		String view = controller.guestDashboard(model, session);

		assertEquals("guest-dashboard", view);
		assertTrue(Boolean.TRUE.equals(session.getAttribute("guestCheckout")));
		assertEquals(8L, model.getAttribute("totalProducts"));
		assertEquals(List.of(), model.getAttribute("featuredProducts"));
		assertEquals(List.of(), model.getAttribute("featuredPromotions"));
		verify(productService).getFeaturedProducts(6);
		verify(promotionService).getFeaturedPromotions();
	}
}
