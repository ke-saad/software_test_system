package com.security.filters;

import com.auth0.jwt.interfaces.DecodedJWT;
import com.security.utils.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

public class JWTAuthorizationFilter extends OncePerRequestFilter {

	private final JwtUtil jwtUtil;
	private final UserDetailsService userDetailsService;

	@Autowired
	public JWTAuthorizationFilter(JwtUtil jwtUtil, UserDetailsService userDetailsService) {
		this.jwtUtil = jwtUtil;
		this.userDetailsService = userDetailsService;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		
		System.out.println("Request URI: " + request.getRequestURI());

		String authToken = request.getHeader("Authorization");

		if (authToken != null && authToken.startsWith("Bearer ")) {
			try {
				String jwt = authToken.substring(7);
				DecodedJWT decodedJwt = jwtUtil.verifySignature(jwt);

				if (decodedJwt != null) {
					String userName = jwtUtil.getUsername(decodedJwt);
					UserDetails userDetails = userDetailsService.loadUserByUsername(userName);

					Collection<GrantedAuthority> authorities = new ArrayList<>();
					if (userDetails != null) {
						authorities = new ArrayList<>(userDetails.getAuthorities());
					}

					UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
							userDetails, null, authorities);
					SecurityContextHolder.getContext().setAuthentication(authenticationToken);
				}
			} catch (Exception e) {
				response.setHeader("error message", e.getMessage());
				response.sendError(HttpServletResponse.SC_FORBIDDEN, "Invalid JWT token");
				return;
			}
		}

		filterChain.doFilter(request, response);
	}
	
	@Override
	protected boolean shouldNotFilter(HttpServletRequest request) {
	    return request.getRequestURI().equals("/login");
	}
}