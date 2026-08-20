package com.example.productmanager.service;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;

@Service
public class AppSpecialNoticeService {

	private volatile Notice currentNotice;

	public void publishInfo(String message) {
		publish("info", message);
	}

	public void publishSuccess(String message) {
		publish("success", message);
	}

	public void publishWarning(String message) {
		publish("warning", message);
	}

	public void clear() {
		currentNotice = null;
	}

	public Notice getCurrentNotice() {
		return currentNotice;
	}

	private void publish(String level, String message) {
		if (message == null || message.isBlank()) {
			return;
		}
		currentNotice = new Notice(level, message, LocalDateTime.now());
	}

	public record Notice(String level, String message, LocalDateTime updatedAt) {
	}
}
