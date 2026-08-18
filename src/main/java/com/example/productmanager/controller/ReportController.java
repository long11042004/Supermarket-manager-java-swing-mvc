package com.example.productmanager.controller;

import java.time.LocalDate;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.example.productmanager.model.OrderStatus;
import com.example.productmanager.model.RoleName;
import com.example.productmanager.model.User;
import com.example.productmanager.service.ReportService;

import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/reports")
public class ReportController {

	private final ReportService reportService;

	public ReportController(ReportService reportService) {
		this.reportService = reportService;
	}

	@GetMapping
	public String reports(@RequestParam(value = "fromDate", required = false) LocalDate fromDate,
			@RequestParam(value = "toDate", required = false) LocalDate toDate,
			@RequestParam(value = "status", required = false) String status,
			HttpSession session,
			Model model) {
		if (!hasPermission(session, RoleName.ADMIN, RoleName.MANAGER)) {
			return "redirect:/login";
		}

		LocalDate effectiveTo = toDate == null ? LocalDate.now() : toDate;
		LocalDate effectiveFrom = fromDate == null ? effectiveTo.minusDays(29) : fromDate;
		if (effectiveFrom.isAfter(effectiveTo)) {
			LocalDate temp = effectiveFrom;
			effectiveFrom = effectiveTo;
			effectiveTo = temp;
		}

		OrderStatus statusFilter = parseStatus(status);
		ReportService.ReportData reportData = reportService.generateReport(effectiveFrom, effectiveTo, statusFilter);

		model.addAttribute("fromDate", effectiveFrom);
		model.addAttribute("toDate", effectiveTo);
		model.addAttribute("status", statusFilter == null ? "" : statusFilter.name());
		model.addAttribute("statuses", OrderStatus.values());
		model.addAttribute("report", reportData);
		return "reports";
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

	private boolean hasPermission(HttpSession session, RoleName... allowedRoles) {
		User currentUser = (User) session.getAttribute("loggedInUser");
		if (currentUser == null) {
			return false;
		}
		Set<RoleName> roleNames = currentUser.getRoles() == null ? Set.of() : currentUser.getRoles().stream()
				.map(role -> role.getName())
				.collect(Collectors.toSet());
		for (RoleName role : allowedRoles) {
			if (roleNames.contains(role)) {
				return true;
			}
		}
		return false;
	}
}