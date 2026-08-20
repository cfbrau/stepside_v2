package com.stepside.StepSide.integration.arca.service;

import com.stepside.StepSide.integration.arca.dto.ArcaPersonaResponseDto;

/**
 * Contrato de negocio encargado de orquestar las consultas al Padrón Alcance 4 de ARCA.
 */
public interface ArcaPadronService {

    /**
     * Consulta los datos perimetrales de un contribuyente en el clúster de ARCA.
     * Retorna el XML crudo en frío para analizar la respuesta forense de red.
     *
     * @param cuitConsultar Número de CUIT (sin guiones) que se desea investigar.
     * @return String con el XML crudo devuelto por el servidor gubernamental.
     */
    String consultarPadronCrudo(String cuitConsultar);

    /**
     * Recupera la persona ya mapeada a DTO para ser entregada al frontend.
     *
     * @param cuitConsultar Número de CUIT (sin guiones) que se desea investigar.
     * @return DTO de persona ya transformado para el consumo del cliente.
     */
    ArcaPersonaResponseDto.PersonaData obtenerPersonaMapeada(String cuitConsultar);
}
