package com.example.productmanager.controller;

import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.productmanager.dto.user.UserLoginDTO;
import com.example.productmanager.dto.user.UserRegistrationDTO;
import com.example.productmanager.entity.User;
import com.example.productmanager.multilanguage.MessageResolver;
import com.example.productmanager.security.JwtService;
import com.example.productmanager.security.SecurityUserPrincipal;
import com.example.productmanager.service.UserService;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.AllArgsConstructor;

@Controller
@AllArgsConstructor
public class AuthController {

	private final UserService userService;
	private final MessageResolver messageResolver;
	private final JwtService jwtService;

	@GetMapping("/login")
	public String loginPage(Model model) {
		model.addAttribute("loginUser", new UserLoginDTO());
		model.addAttribute("registerUser", new UserRegistrationDTO());
		return "login";
	}

	@PostMapping("/login")
	public String login(@ModelAttribute("loginUser") UserLoginDTO loginUser,
			HttpSession session,
			HttpServletResponse response,
			RedirectAttributes redirectAttributes) {
		try {
			SecurityContextHolder.clearContext();
			User authenticatedUser = userService.login(loginUser.getUsername(), loginUser.getPassword());
			String token = jwtService.generateToken(authenticatedUser);
			response.addHeader(HttpHeaders.SET_COOKIE, jwtService.createAccessTokenCookie(token).toString());
			session.setAttribute("loggedInUser", authenticatedUser);
			session.removeAttribute("guestCheckout");
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
	public String logout(HttpSession session, HttpServletResponse response) {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication != null && authentication.getPrincipal() instanceof SecurityUserPrincipal principal) {
			userService.logLogout(principal.getId());
		}
		SecurityContextHolder.clearContext();
		response.addHeader(HttpHeaders.SET_COOKIE, jwtService.clearAccessTokenCookie().toString());
		if (session != null) {
			session.removeAttribute("loggedInUser");
			session.removeAttribute("guestCheckout");
			session.invalidate();
		}
		return "redirect:/login";
	}
}
