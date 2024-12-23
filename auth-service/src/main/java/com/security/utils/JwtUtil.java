package com.security.utils;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class JwtUtil {
	private final String SECRET_KEY = "saad1234";
	private Algorithm algorithm = Algorithm.HMAC256(SECRET_KEY.getBytes(StandardCharsets.UTF_8));

	public String generateAccessToken(UserDetails user) {
		Map<String, Object> claims = new HashMap<>();
		claims.put("roles", user.getAuthorities().stream().map(ga -> ga.getAuthority()).collect(Collectors.toList()));
		return createAccessToken(claims, user.getUsername());
	}

	public String generateRefreshToken(UserDetails user) {
		return createRefreshToken(user.getUsername());
	}

	public String createAccessToken(Map<String, Object> claims, String subject) {
		List<String> rolesList = (List<String>) claims.get("roles");
		String[] rolesArray = rolesList.toArray(new String[0]);

		return JWT.create().withSubject(subject).withIssuedAt(new Date(System.currentTimeMillis()))
				.withExpiresAt(new Date(System.currentTimeMillis() + 1000 * 60 * 2)).withArrayClaim("roles", rolesArray)
				.sign(algorithm);
	}

	public String createRefreshToken(String subject) {
		return JWT.create().withSubject(subject).withIssuedAt(new Date(System.currentTimeMillis()))
				.withExpiresAt(new Date(System.currentTimeMillis() + 1000 * 60 * 60 * 24 * 365)).sign(algorithm);
	}

	public String getUsername(DecodedJWT decodedJwt) {
		return decodedJwt.getSubject();
	}

	public DecodedJWT verifySignature(String token) {
		try {
			Algorithm algo = Algorithm.HMAC256(SECRET_KEY);
			JWTVerifier verifier = JWT.require(algo).build();
			DecodedJWT decodedJwt = verifier.verify(token);
			return decodedJwt;
		} catch (Exception e) {
			System.out.println("VerifySignature : " + e.getMessage());
			return null;
		}
	}
}