package com.example.productmanager.security;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;

import com.example.productmanager.entity.User;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Service
public class JwtService {

	public static final String ACCESS_TOKEN_COOKIE = "ACCESS_TOKEN";

	private final SecretKey secretKey;
	private final long expirationMs;

	public JwtService(@Value("${app.jwt.secret}") String secret,
			@Value("${app.jwt.expiration-ms}") long expirationMs) {
		this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
		this.expirationMs = expirationMs;
	}

	public String generateToken(User user) {
		Date now = new Date();
		Date expiration = new Date(now.getTime() + expirationMs);
		List<String> roles = user.getRoles() == null
				? List.of()
				: user.getRoles().stream().map(role -> role.getName().name()).toList();

		return Jwts.builder()
				.subject(user.getUsername())
				.claim("uid", user.getId())
				.claim("roles", roles)
				.issuedAt(now)
				.expiration(expiration)
				.signWith(secretKey)
				.compact();
	}

	public String extractUsername(String token) {
		return parseClaims(token).getSubject();
	}

	public boolean isTokenValid(String token) {
		try {
			parseClaims(token);
			return true;
		} catch (Exception ex) {
			return false;
		}
	}

	public ResponseCookie createAccessTokenCookie(String token) {
		return ResponseCookie.from(ACCESS_TOKEN_COOKIE, token)
				.httpOnly(true)
				.secure(false)
				.path("/")
				.maxAge(expirationMs / 1000)
				.sameSite("Lax")
				.build();
	}

	public ResponseCookie clearAccessTokenCookie() {
		return ResponseCookie.from(ACCESS_TOKEN_COOKIE, "")
				.httpOnly(true)
				.secure(false)
				.path("/")
				.maxAge(0)
				.sameSite("Lax")
				.build();
	}

	private Claims parseClaims(String token) {
		return Jwts.parser()
				.verifyWith(secretKey)
				.build()
				.parseSignedClaims(token)
				.getPayload();
	}
}
