package com.example.productmanager.multilanguage;

import java.util.Locale;

import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public final class MessageResolver {

	private final MessageSource messageSource;

	public final String msg(String key, Object... args) {
		Locale locale = LocaleContextHolder.getLocaleContext() == null
				? Locale.forLanguageTag("vi-VN")
				: LocaleContextHolder.getLocale();
		return messageSource.getMessage(key, args, locale);

		//return messageSource.getMessage(key, args, LocaleContextHolder.getLocale());
	}
}
