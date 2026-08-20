package com.example.productmanager.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.example.productmanager.model.Product;
import com.example.productmanager.model.User;
import com.example.productmanager.repository.CustomerOrderRepository;

@Service
@ConditionalOnProperty(name = "app.scheduling.enabled", havingValue = "true", matchIfMissing = true)
public class SchedulerService {

	private static final Logger log = LoggerFactory.getLogger(SchedulerService.class);

	private final ProductService productService;
	private final UserService userService;
	private final AppSpecialNoticeService appSpecialNoticeService;
	private final CustomerOrderRepository customerOrderRepository;
	private final EmailService emailService;
	private final int lowStockThreshold;
	private final int expiringDaysAhead;
	private final String summaryRecipient;
	private final String profileReminderTemplate;
	private final int profileReminderMaxMissingFields;

	public SchedulerService(ProductService productService,
			UserService userService,
			AppSpecialNoticeService appSpecialNoticeService,
			CustomerOrderRepository customerOrderRepository,
			EmailService emailService,
			@Value("${app.scheduling.low-stock-threshold:10}") int lowStockThreshold,
			@Value("${app.scheduling.expiring-days-ahead:7}") int expiringDaysAhead,
			@Value("${app.scheduling.daily-summary.recipient:}") String summaryRecipient,
			@Value("${app.scheduling.profile-reminder.template:Ban chua bo sung day du thong tin: %s. Vui long cap nhat trong trang ho so.}") String profileReminderTemplate,
			@Value("${app.scheduling.profile-reminder.max-missing-fields:3}") int profileReminderMaxMissingFields) {
		this.productService = productService;
		this.userService = userService;
		this.appSpecialNoticeService = appSpecialNoticeService;
		this.customerOrderRepository = customerOrderRepository;
		this.emailService = emailService;
		this.lowStockThreshold = lowStockThreshold;
		this.expiringDaysAhead = expiringDaysAhead;
		this.summaryRecipient = summaryRecipient;
		this.profileReminderTemplate = profileReminderTemplate;
		this.profileReminderMaxMissingFields = profileReminderMaxMissingFields;
	}

	@Scheduled(
			fixedDelayString = "${app.scheduling.inventory-check.fixed-delay-ms:1800000}",
			initialDelayString = "${app.scheduling.inventory-check.initial-delay-ms:15000}")
	public void monitorInventoryHealth() {
		List<Product> lowStockProducts = productService.getLowStockProducts(lowStockThreshold);
		List<Product> expiringSoonProducts = productService.getExpiringSoonProducts(expiringDaysAhead);

		log.info("[SCHEDULED][INVENTORY] lowStock={}, expiringSoon={}, threshold={}, daysAhead={}",
				lowStockProducts.size(),
				expiringSoonProducts.size(),
				lowStockThreshold,
				expiringDaysAhead);
	}

	@Scheduled(cron = "${app.scheduling.daily-summary.cron:0 0 8 * * *}", zone = "${app.scheduling.zone:Asia/Ho_Chi_Minh}")
	public void sendDailySummaryReport() {
		if (summaryRecipient == null || summaryRecipient.isBlank()) {
			log.info("[SCHEDULED][DAILY_SUMMARY] skipped: app.scheduling.daily-summary.recipient is empty");
			return;
		}

		LocalDateTime endTime = LocalDateTime.now();
		LocalDateTime startTime = endTime.minusDays(1);

		long totalOrders = customerOrderRepository.countOrdersInPeriod(startTime, endTime, null);
		long guestOrders = customerOrderRepository.countGuestOrdersInPeriod(startTime, endTime, null);
		BigDecimal revenue = customerOrderRepository.sumRevenueInPeriod(startTime, endTime, null);
		List<Product> lowStockProducts = productService.getLowStockProducts(lowStockThreshold);
		List<Product> expiringSoonProducts = productService.getExpiringSoonProducts(expiringDaysAhead);

		String report = new StringBuilder("Bao cao he thong trong 24 gio qua\n")
				.append("- Tong don hang: ").append(totalOrders).append("\n")
				.append("- Don hang khach vang lai: ").append(guestOrders).append("\n")
				.append("- Doanh thu: ").append(revenue == null ? "0" : revenue.toPlainString()).append(" VND\n")
				.append("- San pham sap het: ").append(lowStockProducts.size()).append("\n")
				.append("- San pham sap het han: ").append(expiringSoonProducts.size()).append("\n")
				.toString();

		emailService.sendSimpleEmail(summaryRecipient, "Bao cao he thong hang ngay", report);
		log.info("[SCHEDULED][DAILY_SUMMARY] sent to {}", summaryRecipient);
	}

	@Scheduled(cron = "${app.scheduling.profile-reminder.cron:0 */10 * * * *}", zone = "${app.scheduling.zone:Asia/Ho_Chi_Minh}")
	public void publishIncompleteProfileReminders() {
		List<User> users = userService.getAllUsers();
		int warnedUsers = 0;

		for (User user : users) {
			if (user == null || user.getId() == null || !user.isEnabled()) {
				continue;
			}

			List<String> missingFields = collectMissingProfileFields(user);
			if (missingFields.isEmpty()) {
				appSpecialNoticeService.clearUserNotice(user.getId());
				continue;
			}

			String missingText = String.join(", ", missingFields);
			String reminder = String.format(profileReminderTemplate, missingText);
			appSpecialNoticeService.publishUserWarning(user.getId(), reminder);
			warnedUsers++;
		}

		log.info("[SCHEDULED][PROFILE_REMINDER] warnedUsers={}, totalUsers={}", warnedUsers, users.size());
	}

	private List<String> collectMissingProfileFields(User user) {
		List<String> missing = new ArrayList<>();
		if (isBlank(user.getPhoneNumber())) {
			missing.add("so dien thoai");
		}
		if (isBlank(user.getAddress())) {
			missing.add("dia chi");
		}
		if (isBlank(user.getEmail())) {
			missing.add("email");
		}
		if (isBlank(user.getFullName())) {
			missing.add("ho ten");
		}

		if (missing.size() <= profileReminderMaxMissingFields) {
			return missing;
		}
		return missing.subList(0, profileReminderMaxMissingFields);
	}

	private boolean isBlank(String value) {
		return value == null || value.isBlank();
	}
}
