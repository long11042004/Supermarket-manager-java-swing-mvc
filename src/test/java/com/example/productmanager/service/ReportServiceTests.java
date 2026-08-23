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

import com.example.productmanager.repository.CustomerOrderRepository;

class ReportServiceTests {

    @Test
    void shouldReturnZeroValuesWhenNoOrdersExistInPeriod() {
        CustomerOrderRepository customerOrderRepository = mock(CustomerOrderRepository.class);
        ReportService reportService = new ReportService(customerOrderRepository);

        when(customerOrderRepository.countOrdersInPeriod(any(), any(), isNull())).thenReturn(0L);
        when(customerOrderRepository.sumRevenueInPeriod(any(), any(), isNull())).thenReturn(null);
        when(customerOrderRepository.countGuestOrdersInPeriod(any(), any(), isNull())).thenReturn(0L);
        when(customerOrderRepository.countOrdersByStatusInPeriod(any(), any())).thenReturn(List.of());
        when(customerOrderRepository.findTopProductsInPeriod(any(), any(), isNull(), any())).thenReturn(List.of());

        ReportService.ReportData reportData = reportService.generateReport(LocalDate.now().minusDays(7), LocalDate.now(), null);

        assertThat(reportData.totalOrders()).isZero();
        assertThat(reportData.totalRevenue()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(reportData.averageOrderValue()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(reportData.guestOrders()).isZero();
        assertThat(reportData.memberOrders()).isZero();
        assertThat(reportData.statusStats()).isEmpty();
        assertThat(reportData.topProducts()).isEmpty();
    }
}
