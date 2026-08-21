package com.example.productmanager.security;

import java.util.Collection;
import java.util.List;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.example.productmanager.model.User;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public class SecurityUserPrincipal implements UserDetails {

	private final Long id;
	private final String username;
	private final String password;
	private final boolean enabled;
	private final List<GrantedAuthority> authorities;

	public static SecurityUserPrincipal from(User user) {
		List<GrantedAuthority> authorities = user.getRoles() == null
				? List.of()
				: user.getRoles().stream()
						.map(role -> new SimpleGrantedAuthority("ROLE_" + role.getName().name()))
						.map(authority -> (GrantedAuthority) authority)
						.toList();

		return new SecurityUserPrincipal(
				user.getId(),
				user.getUsername(),
				user.getPassword(),
				user.isEnabled(),
				authorities);
	}

	public Long getId() {
		return id;
	}

	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		return authorities;
	}

	@Override
	public String getPassword() {
		return password;
	}

	@Override
	public String getUsername() {
		return username;
	}

	@Override
	public boolean isAccountNonExpired() {
		return true;
	}

	@Override
	public boolean isAccountNonLocked() {
		return true;
	}

	@Override
	public boolean isCredentialsNonExpired() {
		return true;
	}

	@Override
	public boolean isEnabled() {
		return enabled;
	}
}
