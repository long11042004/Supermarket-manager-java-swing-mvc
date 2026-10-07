package com.example.productmanager.service;

import java.util.Locale;

import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;

import com.example.productmanager.entity.Product;

@Service
public class ProductDisplayService {

	private final GoogleTranslateService googleTranslateService;

	public ProductDisplayService(GoogleTranslateService googleTranslateService) {
		this.googleTranslateService = googleTranslateService;
	}

	public String displayName(Product product) {
		Locale locale = LocaleContextHolder.getLocale();
		if (locale != null && "en".equalsIgnoreCase(locale.getLanguage())) {
			return englishText(product.getNameVi(), product.getNameEn());
		}
		return hasText(product.getNameVi()) ? product.getNameVi() : product.getNameEn();
	}

	public String displayUnit(Product product) {
		Locale locale = LocaleContextHolder.getLocale();
		if (locale != null && "en".equalsIgnoreCase(locale.getLanguage())) {
			return englishText(product.getUnitVi(), product.getUnitEn());
		}
		return hasText(product.getUnitVi()) ? product.getUnitVi() : product.getUnitEn();
	}

	public String englishName(Product product) {
		return englishText(product.getNameVi(), product.getNameEn());
	}

	public String englishUnit(Product product) {
		return englishText(product.getUnitVi(), product.getUnitEn());
	}

	private String englishText(String vietnameseText, String englishText) {
		if (hasText(englishText)) {
			return englishText;
		}
		if (hasText(vietnameseText)) {
			return googleTranslateService.translate(vietnameseText, "en");
		}
		return englishText;
	}

	private boolean hasText(String value) {
		return value != null && !value.isBlank();
	}
}
