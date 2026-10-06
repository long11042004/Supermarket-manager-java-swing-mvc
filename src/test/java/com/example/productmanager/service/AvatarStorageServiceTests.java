package com.example.productmanager.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

class AvatarStorageServiceTests {

	@TempDir
	Path tempDirectory;

	@Test
	void storeSavesValidatedImageAndReturnsItsUrl() throws IOException {
		AvatarStorageService storageService = new AvatarStorageService(tempDirectory.toString());
		MockMultipartFile file = new MockMultipartFile(
				"avatarFile", "avatar.png", "image/png", createPngImage());

		String avatarUrl = storageService.store(file);

		assertTrue(avatarUrl.startsWith(AvatarStorageService.AVATAR_URL_PREFIX));
		assertTrue(avatarUrl.endsWith(".png"));
		assertTrue(Files.exists(tempDirectory.resolve(avatarUrl.substring(
				AvatarStorageService.AVATAR_URL_PREFIX.length()))));
	}

	@Test
	void storeRejectsNonImageFiles() throws IOException {
		AvatarStorageService storageService = new AvatarStorageService(tempDirectory.toString());
		MockMultipartFile file = new MockMultipartFile(
				"avatarFile", "avatar.png", "image/png", "not an image".getBytes());

		IllegalArgumentException exception = assertThrows(
				IllegalArgumentException.class, () -> storageService.store(file));

		assertEquals("err.profile.avatarInvalid", exception.getMessage());
		try (var storedFiles = Files.list(tempDirectory)) {
			assertTrue(storedFiles.findAny().isEmpty());
		}
	}

	private byte[] createPngImage() throws IOException {
		BufferedImage image = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
		try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
			ImageIO.write(image, "png", output);
			return output.toByteArray();
		}
	}
}
