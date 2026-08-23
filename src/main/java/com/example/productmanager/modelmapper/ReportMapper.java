package com.example.productmanager.modelmapper;

import com.example.productmanager.dto.report.ReportResponseDTO;
import com.example.productmanager.service.ReportService.ReportData;

public final class ReportMapper {

	private ReportMapper() {
	}

	public static ReportResponseDTO toResponse(ReportData data) {
		if (data == null) {
			return null;
		}
		return new ReportResponseDTO(
				data.totalOrders(),
				data.totalRevenue(),
				data.averageOrderValue(),
				data.guestOrders(),
				data.memberOrders(),
				data.statusStats().stream()
						.map(item -> new ReportResponseDTO.StatusStatDTO(
								item.status() == null ? null : item.status().name(),
								item.total()))
						.toList(),
				data.topProducts().stream()
						.map(item -> new ReportResponseDTO.TopProductStatDTO(
								item.productName(),
								item.unit(),
								item.totalQuantity(),
								item.totalRevenue()))
						.toList());
	}
}
