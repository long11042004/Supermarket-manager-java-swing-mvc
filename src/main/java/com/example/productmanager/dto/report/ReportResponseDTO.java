package com.example.productmanager.dto.report;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record ReportResponseDTO(
		long totalOrders,
		BigDecimal totalRevenue,
		BigDecimal averageOrderValue,
		long guestOrders,
		long memberOrders,
		long cancelledOrders,
		BigDecimal cancellationRate,
		List<StatusStatDTO> statusStats,
		List<TopProductStatDTO> topProducts,
		List<SalesTrendDTO> salesTrend,
		boolean monthlyTrend) {

	public record StatusStatDTO(String status, Long total) {
	}

	public record TopProductStatDTO(String productName, String unit, Long totalQuantity, BigDecimal totalRevenue) {
	}

	public record SalesTrendDTO(LocalDate periodStart, long orderCount, BigDecimal revenue, int revenueHeightPercent) {
	}
}
