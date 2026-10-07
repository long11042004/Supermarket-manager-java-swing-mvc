package com.example.productmanager.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import lombok.AllArgsConstructor;

@Configuration
@EnableMethodSecurity
@AllArgsConstructor
public class SecurityConfig {

	private final JwtAuthenticationFilter jwtAuthenticationFilter;
	private final CustomUserDetailsService customUserDetailsService;

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		http
				.authorizeHttpRequests(auth -> auth
						.requestMatchers(
								"/",
								"/login",
								"/logout",
								"/register",
								"/error",
								"/favicon.ico",
								"/css/**",
								"/js/**",
								"/images/**",
								"/webjars/**",
								"/uploads/avatars/**").permitAll()
						.requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
						.requestMatchers(HttpMethod.GET, "/products", "/products/").permitAll()
						.requestMatchers(HttpMethod.HEAD, "/products", "/products/").permitAll()
						.requestMatchers(HttpMethod.POST,
								"/products/*/cart",
								"/products/cart/**",
								"/orders/checkout").permitAll()
						.requestMatchers(HttpMethod.GET, "/products/*/edit").hasAnyRole("MANAGER", "ADMIN")
						.requestMatchers(HttpMethod.HEAD, "/products/*/edit").hasAnyRole("MANAGER", "ADMIN")
						.requestMatchers(HttpMethod.POST,
								"/products",
								"/products/*/update",
								"/products/*/delete").hasAnyRole("MANAGER", "ADMIN")
						.requestMatchers("/products/**").denyAll()
						.requestMatchers("/dashboard", "/customer-dashboard", "/profile", "/profile/**")
						.hasAnyRole("ADMIN", "MANAGER", "STAFF", "CUSTOMER")
						.requestMatchers("/users", "/users/**", "/reports", "/reports/**")
						.hasAnyRole("MANAGER", "ADMIN")
						.requestMatchers(HttpMethod.POST, "/api/products", "/api/products/**")
						.hasAnyRole("MANAGER", "ADMIN")
						.requestMatchers(HttpMethod.PUT, "/api/products", "/api/products/**")
						.hasAnyRole("MANAGER", "ADMIN")
						.requestMatchers(HttpMethod.DELETE, "/api/products", "/api/products/**")
						.hasAnyRole("MANAGER", "ADMIN")
						.requestMatchers(HttpMethod.GET, "/api/products", "/api/products/**")
						.hasAnyRole("STAFF", "MANAGER", "ADMIN")
						.requestMatchers(HttpMethod.HEAD, "/api/products", "/api/products/**")
						.hasAnyRole("STAFF", "MANAGER", "ADMIN")
						.requestMatchers("/api/reports", "/api/reports/**").hasAnyRole("MANAGER", "ADMIN")
						.requestMatchers(HttpMethod.POST, "/api/orders/**").hasAnyRole("STAFF", "MANAGER", "ADMIN")
						.requestMatchers("/api/**").denyAll()
						.requestMatchers("/orders", "/orders/**").hasRole("CUSTOMER")
						.anyRequest().denyAll())
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
				.authenticationProvider(authenticationProvider())
				.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
				.logout(logout -> logout
						.logoutUrl("/logout")
						.clearAuthentication(true)
						.invalidateHttpSession(true)
						.deleteCookies(JwtService.ACCESS_TOKEN_COOKIE)
						.logoutSuccessUrl("/login"));

		return http.build();
	}

	@Bean
	public AuthenticationProvider authenticationProvider() {
		DaoAuthenticationProvider provider = new DaoAuthenticationProvider(customUserDetailsService);
		provider.setPasswordEncoder(passwordEncoder());
		return provider;
	}

	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
		return configuration.getAuthenticationManager();
	}
}
