package com.example.productmanager.controller;

import java.time.LocalDate;
import java.util.concurrent.CompletableFuture;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.productmanager.model.OrderStatus;
import com.example.productmanager.service.ReportService;

import lombok.AllArgsConstructor;

@RestController
@RequestMapping("/api/reports")
@AllArgsConstructor
public class ReportApiController {

	private final ReportService reportService;

	@GetMapping("/sync")
	public ReportService.ReportData reportSync(
			@RequestParam(value = "fromDate", required = false) LocalDate fromDate,
			@RequestParam(value = "toDate", required = false) LocalDate toDate,
			@RequestParam(value = "status", required = false) String status) {
		LocalDate effectiveTo = toDate == null ? LocalDate.now() : toDate;
		LocalDate effectiveFrom = fromDate == null ? effectiveTo.minusDays(29) : fromDate;
		if (effectiveFrom.isAfter(effectiveTo)) {
			LocalDate temp = effectiveFrom;
			effectiveFrom = effectiveTo;
			effectiveTo = temp;
		}

		return reportService.generateReport(effectiveFrom, effectiveTo, parseStatus(status));
	}

	@GetMapping("/async")
	public CompletableFuture<ReportService.ReportData> reportAsync(
			@RequestParam(value = "fromDate", required = false) LocalDate fromDate,
			@RequestParam(value = "toDate", required = false) LocalDate toDate,
			@RequestParam(value = "status", required = false) String status) {
		LocalDate effectiveTo = toDate == null ? LocalDate.now() : toDate;
		LocalDate effectiveFrom = fromDate == null ? effectiveTo.minusDays(29) : fromDate;
		if (effectiveFrom.isAfter(effectiveTo)) {
			LocalDate temp = effectiveFrom;
			effectiveFrom = effectiveTo;
			effectiveTo = temp;
		}

		return reportService.generateReportAsync(effectiveFrom, effectiveTo, parseStatus(status));
	}

	private OrderStatus parseStatus(String status) {
		if (status == null || status.isBlank()) {
			return null;
		}
		try {
			return OrderStatus.valueOf(status.trim().toUpperCase());
		} catch (IllegalArgumentException ex) {
			return null;
		}
	}
}