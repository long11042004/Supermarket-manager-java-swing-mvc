package com.example.productmanager.exception;

import java.time.LocalDateTime;

public class ApiError {

	private final String error;
	private final String message;
	private final int status;
	private final String path;
	private final LocalDateTime timestamp;

	public ApiError(String error, String message, int status, String path) {
		this.error = error;
		this.message = message;
		this.status = status;
		this.path = path;
		this.timestamp = LocalDateTime.now();
	}

	public String getError() {
		return error;
	}

	public String getMessage() {
		return message;
	}

	public int getStatus() {
		return status;
	}

	public String getPath() {
		return path;
	}

	public LocalDateTime getTimestamp() {
		return timestamp;
	}
}