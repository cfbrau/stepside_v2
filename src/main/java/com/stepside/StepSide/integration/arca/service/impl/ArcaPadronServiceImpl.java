package com.stepside.StepSide.integration.arca.service.impl;

import com.stepside.StepSide.integration.arca.client.ArcaSoapClient;
import com.stepside.StepSide.integration.arca.dto.ArcaPersonaResponseDto;
import com.stepside.StepSide.integration.arca.service.ArcaPadronService;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.Unmarshaller;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.StringReader;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

@Service
@Slf4j
@RequiredArgsConstructor
public class ArcaPadronServiceImpl implements ArcaPadronService {

    private static final String SOAP_ACTION = "";

    private final ArcaAuthServiceImpl arcaAuthService;
    private final ArcaSoapClient arcaSoapClient;

    // Inyección de la caché singleton en memoria
    private final ArcaTokenCache arcaTokenCache;

    @Value("${stepside.arca.service-url-padronprodd}")
    private String padronUrlProduccion;

    @Override
    public String consultarPadronCrudo(String cuitConsultar) {
        String targetCuit = cuitConsultar.trim().replaceAll("\\D", "");

        String tokenReal;
        String signReal;

        try {
            // EVALUACIÓN DE LA CACHÉ SINGLETON
            if (arcaTokenCache.esValido()) {
                log.info("[ARCA CACHÉ HIT] Reutilizando Token y Sign vigentes en memoria de StepSide.");
                tokenReal = arcaTokenCache.getToken();
                signReal = arcaTokenCache.getSign();
            } else {
                log.info("[ARCA CACHÉ MISS] Credenciales expiradas o nulas. Conectando al WSAA de Producción...");
                String xmlCrudoWsaa = arcaAuthService.obtenerTicketAccesoProduccion();

                tokenReal = extraerTagXml(xmlCrudoWsaa, "token");
                signReal = extraerTagXml(xmlCrudoWsaa, "sign");

                // Extraemos la fecha de expiración real que firmó ARCA (ej: 2026-07-25T02:32:22.359-03:00)
                String expirationStr = extraerTagXml(xmlCrudoWsaa, "expirationTime");
                ZonedDateTime zonedDateTime = ZonedDateTime.parse(expirationStr, DateTimeFormatter.ISO_OFFSET_DATE_TIME);
                LocalDateTime fechaExpiracionARCA = zonedDateTime.toLocalDateTime();

                // Guardamos en el Singleton para los próximos requests
                arcaTokenCache.setToken(tokenReal);
                arcaTokenCache.setSign(signReal);
                arcaTokenCache.setFechaExpiracion(fechaExpiracionARCA);

                log.info("[ARCA CACHÉ REFRESH] Nuevas credenciales almacenadas. Válidas hasta: {}", fechaExpiracionARCA);
            }

            String soapEnvelopeFirmado = arcaAuthService.consultarPersonaRealA13(tokenReal, signReal, targetCuit);
            return arcaSoapClient.sendSoapRequest(padronUrlProduccion, SOAP_ACTION, soapEnvelopeFirmado);

        } catch (Exception e) {
            log.error("[ARCA PRODUCCIÓN CRITICAL] Error en pipeline de negocio con caché: ", e);
            arcaTokenCache.desalojar(); // Desalojamos por seguridad ante fallas críticas
            throw new IllegalStateException("Falla en pipeline Padrón A13: " + e.getMessage());
        }
    }

    public ArcaPersonaResponseDto.PersonaData obtenerPersonaMapeada(String cuitConsultar) {
        String xmlResponsePadron = this.consultarPadronCrudo(cuitConsultar);
        try {
            int inicio = xmlResponsePadron.indexOf("<personaReturn>");
            int fin = xmlResponsePadron.indexOf("</personaReturn>") + "</personaReturn>".length();

            if (inicio == -1 || fin == -1) {
                throw new IllegalStateException("La respuesta del clúster de negocio no contiene el elemento <personaReturn>");
            }

            String xmlLimpioNegocio = xmlResponsePadron.substring(inicio, fin);

            JAXBContext jaxbContext = JAXBContext.newInstance(ArcaPersonaResponseDto.class);
            Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();

            StringReader reader = new StringReader(xmlLimpioNegocio);
            ArcaPersonaResponseDto responseDto = (ArcaPersonaResponseDto) unmarshaller.unmarshal(reader);

            return responseDto.getPersona();
        } catch (Exception e) {
            log.error("[ARCA CORE PARSER] Error al procesar o desmapear la estructura XML: ", e);
            throw new IllegalArgumentException("Error en el Marshalling de datos fiscales: " + e.getMessage());
        }
    }

    private String extraerTagXml(String xml, String tag) {
        try {
            String tagInicio = "&lt;" + tag + "&gt;";
            String tagFin = "&lt;/" + tag + "&gt;";
            int inicio = xml.indexOf(tagInicio) + tagInicio.length();
            int fin = xml.indexOf(tagFin);
            return xml.substring(inicio, fin).trim();
        } catch (Exception e) {
            throw new IllegalArgumentException("No se pudo extraer la credencial '" + tag + "' del WSAA.");
        }
    }
}
