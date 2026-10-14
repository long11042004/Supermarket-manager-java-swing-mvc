package com.example.productmanager.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;

import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.productmanager.entity.OrderStatus;
import com.example.productmanager.repository.CustomerOrderRepository;
import com.example.productmanager.repository.CustomerOrderRepository.DailySalesProjection;

import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class ReportService {

	private final CustomerOrderRepository customerOrderRepository;

	@Async("reportTaskExecutor")
	public CompletableFuture<ReportData> generateReportAsync(LocalDate fromDate, LocalDate toDate, OrderStatus statusFilter) {
		return CompletableFuture.completedFuture(generateReport(fromDate, toDate, statusFilter));
	}

	@Transactional(readOnly = true)
	public ReportData generateReport(LocalDate fromDate, LocalDate toDate, OrderStatus statusFilter) {
		LocalDateTime startTime = fromDate.atStartOfDay();
		LocalDateTime endTime = toDate.plusDays(1).atStartOfDay().minusNanos(1);

		long totalOrders = customerOrderRepository.countOrdersInPeriod(startTime, endTime, statusFilter);
		BigDecimal totalRevenue = customerOrderRepository.sumRevenueInPeriod(startTime, endTime, statusFilter);
		if (totalRevenue == null) {
			totalRevenue = BigDecimal.ZERO;
		}
		long guestOrders = customerOrderRepository.countGuestOrdersInPeriod(startTime, endTime, statusFilter);
		long memberOrders = Math.max(0L, totalOrders - guestOrders);

		BigDecimal averageOrderValue = totalOrders == 0
				? BigDecimal.ZERO
				: totalRevenue.divide(BigDecimal.valueOf(totalOrders), 0, RoundingMode.HALF_UP);

		List<StatusStat> statusStats = customerOrderRepository.countOrdersByStatusInPeriod(startTime, endTime, statusFilter).stream()
				.map(item -> new StatusStat(item.getStatus(), item.getTotal()))
				.toList();

		List<TopProductStat> topProducts = customerOrderRepository
				.findTopProductsInPeriod(startTime, endTime, statusFilter, PageRequest.of(0, 5)).stream()
				.map(item -> new TopProductStat(item.getProductName(), item.getUnit(), item.getTotalQuantity(), item.getTotalRevenue()))
				.toList();

		long cancelledOrders = statusStats.stream()
				.filter(item -> item.status() == OrderStatus.CANCELLED)
				.mapToLong(item -> item.total() == null ? 0L : item.total())
				.sum();
		BigDecimal cancellationRate = totalOrders == 0
				? BigDecimal.ZERO
				: BigDecimal.valueOf(cancelledOrders)
						.multiply(BigDecimal.valueOf(100))
						.divide(BigDecimal.valueOf(totalOrders), 1, RoundingMode.HALF_UP);

		boolean monthlyTrend = ChronoUnit.DAYS.between(fromDate, toDate) + 1 > 31;
		List<SalesTrendStat> salesTrend = buildSalesTrend(
				fromDate,
				toDate,
				customerOrderRepository.findDailySalesInPeriod(startTime, endTime, statusFilter),
				monthlyTrend);

		return new ReportData(totalOrders, totalRevenue, averageOrderValue, guestOrders, memberOrders,
				cancelledOrders, cancellationRate, statusStats, topProducts, salesTrend, monthlyTrend);
	}

	private List<SalesTrendStat> buildSalesTrend(LocalDate fromDate, LocalDate toDate,
			List<DailySalesProjection> dailySales, boolean monthlyTrend) {
		Map<LocalDate, TrendPoint> points = new TreeMap<>();
		for (DailySalesProjection daily : dailySales) {
			LocalDate periodStart = monthlyTrend
					? YearMonth.from(daily.getOrderDate()).atDay(1)
					: daily.getOrderDate();
			TrendPoint point = new TrendPoint(
					daily.getOrderCount() == null ? 0L : daily.getOrderCount(),
					daily.getRevenue() == null ? BigDecimal.ZERO : daily.getRevenue());
			points.merge(periodStart, point, TrendPoint::plus);
		}

		LocalDate firstPeriod = monthlyTrend ? YearMonth.from(fromDate).atDay(1) : fromDate;
		LocalDate lastPeriod = monthlyTrend ? YearMonth.from(toDate).atDay(1) : toDate;
		for (LocalDate period = firstPeriod; !period.isAfter(lastPeriod);
				period = monthlyTrend ? period.plusMonths(1) : period.plusDays(1)) {
			points.putIfAbsent(period, new TrendPoint(0L, BigDecimal.ZERO));
		}

		BigDecimal maximumRevenue = points.values().stream()
				.map(TrendPoint::revenue)
				.max(BigDecimal::compareTo)
				.orElse(BigDecimal.ZERO);
		List<SalesTrendStat> trend = new ArrayList<>(points.size());
		for (Map.Entry<LocalDate, TrendPoint> entry : points.entrySet()) {
			TrendPoint point = entry.getValue();
			int revenueHeightPercent = maximumRevenue.signum() == 0 || point.revenue().signum() == 0
					? 0
					: point.revenue()
							.multiply(BigDecimal.valueOf(100))
							.divide(maximumRevenue, 0, RoundingMode.HALF_UP)
							.intValue();
			trend.add(new SalesTrendStat(entry.getKey(), point.orderCount(), point.revenue(), revenueHeightPercent));
		}
		return List.copyOf(trend);
	}

	public record ReportData(
			long totalOrders,
			BigDecimal totalRevenue,
			BigDecimal averageOrderValue,
			long guestOrders,
			long memberOrders,
			long cancelledOrders,
			BigDecimal cancellationRate,
			List<StatusStat> statusStats,
			List<TopProductStat> topProducts,
			List<SalesTrendStat> salesTrend,
			boolean monthlyTrend) {
	}

	public record StatusStat(OrderStatus status, Long total) {
	}

	public record TopProductStat(String productName, String unit, Long totalQuantity, BigDecimal totalRevenue) {
	}

	public record SalesTrendStat(LocalDate periodStart, long orderCount, BigDecimal revenue, int revenueHeightPercent) {
	}

	private record TrendPoint(long orderCount, BigDecimal revenue) {
		private TrendPoint plus(TrendPoint other) {
			return new TrendPoint(orderCount + other.orderCount, revenue.add(other.revenue));
		}
	}
}