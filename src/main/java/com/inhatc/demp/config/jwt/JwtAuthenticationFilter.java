package com.inhatc.demp.config.jwt;

import tools.jackson.databind.ObjectMapper;
import com.inhatc.demp.config.security.SecurityErrorWriter;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtTokenProvider jwtTokenProvider;
    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws IOException, ServletException {
        String token = jwtTokenProvider.resolveToken(request);
        if (token != null) {
            try {
                if (!jwtTokenProvider.validateToken(token)) {
                    SecurityErrorWriter.write(objectMapper, request, response, HttpStatus.UNAUTHORIZED);
                    return;
                }
                SecurityContextHolder.getContext().setAuthentication(jwtTokenProvider.getAuthentication(token));
            } catch (AuthenticationException | JwtException | IllegalArgumentException ex) {
                SecurityContextHolder.clearContext();
                SecurityErrorWriter.write(objectMapper, request, response, HttpStatus.UNAUTHORIZED);
                return;
            } catch (RuntimeException ex) {
                SecurityContextHolder.clearContext();
                SecurityErrorWriter.write(objectMapper, request, response, HttpStatus.INTERNAL_SERVER_ERROR);
                return;
            }
        }
        chain.doFilter(request, response);
    }
}
