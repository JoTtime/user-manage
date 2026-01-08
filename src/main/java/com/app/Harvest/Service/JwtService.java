package com.app.Harvest.Service;

import org.springframework.security.core.userdetails.UserDetails;

import java.util.Map;

public interface JwtService {

    String generateToken(UserDetails userDetails);

    // Add this overloaded method for custom claims
    String generateToken(UserDetails userDetails, Map<String, Object> extraClaims);

    String extractUsername(String token);

    Boolean validateToken(String token, UserDetails userDetails);

    Boolean isTokenExpired(String token);
}