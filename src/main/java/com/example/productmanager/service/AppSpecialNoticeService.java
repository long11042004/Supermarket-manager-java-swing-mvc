package com.example.productmanager.service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

@Service
public class AppSpecialNoticeService {

	private volatile Notice currentNotice;
	private final Map<Long, Notice> userNotices = new ConcurrentHashMap<>();

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

	public void publishUserWarning(Long userId, String message) {
		publishForUser(userId, "warning", message);
	}

	public void publishUserInfo(Long userId, String message) {
		publishForUser(userId, "info", message);
	}

	public Notice getUserNotice(Long userId) {
		if (userId == null) {
			return null;
		}
		return userNotices.get(userId);
	}

	public void clearUserNotice(Long userId) {
		if (userId == null) {
			return;
		}
		userNotices.remove(userId);
	}

	private void publish(String level, String message) {
		if (message == null || message.isBlank()) {
			return;
		}
		currentNotice = new Notice(level, message, LocalDateTime.now());
	}

	private void publishForUser(Long userId, String level, String message) {
		if (userId == null || message == null || message.isBlank()) {
			return;
		}
		userNotices.put(userId, new Notice(level, message, LocalDateTime.now()));
	}

	public record Notice(String level, String message, LocalDateTime updatedAt) {
	}
}
