package com.example.productmanager.service;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.HtmlUtils;
import org.springframework.web.util.UriComponentsBuilder;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Service
public class GoogleTranslateService {

	private static final String TRANSLATE_ENDPOINT = "https://translate.googleapis.com/translate_a/single";

	private final RestClient restClient;
	private final ObjectMapper objectMapper;
	private final ConcurrentMap<TranslationKey, String> translations = new ConcurrentHashMap<>();

	@Autowired
	public GoogleTranslateService() {
		this(RestClient.create(), new ObjectMapper());
	}

	GoogleTranslateService(RestClient restClient, ObjectMapper objectMapper) {
		this.restClient = restClient;
		this.objectMapper = objectMapper;
	}

	public String translate(String text, String targetLanguage) {
		String normalizedLanguage = targetLanguage.toLowerCase(Locale.ROOT);
		if (normalizedLanguage.equals("vi")) {
			return text;
		}
		return translations.computeIfAbsent(new TranslationKey(text, normalizedLanguage), this::translateText);
	}

	private String translateText(TranslationKey key) {
		URI uri = UriComponentsBuilder.fromUriString(TRANSLATE_ENDPOINT)
				.queryParam("client", "gtx")
				.queryParam("sl", "vi")
				.queryParam("tl", key.targetLanguage())
				.queryParam("dt", "t")
				.queryParam("q", key.text())
				.encode(StandardCharsets.UTF_8)
				.build()
				.toUri();
		String response = restClient.get()
				.uri(uri)
				.retrieve()
				.body(String.class);
		if (response == null || response.isBlank()) {
			throw new IllegalStateException("Google Translate API returned no translated text.");
		}

		try {
			JsonNode segments = objectMapper.readTree(response).path(0);
			StringBuilder translatedText = new StringBuilder();
			if (segments.isArray()) {
				for (JsonNode segment : segments) {
					if (segment.isArray() && !segment.isEmpty() && segment.get(0).isString()) {
						translatedText.append(segment.get(0).asString());
					}
				}
			}
			if (translatedText.isEmpty()) {
				throw new IllegalStateException("Google Translate API returned an unexpected response.");
			}
			return HtmlUtils.htmlUnescape(translatedText.toString());
		} catch (JacksonException ex) {
			throw new IllegalStateException("Google Translate API returned invalid JSON.", ex);
		}
	}

	private record TranslationKey(String text, String targetLanguage) {
	}
}
