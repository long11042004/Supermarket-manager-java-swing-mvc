package com.example.productmanager.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.productmanager.model.User;
import com.example.productmanager.service.UserService;

import jakarta.servlet.http.HttpSession;

@Controller
public class AuthController {

	private final UserService userService;

	public AuthController(UserService userService) {
		this.userService = userService;
	}

	@GetMapping("/login")
	public String loginPage(Model model) {
		model.addAttribute("loginUser", new User());
		return "login";
	}

	@PostMapping("/login")
	public String login(@ModelAttribute("loginUser") User loginUser,
			HttpSession session,
			RedirectAttributes redirectAttributes) {
		try {
			User authenticatedUser = userService.login(loginUser.getUsername(), loginUser.getPassword());
			session.setAttribute("loggedInUser", authenticatedUser);
			return "redirect:/dashboard";
		} catch (Exception ex) {
			redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
			return "redirect:/login";
		}
	}

	@PostMapping("/logout")
	public String logout(HttpSession session) {
		session.invalidate();
		return "redirect:/login";
	}
}
