package com.stepside.StepSide.integration.arca.dto;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import lombok.Data;

@Data
@XmlAccessorType(XmlAccessType.FIELD)
public class DomicilioDto {
    private String direccion;
    private String calle;
    private String numero;
    private String localidad;
    private String idProvincia;
    private String descripcionProvincia;
    private String codigoPostal;
    private String tipoDomicilio;
    private String estadoDomicilio;
}
