package com.example.productmanager.service.emailservice;

public interface EmailService {
	void sendSimpleEmail(String to, String subject, String text);

	void sendHtmlEmail(String to, String subject, String htmlContent);
}
