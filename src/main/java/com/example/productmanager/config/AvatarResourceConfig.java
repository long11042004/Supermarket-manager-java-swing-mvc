package com.example.productmanager.config;

import java.nio.file.Path;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.example.productmanager.service.AvatarStorageService;

@Configuration
public class AvatarResourceConfig implements WebMvcConfigurer {

	private final Path uploadDirectory;

	public AvatarResourceConfig(@Value("${app.upload.avatar-dir:uploads/avatars}") String uploadDirectory) {
		this.uploadDirectory = Path.of(uploadDirectory).toAbsolutePath().normalize();
	}

	@Override
	public void addResourceHandlers(ResourceHandlerRegistry registry) {
		String resourceLocation = uploadDirectory.toUri().toString();
		if (!resourceLocation.endsWith("/")) {
			resourceLocation += "/";
		}
		registry.addResourceHandler(AvatarStorageService.AVATAR_URL_PREFIX + "**")
				.addResourceLocations(resourceLocation);
	}
}
