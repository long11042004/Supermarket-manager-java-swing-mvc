package com.example.productmanager.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.productmanager.model.User;
import com.example.productmanager.security.JwtService;
import com.example.productmanager.service.UserService;

import lombok.AllArgsConstructor;

@RestController
@RequestMapping("/api/auth")
@AllArgsConstructor
public class ApiAuthController {

	private final UserService userService;
	private final JwtService jwtService;

	@PostMapping("/login")
	public JwtLoginResponse login(@RequestBody JwtLoginRequest request) {
		User user = userService.login(request.username(), request.password());
		String token = jwtService.generateToken(user);
		return new JwtLoginResponse(token, user.getUsername());
	}

	public record JwtLoginRequest(String username, String password) {
	}

	public record JwtLoginResponse(String token, String username) {
	}
}
