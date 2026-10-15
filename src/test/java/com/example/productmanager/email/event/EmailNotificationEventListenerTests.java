package com.example.productmanager.email.event;

import static org.mockito.ArgumentMatchers.eq;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.mockito.ArgumentCaptor;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import com.example.productmanager.email.jms.EmailNotificationMessage;
import com.example.productmanager.email.jms.EmailNotificationType;
import com.example.productmanager.entity.RoleName;
import com.example.productmanager.entity.User;
import com.example.productmanager.repository.UserRepository;

class EmailNotificationEventListenerTests {

	@Test
	void promotionAnnouncementsAreQueuedOnlyForSubscribedCustomers() {
		JmsTemplate jmsTemplate = org.mockito.Mockito.mock(JmsTemplate.class);
		UserRepository userRepository = org.mockito.Mockito.mock(UserRepository.class);
		EmailNotificationEventListener listener = new EmailNotificationEventListener(jmsTemplate, userRepository);
		ReflectionTestUtils.setField(listener, "emailQueue", "email.notification.queue");
		User subscriber = User.builder()
				.id(8L)
				.username("subscriber")
				.fullName("Subscribed Customer")
				.email("subscriber@example.com")
				.promotionEmailsEnabled(true)
				.build();
		when(userRepository.findPromotionEmailSubscribers(RoleName.CUSTOMER)).thenReturn(List.of(subscriber));

		listener.handlePromotionAnnouncedEmailEvent(new PromotionAnnouncedEmailEvent(
				"Weekend offer",
				"SAVE10",
				"COUPON",
				"PERCENTAGE",
				new BigDecimal("10"),
				new BigDecimal("100000"),
				LocalDateTime.of(2026, 10, 1, 0, 0),
				LocalDateTime.of(2026, 10, 31, 23, 59)));

		verify(userRepository).findPromotionEmailSubscribers(RoleName.CUSTOMER);
		ArgumentCaptor<Object> messageCaptor = ArgumentCaptor.forClass(Object.class);
		verify(jmsTemplate).convertAndSend(eq("email.notification.queue"), messageCaptor.capture());
		EmailNotificationMessage message = assertInstanceOf(EmailNotificationMessage.class, messageCaptor.getValue());
		assertEquals(EmailNotificationType.PROMOTION_ANNOUNCED, message.type());
		assertEquals("subscriber@example.com", message.recipientEmail());
		assertEquals("SAVE10", message.promotion().code());
	}
}
