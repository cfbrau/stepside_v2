package com.stepside.StepSide.integration.arca.client;

/**
 * Motor perimetral genérico de transporte para la ejecución de sobres XML/SOAP contra ARCA.
 */
public interface ArcaSoapClient {

    /**
     * Despacha un sobre XML mediante canales POST y captura la respuesta textual cruda.
     *
     * @param endpointUrl Dirección física del Web Service de ARCA.
     * @param soapAction Cabecera HTTP SOAPAction exigida por la infraestructura IIS de ARCA.
     * @param xmlPayload Cuerpo del sobre XML formateado.
     * @return String con el XML crudo de respuesta devuelto por el servidor gubernamental.
     */
    String sendSoapRequest(String endpointUrl, String soapAction, String xmlPayload);
}
