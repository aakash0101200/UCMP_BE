package com.ucmp.ucmp_backend.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;
import java.security.Key;
import java.util.Date;
import java.util.Set;

@Component
public class JwtUtil  {
    private final Key secretKey = Keys.secretKeyFor(SignatureAlgorithm.HS256);
    private final long expirationMs = 3600000; // 1 hour

    // Generate token with collegeId as subject and role as claim
    public String generateToken(String collegeId, Set<String> role) {
        return Jwts.builder()
                .setSubject(collegeId)
                .claim("role", role)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expirationMs))
                .signWith(secretKey)
                .compact();
    }

    // Extract full claims (used internally)
    public Claims extractClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(secretKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    // Helper: Extract token from Authorization header
    public String extractTokenFromRequest(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        return (header != null && header.startsWith("Bearer ")) ? header.substring(7) : null;
    }

    // Helper: Extract college ID (subject)
    public String extractCollegeId(String token) {
        return extractClaims(token).getSubject();
    }

    // Helper: Extract user role
    public String extractRole(String token) {
        return extractClaims(token).get("role", String.class);
    }

    // Check if the token is expired
    public Boolean isTokenExpired(String token) {
        return extractClaims(token).getExpiration().before(new Date());
    }

    public boolean validateToken(String token, String collegeId) {
        try {
            final String extractedCollegeId = extractCollegeId(token);
            // Check if the extracted ID matches the expected ID AND the token is not expired
            return (extractedCollegeId.equals(collegeId) && !isTokenExpired(token));
        } catch (Exception e) {
            // Log the exception for debugging purposes.
            System.err.println("JWT validation failed: " + e.getMessage());
            return false;
        }
    }
}