package com.stepside.StepSide.common.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stepside.StepSide.common.exception.ErrorResponseDto;
import jakarta.servlet.http.HttpServletResponse;
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

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtFilter jwtFilter;
    private final List<String> allowedOrigins;
    private final String[] publicPaths;
    private final ObjectMapper objectMapper;

    public SecurityConfig(
            JwtFilter jwtFilter,
            ObjectMapper objectMapper,
            @Value("${stepside.security.cors.allowed-origins}") String allowedOriginsStr,
            @Value("${stepside.security.public-paths}") String[] publicPaths) {
        this.jwtFilter = jwtFilter;
        this.objectMapper = objectMapper;
        this.allowedOrigins = Arrays.asList(allowedOriginsStr.split("[,;]"));
        this.publicPaths = publicPaths;
    }

    //Filtrado
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, CorsConfigurationSource corsConfigurationSource) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint((request, response, authException) -> {
                            String acceptHeader = request.getHeader(HttpHeaders.ACCEPT);

                            if (acceptHeader != null && acceptHeader.contains("text/event-stream")) {
                                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                                response.setContentType("text/event-stream");
                                response.setCharacterEncoding("UTF-8");
                                response.getWriter().write("event: ERROR\ndata: {\"status\":\"UNAUTHORIZED\",\"reason\":\"Token expirado o inválido\"}\n\n");
                                response.getWriter().flush();
                            } else {
                                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                                response.setContentType("application/json");
                                response.setCharacterEncoding("UTF-8");

                                ErrorResponseDto errorResponse = new ErrorResponseDto(
                                        LocalDateTime.now(),
                                        HttpServletResponse.SC_UNAUTHORIZED,
                                        "INVALID_CREDENTIALS",
                                        "Credenciales inválidas.",
                                        request.getRequestURI(),
                                        null
                                );

                                objectMapper.writeValue(response.getOutputStream(), errorResponse);
                            }
                        })
                )

                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(publicPaths).permitAll()
                        .requestMatchers("/api/users/**").authenticated()
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(allowedOrigins);
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
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
