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
		publish("info", message, null, new Object[0]);
	}

	public void publishSuccess(String message) {
		publish("success", message, null, new Object[0]);
	}

	public void publishWarning(String message) {
		publish("warning", message, null, new Object[0]);
	}

	public void publishInfoKey(String messageKey, Object... args) {
		publish("info", null, messageKey, args);
	}

	public void publishSuccessKey(String messageKey, Object... args) {
		publish("success", null, messageKey, args);
	}

	public void publishWarningKey(String messageKey, Object... args) {
		publish("warning", null, messageKey, args);
	}

	public void clear() {
		currentNotice = null;
	}

	public Notice getCurrentNotice() {
		return currentNotice;
	}

	public void publishUserWarning(Long userId, String message) {
		publishForUser(userId, "warning", message, null, new Object[0]);
	}

	public void publishUserInfo(Long userId, String message) {
		publishForUser(userId, "info", message, null, new Object[0]);
	}

	public void publishUserWarningKey(Long userId, String messageKey, Object... args) {
		publishForUser(userId, "warning", null, messageKey, args);
	}

	public void publishUserInfoKey(Long userId, String messageKey, Object... args) {
		publishForUser(userId, "info", null, messageKey, args);
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

	private void publish(String level, String message, String messageKey, Object... args) {
		if ((message == null || message.isBlank()) && (messageKey == null || messageKey.isBlank())) {
			return;
		}
		currentNotice = new Notice(level, message, messageKey, args, LocalDateTime.now());
	}

	private void publishForUser(Long userId, String level, String message, String messageKey, Object... args) {
		if (userId == null) {
			return;
		}
		if ((message == null || message.isBlank()) && (messageKey == null || messageKey.isBlank())) {
			return;
		}
		userNotices.put(userId, new Notice(level, message, messageKey, args, LocalDateTime.now()));
	}

	public record Notice(String level, String message, String messageKey, Object[] args, LocalDateTime updatedAt) {
	}
}
