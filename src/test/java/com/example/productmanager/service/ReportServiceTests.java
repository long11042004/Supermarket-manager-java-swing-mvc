package com.example.productmanager.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.example.productmanager.entity.OrderStatus;
import com.example.productmanager.repository.CustomerOrderRepository;

class ReportServiceTests {

    @Test
    void shouldReturnZeroValuesWhenNoOrdersExistInPeriod() {
        CustomerOrderRepository customerOrderRepository = mock(CustomerOrderRepository.class);
        ReportService reportService = new ReportService(customerOrderRepository);

        when(customerOrderRepository.countOrdersInPeriod(any(), any(), isNull())).thenReturn(0L);
        when(customerOrderRepository.sumRevenueInPeriod(any(), any(), isNull())).thenReturn(null);
        when(customerOrderRepository.countGuestOrdersInPeriod(any(), any(), isNull())).thenReturn(0L);
        when(customerOrderRepository.countOrdersByStatusInPeriod(any(), any(), isNull())).thenReturn(List.of());
        when(customerOrderRepository.findTopProductsInPeriod(any(), any(), isNull(), any())).thenReturn(List.of());
        when(customerOrderRepository.findDailySalesInPeriod(any(), any(), isNull())).thenReturn(List.of());

        ReportService.ReportData reportData = reportService.generateReport(LocalDate.now().minusDays(7), LocalDate.now(), null);

        assertThat(reportData.totalOrders()).isZero();
        assertThat(reportData.totalRevenue()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(reportData.averageOrderValue()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(reportData.guestOrders()).isZero();
        assertThat(reportData.memberOrders()).isZero();
        assertThat(reportData.cancelledOrders()).isZero();
        assertThat(reportData.cancellationRate()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(reportData.statusStats()).isEmpty();
        assertThat(reportData.topProducts()).isEmpty();
        assertThat(reportData.salesTrend()).hasSize(8).allSatisfy(item -> {
            assertThat(item.orderCount()).isZero();
            assertThat(item.revenue()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(item.revenueHeightPercent()).isZero();
        });
        assertThat(reportData.monthlyTrend()).isFalse();
    }

    @Test
    void shouldAggregateLongPeriodsByMonthAndCalculateCancellationRate() {
        CustomerOrderRepository customerOrderRepository = mock(CustomerOrderRepository.class);
        ReportService reportService = new ReportService(customerOrderRepository);

        CustomerOrderRepository.OrderStatusCountProjection confirmed = mock(CustomerOrderRepository.OrderStatusCountProjection.class);
        when(confirmed.getStatus()).thenReturn(OrderStatus.CONFIRMED);
        when(confirmed.getTotal()).thenReturn(3L);
        CustomerOrderRepository.OrderStatusCountProjection cancelled = mock(CustomerOrderRepository.OrderStatusCountProjection.class);
        when(cancelled.getStatus()).thenReturn(OrderStatus.CANCELLED);
        when(cancelled.getTotal()).thenReturn(1L);

        CustomerOrderRepository.DailySalesProjection januaryFirst = mock(CustomerOrderRepository.DailySalesProjection.class);
        when(januaryFirst.getOrderDate()).thenReturn(LocalDate.of(2026, 1, 3));
        when(januaryFirst.getOrderCount()).thenReturn(1L);
        when(januaryFirst.getRevenue()).thenReturn(new BigDecimal("100"));
        CustomerOrderRepository.DailySalesProjection januaryLater = mock(CustomerOrderRepository.DailySalesProjection.class);
        when(januaryLater.getOrderDate()).thenReturn(LocalDate.of(2026, 1, 15));
        when(januaryLater.getOrderCount()).thenReturn(2L);
        when(januaryLater.getRevenue()).thenReturn(new BigDecimal("300"));
        CustomerOrderRepository.DailySalesProjection february = mock(CustomerOrderRepository.DailySalesProjection.class);
        when(february.getOrderDate()).thenReturn(LocalDate.of(2026, 2, 1));
        when(february.getOrderCount()).thenReturn(1L);
        when(february.getRevenue()).thenReturn(new BigDecimal("200"));

        when(customerOrderRepository.countOrdersInPeriod(any(), any(), isNull())).thenReturn(4L);
        when(customerOrderRepository.sumRevenueInPeriod(any(), any(), isNull())).thenReturn(new BigDecimal("600"));
        when(customerOrderRepository.countGuestOrdersInPeriod(any(), any(), isNull())).thenReturn(1L);
        when(customerOrderRepository.countOrdersByStatusInPeriod(any(), any(), isNull())).thenReturn(List.of(confirmed, cancelled));
        when(customerOrderRepository.findTopProductsInPeriod(any(), any(), isNull(), any())).thenReturn(List.of());
        when(customerOrderRepository.findDailySalesInPeriod(any(), any(), isNull()))
                .thenReturn(List.of(januaryFirst, januaryLater, february));

        ReportService.ReportData reportData = reportService.generateReport(
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 2, 1), null);

        assertThat(reportData.cancelledOrders()).isEqualTo(1);
        assertThat(reportData.cancellationRate()).isEqualByComparingTo("25.0");
        assertThat(reportData.monthlyTrend()).isTrue();
        assertThat(reportData.salesTrend()).hasSize(2);
        assertThat(reportData.salesTrend().get(0).periodStart()).isEqualTo(LocalDate.of(2026, 1, 1));
        assertThat(reportData.salesTrend().get(0).orderCount()).isEqualTo(3);
        assertThat(reportData.salesTrend().get(0).revenue()).isEqualByComparingTo("400");
        assertThat(reportData.salesTrend().get(0).revenueHeightPercent()).isEqualTo(100);
        assertThat(reportData.salesTrend().get(1).periodStart()).isEqualTo(LocalDate.of(2026, 2, 1));
        assertThat(reportData.salesTrend().get(1).revenueHeightPercent()).isEqualTo(50);
    }
}
