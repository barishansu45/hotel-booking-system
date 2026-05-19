package com.hotelbooking.hotel.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Value("${app.supabase.jwt-secret}")
    private String jwtSecret;

    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                    HttpServletResponse response, 
                                    FilterChain filterChain) throws ServletException, IOException {
        
        String authHeader = request.getHeader("Authorization");
        
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            String token = authHeader.substring(7);
            Claims claims = validateToken(token);
            
            if (claims != null) {
                String userId = claims.getSubject();
                String email = claims.get("email", String.class);
                String role = extractRole(claims);

                List<SimpleGrantedAuthority> authorities = role != null && !role.isBlank()
                    ? Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()))
                    : Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"));
                
                UsernamePasswordAuthenticationToken authentication = 
                    new UsernamePasswordAuthenticationToken(userId, null, authorities);
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                
                SecurityContextHolder.getContext().setAuthentication(authentication);
                
                request.setAttribute("userId", userId);
                request.setAttribute("userEmail", email);
                request.setAttribute("userRole", role);
                
                log.debug("Authenticated user: {} with role: {}", email, role);
            }
        } catch (Exception e) {
            log.error("JWT validation failed: {}", e.getMessage());
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Supabase puts roles in {@code app_metadata} / {@code user_metadata}, not a top-level {@code role} claim.
     */
    private String extractRole(Claims claims) {
        String r = claims.get("role", String.class);
        if (r != null && !r.isBlank()) {
            return r.trim();
        }
        Object userMeta = claims.get("user_metadata");
        if (userMeta instanceof java.util.Map<?, ?> map) {
            Object role = map.get("role");
            if (role != null && !role.toString().isBlank()) {
                return role.toString().trim();
            }
        }
        Object appMeta = claims.get("app_metadata");
        if (appMeta instanceof java.util.Map<?, ?> map) {
            Object role = map.get("role");
            if (role != null && !role.toString().isBlank()) {
                return role.toString().trim();
            }
        }
        return null;
    }

    private Claims validateToken(String token) {
        try {
            SecretKey key = new SecretKeySpec(
                jwtSecret.getBytes(StandardCharsets.UTF_8), 
                "HmacSHA256"
            );
            
            return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        } catch (Exception e) {
            log.error("Token validation error: {}", e.getMessage());
            return null;
        }
    }
}
