package com.example.productmanager.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.example.productmanager.model.Product;
import com.example.productmanager.repository.CustomerOrderRepository;

@Service
@ConditionalOnProperty(name = "app.scheduling.enabled", havingValue = "true", matchIfMissing = true)
public class SchedulerService {

	private static final Logger log = LoggerFactory.getLogger(SchedulerService.class);

	private final ProductService productService;
	private final CustomerOrderRepository customerOrderRepository;
	private final EmailService emailService;
	private final AppSpecialNoticeService appSpecialNoticeService;
	private final int lowStockThreshold;
	private final int expiringDaysAhead;
	private final String summaryRecipient;
	private final String specialNoticeMessage;

	public SchedulerService(ProductService productService,
			CustomerOrderRepository customerOrderRepository,
			EmailService emailService,
			AppSpecialNoticeService appSpecialNoticeService,
			@Value("${app.scheduling.low-stock-threshold:10}") int lowStockThreshold,
			@Value("${app.scheduling.expiring-days-ahead:7}") int expiringDaysAhead,
			@Value("${app.scheduling.daily-summary.recipient:}") String summaryRecipient,
			@Value("${app.scheduling.special-notice.promo-message:Khuyen mai dac biet hom nay: Giam 10% cho hoa don tu 300.000 VND}") String specialNoticeMessage) {
		this.productService = productService;
		this.customerOrderRepository = customerOrderRepository;
		this.emailService = emailService;
		this.appSpecialNoticeService = appSpecialNoticeService;
		this.lowStockThreshold = lowStockThreshold;
		this.expiringDaysAhead = expiringDaysAhead;
		this.summaryRecipient = summaryRecipient;
		this.specialNoticeMessage = specialNoticeMessage;
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

		if (!lowStockProducts.isEmpty() || !expiringSoonProducts.isEmpty()) {
			String warning = new StringBuilder("Canh bao he thong: ")
					.append(lowStockProducts.size())
					.append(" san pham sap het, ")
					.append(expiringSoonProducts.size())
					.append(" san pham sap het han.")
					.toString();
			appSpecialNoticeService.publishWarning(warning);
		} else {
			appSpecialNoticeService.publishSuccess("Kho hang dang on dinh. Chua co canh bao ton kho hoac han su dung.");
		}
	}

	@Scheduled(cron = "${app.scheduling.special-notice.cron:0 0 9 * * *}", zone = "${app.scheduling.zone:Asia/Ho_Chi_Minh}")
	public void publishSpecialNotice() {
		appSpecialNoticeService.publishInfo(specialNoticeMessage);
		log.info("[SCHEDULED][SPECIAL_NOTICE] published in-app special notice");
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
}
