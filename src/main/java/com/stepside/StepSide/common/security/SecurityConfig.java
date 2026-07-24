package com.stepside.StepSide.common.security;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtFilter jwtFilter;

    // Inyección limpia y desacoplada de los orígenes según el entorno activo
    @Value("${stepside.security.cors.allowed-origins}")
    private final List<String> allowedOrigins;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // DSL Moderna de Spring Security 3.x sin lambdas redundantes
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // MANEJO PERIMETRAL DE EXCEPCIONES: Captura fallas de tokens en microsegundo cero
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint((request, response, authException) -> {
                            String acceptHeader = request.getHeader(HttpHeaders.ACCEPT);

                            // Detección segura de canales streaming o Server-Sent Events (SSE)
                            if (acceptHeader != null && acceptHeader.contains("text/event-stream")) {
                                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                                response.setContentType("text/event-stream");
                                response.setCharacterEncoding("UTF-8");
                                response.getWriter().write("event: ERROR\ndata: {\"status\":\"UNAUTHORIZED\",\"reason\":\"Token expirado o inválido\"}\n\n");
                                response.getWriter().flush();
                            } else {
                                // Saneado: Evita revelar detalles crudos de excepciones internas
                                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Acceso denegado: Credenciales no válidas.");
                            }
                        })
                )

                .authorizeHttpRequests(auth -> auth
                        // 1. COMPUERTAS PÚBLICAS (Saneado: /error incluido en el perímetro de Spring Security)
                        .requestMatchers(
                                "/api/auth/signup",
                                "/api/auth/login",
                                "/api/auth/forgot-password/**",
                                "/swagger-ui/**",
                                "/v3/api-docs/**",
                                "/swagger-ui.html",
                                "/error" // Al permitirlo aquí, evitamos usar web.ignoring() que desprotegía el hilo
                        ).permitAll()

                        // 2. COMPUERTAS AUTENTICADAS
                        .requestMatchers("/api/users/**").authenticated()

                        // 3. CIERRE PERIMETRAL GENERAL (Zero Trust)
                        .anyRequest().authenticated()
                )
                // Inyección perimetral de tu filtro personalizado
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // Uso estricto de la propiedad dinámica inyectada desde los archivos properties
        configuration.setAllowedOrigins(allowedOrigins);
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));

        // Saneado: Uso de constantes HTTP estándar de Spring para mitigar errores de tipeo manual
        configuration.setAllowedHeaders(List.of(
                HttpHeaders.AUTHORIZATION,
                HttpHeaders.CONTENT_TYPE,
                HttpHeaders.CACHE_CONTROL,
                "X-Requested-With"
        ));
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
