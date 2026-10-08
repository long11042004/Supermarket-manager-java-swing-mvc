package com.example.productmanager.dto.promotion;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.springframework.format.annotation.DateTimeFormat;

import com.example.productmanager.entity.DiscountType;
import com.example.productmanager.entity.PromotionKind;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PromotionFormDTO {

	private String name;
	private PromotionKind kind;
	private String code;
	private DiscountType discountType;
	private BigDecimal discountValue;
	private BigDecimal minimumOrderAmount;
	private BigDecimal maximumDiscount;

	@DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
	private LocalDateTime startsAt;

	@DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
	private LocalDateTime endsAt;

	private Integer maximumRedemptions;
}
