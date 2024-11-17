package com.security.filters;

import com.security.service.AccountService;
import com.security.utils.JwtUtil;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.stereotype.Component;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Component
public class JwtAuthenticationFilter extends UsernamePasswordAuthenticationFilter {
	private AuthenticationManager authenticationManager;
	public AccountService accountService;

	@Autowired
	private BCryptPasswordEncoder passwordEncoder;

	@Autowired
	private JwtUtil jwtUtil = new JwtUtil();

	public JwtAuthenticationFilter(AuthenticationManager authenticationManager, AccountService accountService) {
		super(authenticationManager);
		this.authenticationManager = authenticationManager;
		this.accountService = accountService;
		setFilterProcessesUrl("/login");
	}

	@Override
	public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response)
			throws AuthenticationException {
		try {
			Map<String, String> jsonMap = new ObjectMapper().readValue(request.getInputStream(), Map.class);
			String username = jsonMap.get("username");
			String password = jsonMap.get("password");

			User user = (User) accountService.loadUserByUsername(username);
			System.out.println("user.getAuthorities(): " + user.getAuthorities());

			if (user != null) {

				if (passwordEncoder.matches(password, user.getPassword())) {
					UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
							username, password, user.getAuthorities());
					setDetails(request, authenticationToken);
					return authenticationManager.authenticate(authenticationToken);
				} else {
					System.out.println("Password does not match");
				}
			} else {
				System.out.println("User not found: " + username);
			}
		} catch (IOException e) {
			throw new AuthenticationException("Could not read request body") {
			};
		}

		return null;
	}

	@Override
	protected void successfulAuthentication(HttpServletRequest request, HttpServletResponse response, FilterChain chain,
			Authentication authResult) throws IOException, ServletException {
		System.out.println("successfulAuthentication called...");

		User user = (User) authResult.getPrincipal();

		String refreshToken = jwtUtil.generateRefreshToken(user);
		String accessToken = jwtUtil.generateAccessToken(user);

		Map<String, String> idToken = new HashMap<>();
		idToken.put("refresh-token", refreshToken);
		idToken.put("access-token", accessToken);

		response.setContentType("application/json");
		new ObjectMapper().writeValue(response.getOutputStream(), idToken);
	}

	@Override
	protected void unsuccessfulAuthentication(HttpServletRequest request, HttpServletResponse response,
			AuthenticationException failed) throws IOException, ServletException {
		response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
		response.setContentType("application/json");
		Map<String, String> error = new HashMap<>();
		error.put("error", failed.getMessage());
		new ObjectMapper().writeValue(response.getOutputStream(), error);
	}

}