package com.takhunter.backend.config;

import com.takhunter.backend.model.User;
import com.takhunter.backend.repository.UserRepository;
import com.takhunter.backend.util.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;

    public JwtAuthenticationFilter(JwtUtil jwtUtil, UserRepository userRepository) {
        this.jwtUtil = jwtUtil;
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String authorizationHeader = request.getHeader("Authorization");
        String requestURI = request.getRequestURI();

        // Cek apakah request membutuhkan pengawasan log otentikasi
        boolean shouldLog = requestURI.startsWith("/api/eo/kegiatan") || requestURI.startsWith("/api/pendaftaran");

        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            if (shouldLog) {
                log.info("Akses publik/tanpa token diizinkan untuk melewati filter ke rute: {} {}", request.getMethod(), requestURI);
            }
            filterChain.doFilter(request, response);
            return;
        }

        String token = authorizationHeader.substring(7);
        if (!jwtUtil.isTokenValid(token)) {
            if (shouldLog) {
                log.warn("JWT invalid/expired untuk {} {}", request.getMethod(), requestURI);
            }
            filterChain.doFilter(request, response);
            return;
        }

        String email = jwtUtil.getEmailFromToken(token);
        User user = userRepository.findByEmail(email).orElse(null);

        if (user != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            String normalizedRole = normalizeRole(user.getRole());
            SimpleGrantedAuthority authority = new SimpleGrantedAuthority("ROLE_" + normalizedRole);
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    user,
                    null,
                    List.of(authority)
            );
            SecurityContextHolder.getContext().setAuthentication(authentication);

            if (shouldLog) {
                log.info(
                        "JWT terautentikasi untuk {} {} email={} dbRole={} authority={}",
                        request.getMethod(),
                        requestURI,
                        user.getEmail(),
                        user.getRole(),
                        authority.getAuthority()
                );
            }
        } else if (shouldLog) {
            log.warn("JWT valid tetapi data user tidak ditemukan di database untuk {} {} email={}", request.getMethod(), requestURI, email);
        }

        filterChain.doFilter(request, response);
    }

    private String normalizeRole(String role) {
        if (role == null) {
            return "";
        }

        return role.trim()
                .replace("-", "_")
                .toUpperCase();
    }
}