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
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class JwtProvider {

    private static final String ROLE_CLAIM = "roles";
    private static final String ROLE_PREFIX = "ROLE_";

    private final SecretKey secretKey;
    private final long expirationTimeMillis;

    public JwtProvider(
            @Value("${stepside.jwt.secret}") String secretString,
            @Value("${stepside.jwt.expiration-hours}") String expirationHoursStr) {

        if (secretString == null || secretString.isBlank()) {
            throw new IllegalStateException("La propiedad stepside.jwt.secret no puede estar vacía.");
        }

        byte[] secretBytes = secretString.getBytes(StandardCharsets.UTF_8);
        this.secretKey = new SecretKeySpec(secretBytes, "HmacSHA256");
        long hours = Long.parseLong(expirationHoursStr.trim());
        this.expirationTimeMillis = hours * 60 * 60 * 1000;
    }


    // MANTIENE LA COMPATIBILIDAD CLÁSICA: Para que no falle nada de lo que ya tenías
    public String generateToken(String email, String roleName) {
        return generateToken(email, roleName, "UNKNOWN_COMPANY");
    }

    // SOBRECARGA MULTITENANT REAL: Almacena la empresa en un claim inmutable
    public String generateToken(String email, String roleName, String companyId) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expirationTimeMillis);
        String normalizedEmail = email.trim();
        String normalizedRole = roleName.trim().toUpperCase();
        String normalizedCompany = companyId != null ? companyId.trim() : "UNKNOWN_COMPANY";

        return Jwts.builder()
                .subject(normalizedEmail)
                .claims(Map.of(
                        ROLE_CLAIM, normalizedRole,
                        "company", normalizedCompany // <-- El claim viaja blindado en el token
                ))
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(secretKey)
                .compact();
    }


    public String getEmailFromToken(String token) {
        return extractAllClaims(token).getSubject();
    }

    public List<GrantedAuthority> getAuthoritiesFromToken(String token) {
        Claims claims = extractAllClaims(token);
        String role = claims.get(ROLE_CLAIM, String.class);

        if (role == null || role.isBlank()) {
            return List.of();
        }

        String formattedRole = role.startsWith(ROLE_PREFIX) ? role : ROLE_PREFIX + role;
        return List.of(new SimpleGrantedAuthority(formattedRole));
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (SignatureException e) {
            logJwtDiagnostic(token, "Diagnóstico JWT sin verificar");
            log.error("[AUDITORÍA CRIPTOGRÁFICA] Firma JWT inválida o manipulada fraudulentamente.");
        } catch (MalformedJwtException e) {
            logJwtDiagnostic(token, "Diagnóstico JWT malformado");
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

    private void logJwtDiagnostic(String token, String context) {
        if (token == null || token.isBlank()) {
            log.warn("{}: token vacío o ausente.", context);
            return;
        }

        String[] segments = token.split("\\.", -1);
        String header = decodeJwtSegment(segments, 0);
        String payload = decodeJwtSegment(segments, 1);

//        log.info("{}: longitud={}, segmentos={}, header={}, payload={}.",
//                context, token.length(), segments.length, header, payload);
    }

    private String decodeJwtSegment(String[] segments, int index) {
        if (segments.length <= index || segments[index].isBlank()) {
            return "<segmento-ausente>";
        }

        try {
            byte[] decoded = Base64.getUrlDecoder().decode(segments[index]);
            return new String(decoded, StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            return "<segmento-base64url-invalido>";
        }
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
