package com.example.productmanager.controller;

import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import com.example.productmanager.service.AppSpecialNoticeService;
import com.example.productmanager.service.AppSpecialNoticeService.Notice;

import lombok.AllArgsConstructor;

@ControllerAdvice
@AllArgsConstructor
public class GlobalViewAttributes {

	private final AppSpecialNoticeService appSpecialNoticeService;

	@ModelAttribute
	public void addGlobalNotice(Model model) {
		Notice notice = appSpecialNoticeService.getCurrentNotice();
		if (notice == null) {
			return;
		}
		model.addAttribute("specialNoticeMessage", notice.message());
		model.addAttribute("specialNoticeLevel", notice.level());
		model.addAttribute("specialNoticeUpdatedAt", notice.updatedAt());
	}
}
