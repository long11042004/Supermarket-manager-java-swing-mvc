package com.example.productmanager.jms;

import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import com.example.productmanager.email.jms.EmailNotificationJmsListener;
import com.example.productmanager.email.jms.EmailNotificationMessage;
import com.example.productmanager.email.jms.EmailNotificationType;
import com.example.productmanager.email.service.EmailService;

class EmailNotificationJmsListenerTests {

    @Test
    void shouldSendOrderConfirmationEmailWhenMessageTypeIsOrderConfirmed() {
        EmailService emailService = mock(EmailService.class);
        SpringTemplateEngine templateEngine = mock(SpringTemplateEngine.class);
        EmailNotificationJmsListener listener = new EmailNotificationJmsListener(emailService, templateEngine);

        when(templateEngine.process(eq("email/order-confirmation"), any(Context.class))).thenReturn("<html>ok</html>");

        listener.handle(new EmailNotificationMessage(
                EmailNotificationType.ORDER_CONFIRMED,
                "customer@example.com",
                "Khách hàng",
                null,
                42L,
                "PENDING",
                "123 Nguyễn Trãi"));

        verify(emailService).sendHtmlEmail(eq("customer@example.com"), eq("Xác nhận đơn hàng #42"), eq("<html>ok</html>"));
    }
}
