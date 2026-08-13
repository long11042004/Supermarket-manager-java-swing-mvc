package com.example.productmanager.exception;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(RuntimeException.class)
	public ResponseEntity<ApiError> handleRuntimeException(RuntimeException ex) {
		return ResponseEntity.status(HttpStatus.NOT_FOUND)
				.body(new ApiError(ex.getMessage(), HttpStatus.NOT_FOUND.value()));
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
		Map<String, String> fieldErrors = new HashMap<>();
		ex.getBindingResult().getFieldErrors()
				.forEach(error -> fieldErrors.put(error.getField(), error.getDefaultMessage()));

		Map<String, Object> response = new HashMap<>();
		response.put("message", "Du lieu khong hop le");
		response.put("status", HttpStatus.BAD_REQUEST.value());
		response.put("errors", fieldErrors);

		return ResponseEntity.badRequest().body(response);
	}
}