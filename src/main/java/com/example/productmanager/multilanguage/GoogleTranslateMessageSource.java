package com.example.productmanager.multilanguage;

import java.text.MessageFormat;
import java.util.Locale;

import org.springframework.context.support.AbstractMessageSource;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.stereotype.Component;

import com.example.productmanager.service.GoogleTranslateService;

@Component("messageSource")
public class GoogleTranslateMessageSource extends AbstractMessageSource {

	private static final Locale SOURCE_LOCALE = Locale.forLanguageTag("vi-VN");

	private final ResourceBundleMessageSource vietnameseMessages;
	private final ResourceBundleMessageSource englishMessages;
	private final GoogleTranslateService googleTranslateService;

	public GoogleTranslateMessageSource(GoogleTranslateService googleTranslateService) {
		this.googleTranslateService = googleTranslateService;
		this.vietnameseMessages = new ResourceBundleMessageSource();
		this.vietnameseMessages.setBasename("messages_vi");
		this.vietnameseMessages.setDefaultEncoding("UTF-8");
		this.vietnameseMessages.setFallbackToSystemLocale(false);
		this.englishMessages = new ResourceBundleMessageSource();
		this.englishMessages.setBasename("messages_en");
		this.englishMessages.setDefaultEncoding("UTF-8");
		this.englishMessages.setFallbackToSystemLocale(false);
	}

	@Override
	protected String resolveCodeWithoutArguments(String code, Locale locale) {
		return resolveMessage(code, locale);
	}

	@Override
	protected MessageFormat resolveCode(String code, Locale locale) {
		String message = resolveMessage(code, locale);
		return message == null ? null : new MessageFormat(message, locale);
	}

	private String resolveMessage(String code, Locale locale) {
		String sourceMessage = vietnameseMessages.getMessage(code, null, null, SOURCE_LOCALE);
		if (locale.getLanguage().equals("vi")) {
			return sourceMessage;
		}
		if (locale.getLanguage().equals("en")) {
			String englishMessage = englishMessages.getMessage(code, null, null, locale);
			if (englishMessage != null) {
				return englishMessage;
			}
		}
		if (sourceMessage == null) {
			return null;
		}
		return googleTranslateService.translate(sourceMessage, locale.getLanguage());
	}
}
