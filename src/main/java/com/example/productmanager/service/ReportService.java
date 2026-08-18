package com.example.productmanager.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.productmanager.model.OrderStatus;
import com.example.productmanager.repository.CustomerOrderRepository;

@Service
public class ReportService {

	private final CustomerOrderRepository customerOrderRepository;

	public ReportService(CustomerOrderRepository customerOrderRepository) {
		this.customerOrderRepository = customerOrderRepository;
	}

	@Transactional(readOnly = true)
	public ReportData generateReport(LocalDate fromDate, LocalDate toDate, OrderStatus statusFilter) {
		LocalDateTime startTime = fromDate.atStartOfDay();
		LocalDateTime endTime = toDate.plusDays(1).atStartOfDay().minusNanos(1);

		long totalOrders = customerOrderRepository.countOrdersInPeriod(startTime, endTime, statusFilter);
		BigDecimal totalRevenue = customerOrderRepository.sumRevenueInPeriod(startTime, endTime, statusFilter);
		long guestOrders = customerOrderRepository.countGuestOrdersInPeriod(startTime, endTime, statusFilter);
		long memberOrders = Math.max(0L, totalOrders - guestOrders);

		BigDecimal averageOrderValue = totalOrders == 0
				? BigDecimal.ZERO
				: totalRevenue.divide(BigDecimal.valueOf(totalOrders), 0, RoundingMode.HALF_UP);

		List<StatusStat> statusStats = customerOrderRepository.countOrdersByStatusInPeriod(startTime, endTime).stream()
				.map(item -> new StatusStat(item.getStatus(), item.getTotal()))
				.toList();

		List<TopProductStat> topProducts = customerOrderRepository
				.findTopProductsInPeriod(startTime, endTime, statusFilter, PageRequest.of(0, 5)).stream()
				.map(item -> new TopProductStat(item.getProductName(), item.getTotalQuantity(), item.getTotalRevenue()))
				.toList();

		return new ReportData(totalOrders, totalRevenue, averageOrderValue, guestOrders, memberOrders, statusStats, topProducts);
	}

	public record ReportData(
			long totalOrders,
			BigDecimal totalRevenue,
			BigDecimal averageOrderValue,
			long guestOrders,
			long memberOrders,
			List<StatusStat> statusStats,
			List<TopProductStat> topProducts) {
	}

	public record StatusStat(OrderStatus status, Long total) {
	}

	public record TopProductStat(String productName, Long totalQuantity, BigDecimal totalRevenue) {
	}
}