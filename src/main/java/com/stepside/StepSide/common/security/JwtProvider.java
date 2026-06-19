package com.stepside.StepSide.common.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * Proveedor perimetral criptográfico para el ciclo de vida de los Tokens JWT.
 */
@Component
public class JwtProvider {

    // Clave secreta dura de 256 bits para firmar el token (Mantenida segura en producción)
    private static final String SECRET_STRING = "StepSideSuperSecretKeyForJWTTokenGeneration2026SeniorArchitecture!!!";
    private static final SecretKey SECRET_KEY = Keys.hmacShaKeyFor(SECRET_STRING.getBytes(StandardCharsets.UTF_8));

    // Tiempo de expiración del Token: 8 Horas en milisegundos
    private static final long EXPIRATION_TIME = 28800000L;

    /**
     * Genera un Token JWT firmado utilizando el email del usuario validado.
     */
    public String generateToken(String email) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + EXPIRATION_TIME);

        return Jwts.builder()
                .subject(email)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(SECRET_KEY)
                .compact();
    }

    /**
     * Extrae el email (Subject) que viene adentro del Token.
     */
    public String getEmailFromToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(SECRET_KEY)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        return claims.getSubject();
    }

    /**
     * Valida si la firma del Token es legítima y si no ha expirado.
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parser().verifyWith(SECRET_KEY).build().parseSignedClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
