package com.example.productmanager.jms;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

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
                new EmailNotificationMessage.OrderNotice(
                        42L,
                        "PENDING",
                        "123 Nguyễn Trãi",
                        new BigDecimal("50000"),
                        new BigDecimal("40000"),
                        new BigDecimal("5000"),
                        new BigDecimal("10000"),
                        "SAVE5",
                        "Ưu đãi mùa thu"),
                null,
                null));

        verify(emailService).sendHtmlEmail(eq("customer@example.com"), eq("Xác nhận đơn hàng #42"), eq("<html>ok</html>"));
        ArgumentCaptor<Context> contextCaptor = ArgumentCaptor.forClass(Context.class);
        verify(templateEngine).process(eq("email/order-confirmation"), contextCaptor.capture());
        Context context = contextCaptor.getValue();
        Object order = context.getVariable("order");
        SpelExpressionParser expressionParser = new SpelExpressionParser();

        assertEquals(new BigDecimal("5000"), expressionParser.parseExpression("discountAmount").getValue(order));
        assertEquals(new BigDecimal("10000"), expressionParser.parseExpression("shippingFee").getValue(order));
        assertEquals("SAVE5", expressionParser.parseExpression("couponCode").getValue(order));
        assertEquals("Ưu đãi mùa thu", expressionParser.parseExpression("automaticPromotionName").getValue(order));
        assertEquals("40000", context.getVariable("formattedSubtotal"));
        assertEquals("5000", context.getVariable("formattedDiscount"));
        assertEquals("10000", context.getVariable("formattedShippingFee"));
        assertEquals("50000", context.getVariable("formattedTotal"));
    }

    @Test
    void shouldSendOrderStatusNotification() {
        EmailService emailService = mock(EmailService.class);
        SpringTemplateEngine templateEngine = mock(SpringTemplateEngine.class);
        EmailNotificationJmsListener listener = new EmailNotificationJmsListener(emailService, templateEngine);
        when(templateEngine.process(eq("email/order-status-updated"), any(Context.class))).thenReturn("<html>status</html>");

        listener.handle(new EmailNotificationMessage(
                EmailNotificationType.ORDER_STATUS_UPDATED,
                "customer@example.com",
                "Customer",
                null,
                new EmailNotificationMessage.OrderNotice(42L, "DELIVERING", "123 Street", null, null, null, null, null, null),
                null,
                null));

        verify(emailService).sendHtmlEmail("customer@example.com", "Cập nhật đơn hàng #42", "<html>status</html>");
    }

    @Test
    void shouldSendAccountSecurityNotification() {
        EmailService emailService = mock(EmailService.class);
        SpringTemplateEngine templateEngine = mock(SpringTemplateEngine.class);
        EmailNotificationJmsListener listener = new EmailNotificationJmsListener(emailService, templateEngine);
        when(templateEngine.process(eq("email/account-security"), any(Context.class))).thenReturn("<html>security</html>");

        listener.handle(new EmailNotificationMessage(
                EmailNotificationType.ACCOUNT_SECURITY,
                "customer@example.com",
                "Customer",
                null,
                null,
                new EmailNotificationMessage.AccountSecurityNotice("PASSWORD_CHANGED", null, null),
                null));

        verify(emailService).sendHtmlEmail(
                "customer@example.com",
                "Thông báo bảo mật tài khoản",
                "<html>security</html>");
    }

    @Test
    void shouldSendPromotionAnnouncement() {
        EmailService emailService = mock(EmailService.class);
        SpringTemplateEngine templateEngine = mock(SpringTemplateEngine.class);
        EmailNotificationJmsListener listener = new EmailNotificationJmsListener(emailService, templateEngine);
        when(templateEngine.process(eq("email/promotion-announcement"), any(Context.class))).thenReturn("<html>promotion</html>");

        listener.handle(new EmailNotificationMessage(
                EmailNotificationType.PROMOTION_ANNOUNCED,
                "customer@example.com",
                "Customer",
                null,
                null,
                null,
                new EmailNotificationMessage.PromotionNotice(
                        "Autumn offer",
                        "AUTUMN10",
                        "COUPON",
                        "PERCENTAGE",
                        new BigDecimal("10"),
                        new BigDecimal("100000"),
                        LocalDateTime.of(2026, 10, 1, 0, 0),
                        LocalDateTime.of(2026, 10, 31, 23, 59))));

        verify(emailService).sendHtmlEmail(
                "customer@example.com",
                "Ưu đãi mới: Autumn offer",
                "<html>promotion</html>");
    }

    @Test
    void shouldRenderAllNewEmailTemplates() {
        EmailService emailService = mock(EmailService.class);
        SpringTemplateEngine templateEngine = createTemplateEngine();
        EmailNotificationJmsListener listener = new EmailNotificationJmsListener(emailService, templateEngine);

        listener.handle(new EmailNotificationMessage(
                EmailNotificationType.ORDER_STATUS_UPDATED,
                "customer@example.com",
                "Customer",
                null,
                new EmailNotificationMessage.OrderNotice(42L, "DELIVERING", "123 Street", null, null, null, null, null, null),
                null,
                null));
        listener.handle(new EmailNotificationMessage(
                EmailNotificationType.ACCOUNT_SECURITY,
                "customer@example.com",
                "Customer",
                null,
                null,
                new EmailNotificationMessage.AccountSecurityNotice("EMAIL_CHANGED", "old@example.com", "new@example.com"),
                null));
        listener.handle(new EmailNotificationMessage(
                EmailNotificationType.PROMOTION_ANNOUNCED,
                "customer@example.com",
                "Customer",
                null,
                null,
                null,
                new EmailNotificationMessage.PromotionNotice(
                        "Autumn offer",
                        "AUTUMN10",
                        "COUPON",
                        "PERCENTAGE",
                        new BigDecimal("10"),
                        new BigDecimal("100000"),
                        LocalDateTime.of(2026, 10, 1, 0, 0),
                        LocalDateTime.of(2026, 10, 31, 23, 59))));

        verify(emailService, org.mockito.Mockito.times(3))
                .sendHtmlEmail(eq("customer@example.com"), any(String.class), any(String.class));
    }

    private SpringTemplateEngine createTemplateEngine() {
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode(TemplateMode.HTML);
        resolver.setCharacterEncoding("UTF-8");
        SpringTemplateEngine engine = new SpringTemplateEngine();
        engine.setTemplateResolver(resolver);
        return engine;
    }
}
