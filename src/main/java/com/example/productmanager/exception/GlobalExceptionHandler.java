package com.example.productmanager.exception;

import java.util.HashMap;
import java.util.Map;

import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.example.productmanager.multilanguage.MessageResolver;

import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;

@RestControllerAdvice
@AllArgsConstructor
public class GlobalExceptionHandler {
	private final MessageResolver messageResolver;

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

	@ExceptionHandler(NotFoundException.class)
	public ResponseEntity<ApiError> handleNotFoundException(NotFoundException ex,
			HttpServletRequest request) {
		return ResponseEntity.status(HttpStatus.NOT_FOUND)
				.body(new ApiError(
						"NOT_FOUND",
						ex.getMessage(),
						HttpStatus.NOT_FOUND.value(),
						request.getRequestURI()));
	}

	@ExceptionHandler(ConflictException.class)
	public ResponseEntity<ApiError> handleConflictException(ConflictException ex,
			HttpServletRequest request) {
		return ResponseEntity.status(HttpStatus.CONFLICT)
				.body(new ApiError(
						"CONFLICT",
						ex.getMessage(),
						HttpStatus.CONFLICT.value(),
						request.getRequestURI()));
	}

	@ExceptionHandler(ForbiddenException.class)
	public ResponseEntity<ApiError> handleForbiddenException(ForbiddenException ex,
			HttpServletRequest request) {
		return ResponseEntity.status(HttpStatus.FORBIDDEN)
				.body(new ApiError(
						"FORBIDDEN",
						ex.getMessage(),
						HttpStatus.FORBIDDEN.value(),
						request.getRequestURI()));
	}

	@ExceptionHandler(BadCredentialsException.class)
	public ResponseEntity<ApiError> handleBadCredentialsException(BadCredentialsException ex,
			HttpServletRequest request) {
		return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
				.body(new ApiError(
						"BAD_CREDENTIALS",
						ex.getMessage(),
						HttpStatus.UNAUTHORIZED.value(),
						request.getRequestURI()));
	}

	@ExceptionHandler(OptimisticLockingFailureException.class)
	public ResponseEntity<ApiError> handleOptimisticLockingFailureException(OptimisticLockingFailureException ex,
			HttpServletRequest request) {
		return ResponseEntity.status(HttpStatus.CONFLICT)
				.body(new ApiError(
						"CONFLICT",
						messageResolver.msg("msg.order.checkoutConcurrentUpdate"),
						HttpStatus.CONFLICT.value(),
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
		response.put("message", messageResolver.msg("err.validation.invalidData"));
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
						messageResolver.msg("err.system.internal"),
						HttpStatus.INTERNAL_SERVER_ERROR.value(),
						request.getRequestURI()));
	}
}