package com.example.productmanager.exception;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import jakarta.servlet.http.HttpServletRequest;

@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<ApiError> handleIllegalArgumentException(IllegalArgumentException ex,
			HttpServletRequest request) {
		return ResponseEntity.status(HttpStatus.BAD_REQUEST)
				.body(new ApiError(
						"BAD_REQUEST",
						ex.getMessage(),
						HttpStatus.BAD_REQUEST.value(),
						request.getRequestURI()));
	}

	@ExceptionHandler(RuntimeException.class)
	public ResponseEntity<ApiError> handleRuntimeException(RuntimeException ex,
			HttpServletRequest request) {
		return ResponseEntity.status(HttpStatus.NOT_FOUND)
				.body(new ApiError(
						"NOT_FOUND",
						ex.getMessage(),
						HttpStatus.NOT_FOUND.value(),
						request.getRequestURI()));
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex,
			HttpServletRequest request) {
		Map<String, String> fieldErrors = new HashMap<>();
		ex.getBindingResult().getFieldErrors()
				.forEach(error -> fieldErrors.put(error.getField(), error.getDefaultMessage()));

		Map<String, Object> response = new HashMap<>();
		response.put("error", "VALIDATION_ERROR");
		response.put("message", "Dữ liệu không hợp lệ");
		response.put("status", HttpStatus.BAD_REQUEST.value());
		response.put("path", request.getRequestURI());
		response.put("timestamp", java.time.LocalDateTime.now());
		response.put("errors", fieldErrors);

		return ResponseEntity.badRequest().body(response);
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ApiError> handleGeneralException(Exception ex,
			HttpServletRequest request) {
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.body(new ApiError(
						"INTERNAL_SERVER_ERROR",
						"Có lỗi xảy ra trong hệ thống",
						HttpStatus.INTERNAL_SERVER_ERROR.value(),
						request.getRequestURI()));
	}
}