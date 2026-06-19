package com.stepside.StepSide.common.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

/**
 * Filtro perimetral de interceptación asíncrona.
 * Valida la presencia y legitimidad del Token JWT en las cabeceras HTTP de cada consulta privada.
 */
@Component
@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {

    private final JwtProvider jwtProvider;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // 1. Extraer la cabecera de Autorización del HTTP request
        String authHeader = request.getHeader("Authorization");

        // 2. Protocolo de Verificación: Evaluar si viene bajo el prefijo estándar 'Bearer '
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7); // Cortamos los primeros 7 caracteres ("Bearer ")

            // 3. Validar la firma criptográfica y expiración del Token en el motor
            if (jwtProvider.validateToken(token)) {
                String email = jwtProvider.getEmailFromToken(token);

                // 4. Si el contexto actual de Spring Security no está autenticado, inyectamos las credenciales
                if (SecurityContextHolder.getContext().getAuthentication() == null) {

                    // Instanciamos el token de autenticación pasándole el email (sin roles/autoridades por ahora = Collections.emptyList())
                    UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                            email, null, Collections.emptyList()
                    );

                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                    // Seteamos de forma atómica la identidad del usuario en el hilo actual de ejecución
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            }
        }

        // Delegamos el control al siguiente filtro de la cadena perimetral de Spring
        filterChain.doFilter(request, response);
    }
}
