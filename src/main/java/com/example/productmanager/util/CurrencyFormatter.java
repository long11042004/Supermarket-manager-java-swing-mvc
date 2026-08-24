package com.example.productmanager.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

import org.springframework.context.i18n.LocaleContextHolder;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class CurrencyFormatter {

	private static final BigDecimal USD_RATE = new BigDecimal("26000");

	public static String formatMoney(BigDecimal amount) {
		return formatMoney(amount, LocaleContextHolder.getLocale());
	}

	public static String formatMoney(BigDecimal amount, Locale locale) {
		BigDecimal displayAmount = normalizeAmount(amount, locale);
		String symbol = isEnglish(locale) ? "$" : "₫";
		String formatted = formatNumber(displayAmount);
		return symbol + " " + formatted;
	}

	private static BigDecimal normalizeAmount(BigDecimal amount, Locale locale) {
		if (amount == null) {
			return BigDecimal.ZERO;
		}
		if (isEnglish(locale)) {
			return amount.divide(USD_RATE, 0, RoundingMode.HALF_UP);
		}
		return amount;
	}

	private static boolean isEnglish(Locale locale) {
		return locale != null && "en".equalsIgnoreCase(locale.getLanguage());
	}

	private static String formatNumber(BigDecimal value) {
		DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.US);
		DecimalFormat formatter = new DecimalFormat("#,##0", symbols);
		return formatter.format(value);
	}
}
