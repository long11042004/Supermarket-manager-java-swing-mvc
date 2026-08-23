package com.example.productmanager.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.productmanager.dto.user.UserLoginDTO;
import com.example.productmanager.dto.user.UserRegistrationDTO;
import com.example.productmanager.i18n.MessageResolver;
import com.example.productmanager.model.User;
import com.example.productmanager.service.UserService;

import jakarta.servlet.http.HttpSession;
import lombok.AllArgsConstructor;

@Controller
@AllArgsConstructor
public class AuthController {

	private final UserService userService;
	private final MessageResolver messageResolver;

	@GetMapping("/login")
	public String loginPage(Model model) {
		model.addAttribute("loginUser", new UserLoginDTO());
		model.addAttribute("registerUser", new UserRegistrationDTO());
		return "login";
	}

	@PostMapping("/login")
	public String login(@ModelAttribute("loginUser") UserLoginDTO loginUser,
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

	@PostMapping("/register")
	public String register(@ModelAttribute("registerUser") UserRegistrationDTO registerUser,
			RedirectAttributes redirectAttributes) {
		try {
			userService.registerCustomer(
					registerUser.getUsername(),
					registerUser.getPassword(),
					registerUser.getEmail(),
					registerUser.getFullName());
			redirectAttributes.addFlashAttribute("successMessage", messageResolver.msg("msg.auth.registerSuccess"));
		} catch (Exception ex) {
			redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
		}
		return "redirect:/login";
	}

	@PostMapping("/logout")
	public String logout(HttpSession session) {
		User currentUser = (User) session.getAttribute("loggedInUser");
		if (currentUser != null) {
			userService.logLogout(currentUser.getId());
		}
		session.invalidate();
		return "redirect:/login";
	}
}
