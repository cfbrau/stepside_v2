package com.stepside.StepSide.integration.arca.dto;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import lombok.Data;
import java.util.List;

@Data
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "personaReturn")
public class ArcaPersonaResponseDto {

    @XmlElement(name = "persona")
    private PersonaData persona;

    @Data
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class PersonaData {
        private String idPersona;
        private String apellido;
        private String nombre;
        private String razonSocial;
        private String formaJuridica;
        private String tipoPersona;
        private String tipoClave;
        private String estadoClave;
        private String numeroDocumento;
        private String tipoDocumento;
        private String idActividadPrincipal;
        private String descripcionActividadPrincipal;

        @XmlElement(name = "domicilio")
        private List<DomicilioDto> domicilios;
    }
}
