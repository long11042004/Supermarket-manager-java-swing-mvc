package com.example.productmanager.email.service;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "app.mail.enabled", havingValue = "false", matchIfMissing = true)
public class ConsoleEmailService implements EmailService {

	@Override
	public void sendSimpleEmail(String to, String subject, String text) {
		if (to == null || to.isBlank()) {
			return;
		}
		System.out.println("========================================");
		System.out.println("[MAIL DEV MODE]");
		System.out.println("To: " + to);
		System.out.println("Subject: " + subject);
		System.out.println("Body:\n" + text);
		System.out.println("========================================");
	}

	@Override
	public void sendHtmlEmail(String to, String subject, String htmlContent) {
		if (to == null || to.isBlank()) {
			return;
		}
		System.out.println("========================================");
		System.out.println("[MAIL DEV MODE - HTML]");
		System.out.println("To: " + to);
		System.out.println("Subject: " + subject);
		System.out.println("Body:\n" + htmlContent);
		System.out.println("========================================");
	}
}
