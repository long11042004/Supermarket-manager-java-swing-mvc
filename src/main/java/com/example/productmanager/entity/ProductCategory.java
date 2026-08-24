package com.example.productmanager.entity;

import java.util.Locale;

import org.springframework.context.i18n.LocaleContextHolder;

public enum ProductCategory {
	SUA("Sữa", "Milk"),
	THUC_PHAM("Thực phẩm", "Food"),
	DO_UONG("Đồ uống", "Beverage"),
	BANH_KEO("Bánh kẹo", "Snacks"),
	RAU_CU("Rau củ", "Vegetables"),
	GIA_VI("Gia vị", "Seasoning"),
	THIT("Thịt", "Meat"),
	THUY_SAN("Thủy sản", "Seafood"),
	TRAI_CAY("Trái cây", "Fruits"),
	CHAM_SOC_CA_NHAN("Chăm sóc cá nhân", "Personal care"),
	DO_GIA_DUNG("Đồ gia dụng", "Household"),
	BANH("Bánh", "Bread");

	private final String labelVi;
	private final String labelEn;

	ProductCategory(String labelVi, String labelEn) {
		this.labelVi = labelVi;
		this.labelEn = labelEn;
	}

	public String getLabel() {
		return getLabel(LocaleContextHolder.getLocale());
	}

	public String getLabel(Locale locale) {
		if (locale != null && "en".equalsIgnoreCase(locale.getLanguage())) {
			return labelEn;
		}
		return labelVi;
	}

	@Override
	public String toString() {
		return getLabel();
	}

	public static ProductCategory fromValue(String value) {
		if (value == null || value.isBlank()) {
			return null;
		}
		String normalized = value.trim();
		for (ProductCategory category : values()) {
			if (category.name().equalsIgnoreCase(normalized)
					|| category.labelVi.equalsIgnoreCase(normalized)
					|| category.labelEn.equalsIgnoreCase(normalized)) {
				return category;
			}
		}
		return null;
	}
}
