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

    /**Genera un Token JWT firmado utilizando el email del usuario validado.*/
    public String generateToken(String email, String roleName) {
        java.util.Map<String, Object> claims = new java.util.HashMap<>();
        claims.put("roles", roleName.trim()); // Inyecta estrictamente el valor puro (ej: "ADMIN")

        // ÉXITO MÁXIMO: Construcción unificada y sólida avalada por el compilador de Java
        return io.jsonwebtoken.Jwts.builder()
                .setClaims(claims)
                .setSubject(email.trim())
                .setIssuedAt(new java.util.Date(System.currentTimeMillis()))
                .setExpiration(new java.util.Date(System.currentTimeMillis() + 86400000)) // 24 horas de vigencia
                .signWith(SECRET_KEY) // <-- SOLUCIÓN: Inyectamos el objeto de seguridad tipado nativo
                .compact();
    }


    /**Extrae el email (Subject) que viene adentro del Token.*/
    public String getEmailFromToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(SECRET_KEY)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        return claims.getSubject();
    }

    /**Valida si la firma del Token es legítima y si no ha expirado.*/
    public boolean validateToken(String token) {
        try {
            Jwts.parser().verifyWith(SECRET_KEY).build().parseSignedClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
