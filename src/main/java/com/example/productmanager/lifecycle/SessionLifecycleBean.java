package com.example.productmanager.lifecycle;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Component;
import org.springframework.web.context.WebApplicationContext;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

@Component
@Scope(value = WebApplicationContext.SCOPE_SESSION, proxyMode = ScopedProxyMode.TARGET_CLASS)
public class SessionLifecycleBean implements Serializable {

	private static final long serialVersionUID = 1L;

	private String sessionToken;
	private LocalDateTime createdAt;
	private int visitCount;

	@PostConstruct
	public void init() {
		this.sessionToken = UUID.randomUUID().toString();
		this.createdAt = LocalDateTime.now();
		this.visitCount = 0;
	}

	@PreDestroy
	public void cleanup() {
		// Called when HTTP session is invalidated/expired.
	}

	public int increaseAndGetVisitCount() {
		visitCount++;
		return visitCount;
	}

	public String getSessionToken() {
		return sessionToken;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}
}
