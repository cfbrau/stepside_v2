package com.stepside.StepSide.common.security; // <-- Centralizado en el núcleo de infraestructura

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import java.util.List;

/**
 * El Único Candado Perimetral de StepSide.
 * Gobierna de forma centralizada las reglas de acceso BSON, CORS y filtros stateless.
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // CORRECCIÓN CRÍTICA: Desactivamos el filtro CSRF mandatorio para APIs Stateless en la nube
                .csrf(csrf -> csrf.disable())
                
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Compuertas públicas del ecosistema unificado
                        .requestMatchers(
                                "/api/auth/signup",
                                "/api/auth/login",
                                "/api/auth/forgot-password/**",
                                "/api/users/**"
                        ).permitAll()
                        .anyRequest().authenticated() // Cierre hermético para dashboards y telemetría
                );

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
                "http://127.0.0.1:5500",
                "http://localhost:[*]"
        ));

        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "Cache-Control", "X-Requested-With"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    /**
     * BEAN DE CONVERSOR: Habilita el cifrado simétrico BCrypt requerido
     * en tu TtoRegistrationServiceImpl para hashear las claves.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
