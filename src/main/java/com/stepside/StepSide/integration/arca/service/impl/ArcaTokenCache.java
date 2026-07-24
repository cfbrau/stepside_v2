package com.stepside.StepSide.integration.arca.service.impl;

import lombok.Data;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;

/**
 * Contenedor Singleton en memoria encargado de custodiar las credenciales activas.
 * Evita la saturación del clúster de autenticación de ARCA.
 */
@Component
@Data
public class ArcaTokenCache {

    private String token;
    private String sign;
    private LocalDateTime fechaExpiracion;

    /**
     * Valida si las credenciales en memoria siguen siendo legítimas y vigentes.
     * Deja un margen de seguridad de 10 minutos para evitar desincronizaciones de reloj.
     */
    public boolean esValido() {
        if (token == null || sign == null || fechaExpiracion == null) {
            return false;
        }
        return LocalDateTime.now().isBefore(fechaExpiracion.minusMinutes(10));
    }

    /**
     * Limpia el estado de la caché forzando una nueva autenticación en el próximo ciclo.
     */
    public void desalojar() {
        this.token = null;
        this.sign = null;
        this.fechaExpiracion = null;
    }
}
