package com.example.productmanager.dto.report;

import java.math.BigDecimal;
import java.util.List;

public record ReportResponseDTO(
		long totalOrders,
		BigDecimal totalRevenue,
		BigDecimal averageOrderValue,
		long guestOrders,
		long memberOrders,
		List<StatusStatDTO> statusStats,
		List<TopProductStatDTO> topProducts) {

	public record StatusStatDTO(String status, Long total) {
	}

	public record TopProductStatDTO(String productName, String unit, Long totalQuantity, BigDecimal totalRevenue) {
	}
}
