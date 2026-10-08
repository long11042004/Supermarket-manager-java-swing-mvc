package com.example.productmanager.controller;

import java.time.LocalDateTime;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.productmanager.controller.support.SessionController;
import com.example.productmanager.dto.promotion.PromotionFormDTO;
import com.example.productmanager.entity.DiscountType;
import com.example.productmanager.entity.PromotionKind;
import com.example.productmanager.entity.RoleName;
import com.example.productmanager.multilanguage.MessageResolver;
import com.example.productmanager.service.PromotionService;

import jakarta.servlet.http.HttpSession;
import lombok.AllArgsConstructor;

@Controller
@RequestMapping("/promotions")
@AllArgsConstructor
public class PromotionController extends SessionController {

	private final PromotionService promotionService;
	private final MessageResolver messageResolver;

	@GetMapping
	public String promotions(Model model, HttpSession session) {
		if (!hasPermission(session, RoleName.ADMIN, RoleName.MANAGER)) {
			return "redirect:/login";
		}
		if (!model.containsAttribute("promotionForm")) {
			PromotionFormDTO form = new PromotionFormDTO();
			form.setKind(PromotionKind.COUPON);
			form.setDiscountType(DiscountType.PERCENTAGE);
			form.setMinimumOrderAmount(java.math.BigDecimal.ZERO);
			form.setStartsAt(LocalDateTime.now().withSecond(0).withNano(0));
			form.setEndsAt(LocalDateTime.now().plusDays(7).withSecond(0).withNano(0));
			model.addAttribute("promotionForm", form);
		}
		model.addAttribute("promotions", promotionService.getAllPromotions());
		model.addAttribute("promotionKinds", PromotionKind.values());
		model.addAttribute("discountTypes", DiscountType.values());
		return "promotions";
	}

	@PostMapping
	public String createPromotion(@ModelAttribute("promotionForm") PromotionFormDTO form,
			HttpSession session,
			RedirectAttributes redirectAttributes) {
		if (!hasPermission(session, RoleName.ADMIN, RoleName.MANAGER)) {
			return "redirect:/login";
		}
		try {
			promotionService.createPromotion(form);
			redirectAttributes.addFlashAttribute("successMessage", messageResolver.msg("msg.promotion.created"));
		} catch (IllegalArgumentException ex) {
			redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
			redirectAttributes.addFlashAttribute("promotionForm", form);
		}
		return "redirect:/promotions";
	}

	@PostMapping("/{promotionId}/active")
	public String updateActive(@PathVariable Long promotionId,
			@RequestParam boolean active,
			HttpSession session,
			RedirectAttributes redirectAttributes) {
		if (!hasPermission(session, RoleName.ADMIN, RoleName.MANAGER)) {
			return "redirect:/login";
		}
		try {
			promotionService.updateActive(promotionId, active);
			redirectAttributes.addFlashAttribute("successMessage", messageResolver.msg("msg.promotion.updated"));
		} catch (IllegalArgumentException ex) {
			redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
		}
		return "redirect:/promotions";
	}
}
