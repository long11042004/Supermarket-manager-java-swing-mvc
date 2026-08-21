package com.example.productmanager.lifecycle;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

@Component
@Scope("prototype")
public class PrototypeRequestMarker {

	private String markerId;
	private LocalDateTime createdAt;

	@PostConstruct
	public void init() {
		this.markerId = UUID.randomUUID().toString();
		this.createdAt = LocalDateTime.now();
	}

	@PreDestroy
	public void cleanup() {
		// Prototype beans are not automatically destroyed by Spring container.
	}

	public String getMarkerId() {
		return markerId;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}
}
