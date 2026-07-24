package com.stepside.StepSide.common.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Proveedor perimetral criptográfico para el ciclo de vida de los Tokens JWT.
 * Saneado bajo la especificación estricta de JJWT 0.12.5 y desacoplamiento de entornos.
 */
@Slf4j
@Component
public class JwtProvider {

    private static final String ROLE_CLAIM = "roles";
    private static final String ROLE_PREFIX = "ROLE_";

    private final SecretKey secretKey;
    private final long expirationTimeMillis;

    // Inyección limpia y desacoplada desde tu archivo application.properties
    public JwtProvider(
            @Value("${stepside.jwt.secret}") String secretString,
            @Value("${stepside.jwt.expiration-hours}") long expirationHours) {

        // Conversión segura de tu secreto a la interfaz criptográfica nativa de Java
        this.secretKey = Keys.hmacShaKeyFor(secretString.getBytes(StandardCharsets.UTF_8));
        // Conversión elástica de horas configuradas a milisegundos de CPU
        this.expirationTimeMillis = expirationHours * 60 * 60 * 1000;
    }

    /**
     * Genera un Token JWT firmado utilizando la API moderna no-obsoleta de JJWT 0.12.x.
     */
    public String generateToken(String email, String roleName) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expirationTimeMillis);

        // CORRECTO: Uso de la API fluida recomendada para producción
        return Jwts.builder()
                .subject(email.trim())
                .claims(Map.of(ROLE_CLAIM, roleName.trim().toUpperCase()))
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(secretKey)
                .compact();
    }

    /**
     * Extrae el email (Subject) que viene adentro del Token de manera segura.
     */
    public String getEmailFromToken(String token) {
        return extractAllClaims(token).getSubject();
    }

    /**
     * Extrae y normaliza las autoridades para Spring Security, completando el contrato con el JwtFilter.
     */
    public List<GrantedAuthority> getAuthoritiesFromToken(String token) {
        Claims claims = extractAllClaims(token);
        String role = claims.get(ROLE_CLAIM, String.class);

        if (role == null || role.isBlank()) {
            return List.of();
        }

        String formattedRole = role.startsWith(ROLE_PREFIX) ? role : ROLE_PREFIX + role;
        return List.of(new SimpleGrantedAuthority(formattedRole));
    }

    /**
     * Valida si la firma del Token es legítima emitiendo logs específicos de auditoría forense.
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (SignatureException e) {
            log.error("[AUDITORÍA CRIPTOGRÁFICA] Firma JWT inválida o manipulada fraudulentamente.");
        } catch (MalformedJwtException e) {
            log.error("[AUDITORÍA CRIPTOGRÁFICA] Estructura del Token JWT corrupta o malformada.");
        } catch (ExpiredJwtException e) {
            log.warn("[AUDITORÍA CRIPTOGRÁFICA] Acceso denegado: El Token JWT ha expirado.");
        } catch (UnsupportedJwtException e) {
            log.error("[AUDITORÍA CRIPTOGRÁFICA] Token JWT no soportado por el motor perimetral.");
        } catch (IllegalArgumentException e) {
            log.error("[AUDITORÍA CRIPTOGRÁFICA] El set de Claims del JWT se encuentra vacío.");
        }
        return false;
    }

    /**
     * Centraliza el parsing seguro del payload del token.
     */
    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
