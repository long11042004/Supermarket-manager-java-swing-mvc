package com.example.productmanager.view;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import com.example.productmanager.multilanguage.MessageResolver;
import com.example.productmanager.security.SecurityUserPrincipal;
import com.example.productmanager.service.AppSpecialNoticeService;
import com.example.productmanager.service.AppSpecialNoticeService.Notice;

import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;

@ControllerAdvice
@AllArgsConstructor
public class GlobalViewAttributes {

	private final AppSpecialNoticeService appSpecialNoticeService;
	private final MessageResolver messageResolver;

	@ModelAttribute
	public void addGlobalNotice(Model model, HttpServletRequest request) {
		String path = request.getRequestURI();
		if ("/login".equals(path) || "/register".equals(path)) {
			return;
		}

		Notice notice = null;
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication != null && authentication.getPrincipal() instanceof SecurityUserPrincipal principal) {
			notice = appSpecialNoticeService.getUserNotice(principal.getId());
		}

		if (notice == null) {
			notice = appSpecialNoticeService.getCurrentNotice();
		}

		if (notice == null) {
			return;
		}

		String resolvedMessage = notice.message();
		if ((resolvedMessage == null || resolvedMessage.isBlank()) && notice.messageKey() != null && !notice.messageKey().isBlank()) {
			resolvedMessage = messageResolver.msg(notice.messageKey(), notice.args());
		}

		model.addAttribute("specialNoticeMessage", resolvedMessage);
		model.addAttribute("specialNoticeLevel", notice.level());
		model.addAttribute("specialNoticeUpdatedAt", notice.updatedAt());
	}
}
