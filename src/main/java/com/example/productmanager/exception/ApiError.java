package com.example.productmanager.exception;

import java.time.LocalDateTime;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class ApiError {

	private final String error;
	private final String message;
	private final int status;
	private final String path;
	private final LocalDateTime timestamp = LocalDateTime.now();
}