package com.example.productmanager.email.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@Service
@ConditionalOnProperty(name = "app.mail.enabled", havingValue = "true")
public class SmtpEmailService implements EmailService {

	private final JavaMailSender mailSender;
	private final String fromAddress;
	private final boolean enabled;

	public SmtpEmailService(JavaMailSender mailSender,
			@Value("${app.mail.from:no-reply@demo.local}") String fromAddress,
			@Value("${app.mail.enabled:false}") boolean enabled) {
		this.mailSender = mailSender;
		this.fromAddress = fromAddress;
		this.enabled = enabled;
	}

	@Override
	public void sendSimpleEmail(String to, String subject, String text) {
		if (!enabled || to == null || to.isBlank()) {
			return;
		}
		SimpleMailMessage message = new SimpleMailMessage();
		message.setFrom(fromAddress);
		message.setTo(to);
		message.setSubject(subject);
		message.setText(text);
		mailSender.send(message);
	}

	@Override
	public void sendHtmlEmail(String to, String subject, String htmlContent) {
		if (!enabled || to == null || to.isBlank()) {
			return;
		}
		try {
			MimeMessage message = mailSender.createMimeMessage();
			MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
			helper.setFrom(fromAddress);
			helper.setTo(to);
			helper.setSubject(subject);
			helper.setText(htmlContent, true);
			mailSender.send(message);
		} catch (MessagingException ex) {
			throw new IllegalStateException("Failed to send HTML email confirmation", ex);
		}
	}
}
