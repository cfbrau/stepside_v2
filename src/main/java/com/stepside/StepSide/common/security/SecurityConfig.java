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

/**
 * El Único Candado Perimetral de StepSide.
 * Gobierna de forma centralizada las reglas de acceso BSON, CORS y filtros stateless.
 * Saneado por el Arquitecto para acoplar el ruteo seguro por tokens JWT.
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtFilter jwtFilter; // Inyección de nuestro nuevo filtro asíncrono

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // SANEAMIENTO MANDATORIO: Desactivamos CSRF ya que la autenticación viaja encapsulada en la cabecera Bearer
                .csrf(csrf -> csrf.disable())

                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Compuertas públicas del ecosistema unificado
                        .requestMatchers(
                                "/api/auth/signup",
                                "/api/auth/login",
                                "/api/auth/forgot-password/**",
                                "/swagger-ui/**",
                                "/v3/api-docs/**",
                                "/swagger-ui.html"
                        ).permitAll()

                        // Los endpoints de /api/users ya NO son públicos de libre acceso, requieren Token válido
                        .requestMatchers("/api/users/**").authenticated()

                        .anyRequest().authenticated()
                )
                // Inyectamos nuestro JwtFilter justo antes del filtro de autenticación por defecto de Spring
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * Puente global de red para tu Frontend de Live Server y entornos Cloud.
     */
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
}
