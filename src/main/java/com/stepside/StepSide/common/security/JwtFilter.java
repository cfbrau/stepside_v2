package com.stepside.StepSide.common.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * FILTRO PERIMETRAL DE INSPECCIÓN DE DATOS: Ecosistema StepSide.
 * Optimizado bajo estándares de producción, mitigación de hilos bloqueantes y seguridad OWASP.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";
    private static final String TOKEN_PARAM = "token";

    private final JwtProvider jwtProvider;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        try {
            String token = extractToken(request);

            if (StringUtils.hasText(token) && jwtProvider.validateToken(token)) {
                setupAuthentication(token, request);
            }
        } catch (Exception e) {
            // Registro forense asíncrono no bloqueante omitiendo fugas de información al cliente
            log.error("Fallo crítico en el perímetro de autenticación JWT: {}", e.getMessage());
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Extrae el token desde los encabezados HTTP o fallback por query parameter.
     */
    private String extractToken(HttpServletRequest request) {
        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (StringUtils.hasText(authHeader) && authHeader.startsWith(BEARER_PREFIX)) {
            return authHeader.substring(BEARER_PREFIX.length()).trim();
        }

        // Soporte controlado para canales persistentes / streaming
        return request.getParameter(TOKEN_PARAM);
    }

    /**
     * Valida, procesa y asienta la identidad en el ecosistema de Spring Security.
     */
    private void setupAuthentication(String token, HttpServletRequest request) {
        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            return;
        }

        String email = jwtProvider.getEmailFromToken(token);
        List<GrantedAuthority> authorities = jwtProvider.getAuthoritiesFromToken(token);

        // Si el token criptográfico carece de jerarquía, el EntryPoint responderá un 401 unificado
        if (authorities.isEmpty()) {
            log.warn("[SEGURIDAD PERIMETRAL] Intento de acceso denegado: Token sin roles asignados: {}", email);
            return;
        }

        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                email, null, authorities
        );

        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authentication);

        log.debug("[AUDITORÍA] Autenticación establecida exitosamente para el principal: {}", email);
    }
}
