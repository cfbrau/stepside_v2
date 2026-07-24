package com.stepside.StepSide.integration.arca.client.impl;

import com.stepside.StepSide.integration.arca.client.ArcaSoapClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import java.nio.charset.StandardCharsets;

@Component
@Slf4j
public class ArcaSoapClientImpl implements ArcaSoapClient {

    private final WebClient webClient;

    public ArcaSoapClientImpl(WebClient.Builder webClientBuilder) {
        WebClient.Builder builderLocal = webClientBuilder;

        try {
            // 1. Configuramos el contexto SSL que confía en los certificados de AFIP Homologación
            io.netty.handler.ssl.SslContext sslContext = io.netty.handler.ssl.SslContextBuilder.forClient()
                    .trustManager(io.netty.handler.ssl.util.InsecureTrustManagerFactory.INSTANCE)
                    .build();

            // 2. Aplicamos la especificación segura de Reactor Netty moderno para Spring Boot 3.4.x
            reactor.netty.http.client.HttpClient httpClient = reactor.netty.http.client.HttpClient.create()
                    .secure(sslContextSpec -> sslContextSpec.sslContext(sslContext));

            builderLocal = builderLocal
                    .clientConnector(new org.springframework.http.client.reactive.ReactorClientHttpConnector(httpClient));

        } catch (Exception e) {
            log.error("[ARCA INFR] Error al inicializar el blindaje SSL del cliente SOAP", e);
        }

        // CORRECTO: La variable final se asigna UNA SOLA VEZ al final del flujo del constructor
        this.webClient = builderLocal
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(10 * 1024 * 1024))
                .build();
    }


    @Override
    public String sendSoapRequest(String endpointUrl, String soapAction, String xmlPayload) {

        try {
            // Transformación mandatoria a bytes para blindar el texto de mutaciones de Spring
            byte[] bytesPayload = xmlPayload.getBytes(StandardCharsets.UTF_8);

            return webClient.post()
                    .uri(endpointUrl)
                    .header("Content-Type", "text/xml;charset=UTF-8")
                    .header("SOAPAction", "\"" + soapAction + "\"")
                    .bodyValue(bytesPayload)
                    .retrieve()
                    .onStatus(statusCode -> statusCode.isError(), clientResponse ->
                            clientResponse.bodyToMono(String.class).flatMap(errorBody -> {
                                log.error("[ARCA ERROR INTERNO AFIP] HTTP {}: {}", clientResponse.statusCode(), errorBody);
                                return Mono.error(new IllegalArgumentException(errorBody));
                            })
                    )
                    .bodyToMono(String.class)
                    .block();
        } catch (Exception e) {
            log.error("[ARCA CRITICAL] Error de socket en transmisión SOAP: ", e);
            throw new IllegalStateException(e.getMessage());
        }
    }
}
