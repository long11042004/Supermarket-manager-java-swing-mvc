package com.example.productmanager.service;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Iterator;
import java.util.UUID;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class AvatarStorageService {

	public static final String AVATAR_URL_PREFIX = "/uploads/avatars/";
	private static final long MAX_FILE_SIZE_BYTES = 5 * 1024 * 1024;
	private static final int MAX_IMAGE_DIMENSION = 4096;
	private static final long MAX_IMAGE_PIXELS = 16_000_000;

	private final Path uploadDirectory;

	public AvatarStorageService(@Value("${app.upload.avatar-dir:uploads/avatars}") String uploadDirectory) {
		this.uploadDirectory = Path.of(uploadDirectory).toAbsolutePath().normalize();
	}

	public String store(MultipartFile file) throws IOException {
		if (file.isEmpty()) {
			throw new IllegalArgumentException("err.profile.avatarInvalid");
		}
		if (file.getSize() > MAX_FILE_SIZE_BYTES) {
			throw new IllegalArgumentException("err.profile.avatarTooLarge");
		}

		String extension = validateImage(file);
		String filename = UUID.randomUUID() + "." + extension;
		Files.createDirectories(uploadDirectory);
		try (InputStream input = file.getInputStream()) {
			Files.copy(input, uploadDirectory.resolve(filename));
		}
		return AVATAR_URL_PREFIX + filename;
	}

	private String validateImage(MultipartFile file) throws IOException {
		try (InputStream input = file.getInputStream();
				ImageInputStream imageInput = ImageIO.createImageInputStream(input)) {
			if (imageInput == null) {
				throw new IllegalArgumentException("err.profile.avatarInvalid");
			}

			Iterator<ImageReader> readers = ImageIO.getImageReaders(imageInput);
			if (!readers.hasNext()) {
				throw new IllegalArgumentException("err.profile.avatarInvalid");
			}

			ImageReader reader = readers.next();
			try {
				reader.setInput(imageInput);
				String format = reader.getFormatName().toLowerCase(Locale.ROOT);
				if (!format.equals("jpeg") && !format.equals("jpg")
						&& !format.equals("png") && !format.equals("gif")) {
					throw new IllegalArgumentException("err.profile.avatarInvalid");
				}

				int width = reader.getWidth(0);
				int height = reader.getHeight(0);
				if (width > MAX_IMAGE_DIMENSION || height > MAX_IMAGE_DIMENSION
						|| (long) width * height > MAX_IMAGE_PIXELS) {
					throw new IllegalArgumentException("err.profile.avatarTooLarge");
				}

				BufferedImage image = reader.read(0);
				if (image == null) {
					throw new IllegalArgumentException("err.profile.avatarInvalid");
				}
				return format.equals("jpeg") ? "jpg" : format;
			} finally {
				reader.dispose();
			}
		}
	}
}
