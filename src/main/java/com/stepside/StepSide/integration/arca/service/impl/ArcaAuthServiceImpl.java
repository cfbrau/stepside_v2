package com.stepside.StepSide.integration.arca.service.impl;

import com.stepside.StepSide.integration.arca.client.ArcaSoapClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateHolder;
import org.bouncycastle.cms.CMSProcessableByteArray;
import org.bouncycastle.cms.CMSSignedData;
import org.bouncycastle.cms.CMSSignedDataGenerator;
import org.bouncycastle.cms.CMSTypedData;
import org.bouncycastle.cms.jcajce.JcaSignerInfoGeneratorBuilder;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.operator.jcajce.JcaDigestCalculatorProviderBuilder;
import org.bouncycastle.util.Store;
import org.bouncycastle.util.encoders.Base64;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.Security;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class ArcaAuthServiceImpl {

    private final ArcaSoapClient arcaSoapClient;

    @Value("${stepside.arca.service-url-logprodd}")
    private String wsaaUrlProduccion;

    @Value("${stepside.arca.cuit-representada}")
    private String cuitRepresentada;

    @Value("${stepside.arca.certificado-string:}")
    private String certificadoString;

    @Value("${stepside.arca.clave-string:}")
    private String claveString;

    static {
        if (Security.getProvider("BC") == null) {
            Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
        }
    }

    public String obtenerTicketAccesoProduccion() {
        log.info("[ARCA PRODUCCIÓN] Solicitando Ticket de Acceso al WSAA real: {}", wsaaUrlProduccion);
        try {
            String xmlLoginTicket = buildLoginTicketXml();
            CMSSignedData signedData = generateCmsSignedData(xmlLoginTicket);
            String cmsBase64 = new String(Base64.encode(signedData.getEncoded()), StandardCharsets.UTF_8).trim();

            String wsaaSoapEnvelope =
                    "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:wsaa=\"http://wsaa.view.sua.dvadac.desein.afip.gov\">\n" +
                            "   <soapenv:Header/>\n" +
                            "   <soapenv:Body>\n" +
                            "      <wsaa:loginCms>\n" +
                            "         <wsaa:in0>" + cmsBase64 + "</wsaa:in0>\n" +
                            "      </wsaa:loginCms>\n" +
                            "   </soapenv:Body>\n" +
                            "</soapenv:Envelope>";

            // Usamos tu cliente SOAP con el parámetro de acción vacío estándar que te funcionaba
            return arcaSoapClient.sendSoapRequest(this.wsaaUrlProduccion, "", wsaaSoapEnvelope);

        } catch (Exception e) {
            log.error("[ARCA PRODUCCIÓN CRITICAL] Error al solicitar credenciales reales: ", e);
            throw new IllegalStateException("Falla de autenticación en Producción: " + e.getMessage());
        }
    }

    public String consultarPersonaRealA13(String tokenRealARCA, String signRealARCA, String cuitObjetivo) {
        // CORREGIDO: El namespace se ata ÚNICAMENTE al tag getPersona con el prefijo a13.
        // Los elementos hijos (token, sign, etc.) quedan con namespace vacío como exige ARCA.
        return "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:a13=\"http://a13.soap.ws.server.puc.sr/\">\n" +
                "   <soapenv:Header/>\n" +
                "   <soapenv:Body>\n" +
                "      <a13:getPersona>\n" +
                "         <token>" + tokenRealARCA.trim() + "</token>\n" +
                "         <sign>" + signRealARCA.trim() + "</sign>\n" +
                "         <cuitRepresentada>" + this.cuitRepresentada.trim() + "</cuitRepresentada>\n" +
                "         <idPersona>" + cuitObjetivo.trim() + "</idPersona>\n" +
                "      </a13:getPersona>\n" +
                "   </soapenv:Body>\n" +
                "</soapenv:Envelope>";
    }

    private String buildLoginTicketXml() {
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

        String signerDN = "serialNumber=CUIT " + this.cuitRepresentada.trim() + ",cn=consulta_cuit";
        String dstDN = "cn=wsaa,o=afip,c=ar,serialNumber=CUIT 33693450239";

        return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
                "<loginTicketRequest version=\"1.0\">\n" +
                "  <header>\n" +
                "    <source>" + signerDN + "</source>\n" +
                "    <destination>" + dstDN + "</destination>\n" +
                "    <uniqueId>" + (System.currentTimeMillis() / 1000) + "</uniqueId>\n" +
                "    <generationTime>" + now.minusMinutes(5).format(formatter) + "-03:00</generationTime>\n" +
                "    <expirationTime>" + now.plusHours(2).format(formatter) + "-03:00</expirationTime>\n" +
                "  </header>\n" +
                "  <service>ws_sr_padron_a13</service>\n" +
                "</loginTicketRequest>";
    }

    private CMSSignedData generateCmsSignedData(String payload) throws Exception {
        X509Certificate certificate;

        // Decodificamos directo a binario el String de las propiedades (Local o Cloud)
        byte[] certBytes = Base64.decode(certificadoString.trim().replaceAll("\\s+", ""));
        try (InputStream certIn = new ByteArrayInputStream(certBytes)) {
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            certificate = (X509Certificate) cf.generateCertificate(certIn);
        }

        // Procesamos la clave privada directo desde la variable en una línea
        String limpiaClave = claveString.trim().replaceAll("\\s+", "");
        byte[] decodedKey = Base64.decode(limpiaClave);

        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(decodedKey);
        KeyFactory kf = KeyFactory.getInstance("RSA");
        PrivateKey privateKey = kf.generatePrivate(keySpec);

        CMSSignedDataGenerator signedDataGenerator = new CMSSignedDataGenerator();
        ContentSigner sha256Signer = new JcaContentSignerBuilder("SHA256withRSA").setProvider("BC").build(privateKey);
        X509CertificateHolder certHolder = new JcaX509CertificateHolder(certificate);

        signedDataGenerator.addSignerInfoGenerator(
                new JcaSignerInfoGeneratorBuilder(
                        new JcaDigestCalculatorProviderBuilder().setProvider("BC").build()
                ).build(sha256Signer, certHolder)
        );

        List<X509CertificateHolder> certList = new ArrayList<>();
        certList.add(certHolder);
        Store<?> certs = new org.bouncycastle.util.CollectionStore<>(certList);
        signedDataGenerator.addCertificates(certs);

        CMSTypedData contentData = new CMSProcessableByteArray(payload.getBytes(StandardCharsets.UTF_8));
        return signedDataGenerator.generate(contentData, true);
    }

}
