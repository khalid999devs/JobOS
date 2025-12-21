package com.jobos.backend.security.jwt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobos.backend.repository.RefreshTokenRepository;
import com.jobos.backend.repository.UserRepository;
import com.jobos.shared.dto.common.ApiResponse;
import com.jobos.shared.dto.common.ErrorResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.UUID;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final ObjectMapper objectMapper;

    public JwtAuthenticationFilter(JwtUtil jwtUtil, UserRepository userRepository, RefreshTokenRepository refreshTokenRepository, ObjectMapper objectMapper) {
        this.jwtUtil = jwtUtil;
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        
        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            String token = authHeader.substring(7);
            
            if (jwtUtil.validateToken(token)) {
                UUID userId = jwtUtil.getUserIdFromToken(token);
                UUID sessionId = jwtUtil.getSessionIdFromToken(token);
                String email = jwtUtil.getEmailFromToken(token);
                String role = jwtUtil.getRoleFromToken(token);

                if (refreshTokenRepository.findBySessionId(sessionId).isEmpty()) {
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.setContentType("application/json");
                    response.setCharacterEncoding("UTF-8");
                    ErrorResponse error = new ErrorResponse(
                            HttpServletResponse.SC_UNAUTHORIZED,
                            "Unauthorized",
                            "Session expired or invalid. Please login again.",
                            request.getRequestURI()
                    );
                    ApiResponse<Object> apiResponse = ApiResponse.error(error, "Session expired or invalid. Please login again.");
                    String jsonResponse = objectMapper.writeValueAsString(apiResponse);
                    response.getWriter().write(jsonResponse);
                    response.getWriter().flush();
                    return;
                }

                userRepository.findById(userId).ifPresent(user -> {
                    UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                            email,
                            null,
                            Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + role))
                    );
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                });
            }
            
            filterChain.doFilter(request, response);
        } catch (Exception e) {
            logger.error("JWT authentication failed", e);
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            ErrorResponse error = new ErrorResponse(
                    HttpServletResponse.SC_UNAUTHORIZED,
                    "Unauthorized",
                    "Invalid or expired token. Please login again.",
                    request.getRequestURI()
            );
            ApiResponse<Object> apiResponse = ApiResponse.error(error, "Invalid or expired token. Please login again.");
            String jsonResponse = objectMapper.writeValueAsString(apiResponse);
            response.getWriter().write(jsonResponse);
            response.getWriter().flush();
        }
    }
}
