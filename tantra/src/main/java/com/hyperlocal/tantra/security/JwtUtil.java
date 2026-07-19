package com.hyperlocal.tantra.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;

@Component
public class JwtUtil {

    // 🔥 Use a fixed, secure signing key so tokens remain valid after restarting the server
    private final String SECRET_STRING = "7f3c8b2a1e4d6f9a8c7b6a5e4d3c2b1a0f9e8d7c6b5a4f3e2d1c0b9a8f7e6d5c";
    private final Key key = Keys.hmacShaKeyFor(SECRET_STRING.getBytes(StandardCharsets.UTF_8));

    private final long jwtExpirationMs = 31536000000L; // 365 Days

    // Set token validity to 365 days (approx 1 year) for long term user login session

    public String generateToken(String mobileNumber, String role) {
        return Jwts.builder()
                .setSubject(mobileNumber)
                .claim("role", role)
                .setIssuedAt(new Date())
                .setExpiration(new Date((new Date()).getTime() + jwtExpirationMs))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * Extracts the registered mobile number (subject) from the payload.
     */
    public String extractMobileNumber(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
    }

    /**
     * Extracts the expiration date of the JWT.
     */
    public Date extractExpiration(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getExpiration();
    }

    /**
     * Assesses whether the token subject matches user mobile number and is not expired.
     */
    public boolean validateToken(String token, String mobileNumber) {
        final String extractedMobile = extractMobileNumber(token);
        return (extractedMobile.equals(mobileNumber) && !extractExpiration(token).before(new Date()));
    }
}