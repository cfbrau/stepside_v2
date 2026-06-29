package com.stepside.StepSide.common.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtFilter jwtFilter; // Inyección de nuestro nuevo filtro asíncrono

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // MEJORA PRODUCTION: Captura fallas de tokens en canales de streaming en el microsegundo cero.
                // Responde un error SSE limpio y libera el hilo de Tomcat sin romper sockets.
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint((request, response, authException) -> {
                            String acceptHeader = request.getHeader("Accept");
                            if (acceptHeader != null && acceptHeader.contains("text/event-stream")) {
                                response.setStatus(jakarta.servlet.http.HttpServletResponse.SC_UNAUTHORIZED);
                                response.setContentType("text/event-stream");
                                response.setCharacterEncoding("UTF-8");
                                response.getWriter().write("event: ERROR\ndata: {\"status\":\"UNAUTHORIZED\",\"reason\":\"Token expirado o inválido\"}\n\n");
                                response.getWriter().flush();
                            } else {
                                response.sendError(jakarta.servlet.http.HttpServletResponse.SC_UNAUTHORIZED, authException.getMessage());
                            }
                        })
                )

                .authorizeHttpRequests(auth -> auth
                        // 1. COMPUERTAS PÚBLICAS LIMITADAS A ONBOARDING Y LOGIN (Cerrado hermético)
                        .requestMatchers(
                                "/api/auth/signup",
                                "/api/auth/login",
                                "/api/auth/forgot-password/**",
                                "/swagger-ui/**",
                                "/v3/api-docs/**",
                                "/swagger-ui.html"
                        ).permitAll()

                        // 2. COMPUERTAS AUTENTICADAS: El streaming exige estrictamente token firmado
                        .requestMatchers("/api/users/**").authenticated()

                        // 3. CIERRE PERIMETRAL GENERAL
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }


    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(List.of(
                "https://stepside-backend-v2-921706262238.southamerica-east1.run.app",
                "http://localhost:5500",   // Puerto clásico de Live Server (VS Code)
                "http://127.0.0.1:5500",   // Loopback clásico de Live Server
                "http://localhost:3000",   // Puerto clásico de React / Next.js
                "http://localhost:8080",
                "http://127.0.0.1:8080",
                "http://localhost:5173"    // Puerto clásico de Vite (React/Vue moderno)
        ));

        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "Cache-Control", "X-Requested-With"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer webSecurityCustomizer() {
        // Indica al núcleo que esta subruta de infraestructura no pertenece al dominio perimetral
        return (web) -> web.ignoring().requestMatchers("/error");
    }
}