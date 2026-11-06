package com.example.productmanager.view;

import java.util.ArrayList;
import java.util.List;

import com.example.productmanager.entity.PaymentMethod;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CounterSaleForm {

	private List<Long> productIds = new ArrayList<>();
	private List<Integer> quantities = new ArrayList<>(List.of(1));
	private String customerName;
	private String contactPhone;
	private String note;
	private PaymentMethod paymentMethod = PaymentMethod.CASH;
	private String keyword;
	private int page;

	public CounterSaleForm() {
		productIds.add(null);
	}
}
