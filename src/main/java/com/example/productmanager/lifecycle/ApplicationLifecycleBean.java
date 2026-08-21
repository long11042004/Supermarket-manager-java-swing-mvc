package com.example.productmanager.lifecycle;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

@Component
@Scope("singleton")
public class ApplicationLifecycleBean {

	private static final Logger log = LoggerFactory.getLogger(ApplicationLifecycleBean.class);

	@PostConstruct
	public void onStart() {
		log.info("[Lifecycle] ApplicationLifecycleBean initialized");
	}

	@PreDestroy
	public void onShutdown() {
		log.info("[Lifecycle] ApplicationLifecycleBean destroyed");
	}
}
