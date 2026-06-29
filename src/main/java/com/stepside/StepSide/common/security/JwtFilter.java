package com.stepside.StepSide.common.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;

/**
 * FILTRO PERIMETRAL DE INSPECCIÓN DE DATOS: Ecosistema StepSide.
 * Corregido por Fabián aplicando telemetría forense para auditar las llaves internas del JWT.
 */
@Component
@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {

    private final JwtProvider jwtProvider;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String token = null;
        String authHeader = request.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            token = authHeader.substring(7);
        } else if (request.getParameter("token") != null) {
            token = request.getParameter("token");
        }

        if (token != null && jwtProvider.validateToken(token)) {
            String email = jwtProvider.getEmailFromToken(token);

            if (SecurityContextHolder.getContext().getAuthentication() == null) {

                String rolReal = null;
                try {
                    // 1. Aislamiento del Payload (Parte central del JWT)
                    String[] partes = token.split("\\.");
                    if (partes.length >= 2) {
                        String base64Payload = partes[1]; // Muerde strictly el cuerpo central del token

                        // Ajuste elástico de padding Base64
                        int paddingFaltante = 4 - (base64Payload.length() % 4);
                        if (paddingFaltante < 4) {
                            for (int i = 0; i < paddingFaltante; i++) base64Payload += "=";
                        }

                        // 2. Decodificación limpia a texto plano UTF-8
                        String jsonCrudoConvertido = new String(
                                java.util.Base64.getUrlDecoder().decode(base64Payload),
                                java.nio.charset.StandardCharsets.UTF_8
                        );

                        // ============================================================================
                        // 🛰️ TELEMETRÍA FORENSE DE RED: Imprime el JSON real que viaja por el cable
                        // ============================================================================
                        System.out.println("====================================================================");
                        System.out.println("[AUDITORÍA JWT] Contenido crudo del Payload que llegó al servidor:");
                        System.out.println(jsonCrudoConvertido);
                        System.out.println("====================================================================");

                        // SANEADO: Usamos estrictamente la variable correcta mapeada en el bloque superior
                        if (jsonCrudoConvertido.contains("\"roles\":")) {
                            int inicio = jsonCrudoConvertido.indexOf("\"roles\":\"") + 9;
                            int fin = jsonCrudoConvertido.indexOf("\"", inicio);
                            rolReal = jsonCrudoConvertido.substring(inicio, fin).trim();
                        } else if (jsonCrudoConvertido.contains("\"authorities\":")) {
                            System.out.println("[AUDITORÍA] Estructura alternativa detectada: authorities");
                        }
                    }
                } catch (Exception e) {
                    System.out.println("[AUDITORÍA ERROR] Falló la decodificación interna: " + e.getMessage());
                    rolReal = null;
                }

                // Escudo de expulsión defensiva OWASP
                if (rolReal == null || rolReal.isBlank()) {
                    throw new AccessDeniedException(
                            "Falla perimetral: El pasaporte criptográfico no declara una jerarquía de acceso válida."
                    );
                }

                String formattedAuthority = "ROLE_" + rolReal.trim().toUpperCase();
                org.springframework.security.core.authority.SimpleGrantedAuthority authority =
                        new org.springframework.security.core.authority.SimpleGrantedAuthority(formattedAuthority);

                // Instanciamos el token cargando su jerarquía real normalizada en la CPU
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        email, null, java.util.List.of(authority)
                );

                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        }

        filterChain.doFilter(request, response);
    }
}
