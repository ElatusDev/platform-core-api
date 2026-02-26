/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 */
package com.akademiaplus.interfaceadapters.config;

import com.akademiaplus.usecases.domain.CertificateAuthority;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.catalina.connector.Connector;
import org.apache.coyote.http11.Http11NioProtocol;
import org.apache.tomcat.util.net.SSLHostConfig;
import org.apache.tomcat.util.net.SSLHostConfigCertificate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.tomcat.servlet.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Adds the mTLS connector (port 8081) to Tomcat for the CA service.
 *
 * <p>The primary enrollment port (8082) is owned entirely by Spring Boot via
 * {@code server.ssl.*} JVM -D args passed from the Docker entrypoint after the
 * CA keystore is written to disk — the same pattern used by all other services.
 *
 * <p>Manual SSL-on-primary was removed to eliminate the Boot 4.0.0-M3 race where
 * autoconfiguration and addConnectorCustomizers both configure the default connector,
 * with autoconfiguration referencing a stale default keystore path.
 *
 * <p>{@link CertificateAuthority} dependency guarantees the keystore and truststore
 * exist on disk before Tomcat attempts to bind either SSL port.
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class DualPortConfiguration {

    private final CertificateAuthority certificateAuthority;

    private static final String KEYSTORE_TYPE = "PKCS12";
    private static final String HTTPS_SCHEME  = "https";
    private static final String HTTP_1_1      = "HTTP/1.1";

    @Bean
    public WebServerFactoryCustomizer<TomcatServletWebServerFactory> mtlsConnectorCustomizer(
            @Value("${ca.mtls.port:8081}") int    mtlsPort,
            @Value("${ca.cert-path}")      String certPath,
            @Value("${CA_KEYSTORE_PASS}")  String keystorePass) {

        return factory -> {
            factory.addAdditionalConnectors(buildMtlsConnector(mtlsPort, certPath, keystorePass));
            log.info("mTLS connector added on port {}", mtlsPort);
        };
    }

    private Connector buildMtlsConnector(int port, String certPath, String keystorePass) {
        Connector connector = new Connector(HTTP_1_1);
        connector.setPort(port);
        connector.setScheme(HTTPS_SCHEME);
        connector.setSecure(true);

        Http11NioProtocol protocol = (Http11NioProtocol) connector.getProtocolHandler();
        protocol.setSSLEnabled(true);

        SSLHostConfig sslHostConfig = new SSLHostConfig();
        sslHostConfig.setCertificateVerification("required");

        sslHostConfig.setTruststoreFile(certPath + "/akademiaplus-truststore.p12");
        sslHostConfig.setTruststorePassword(keystorePass);
        sslHostConfig.setTruststoreType(KEYSTORE_TYPE);

        SSLHostConfigCertificate cert = new SSLHostConfigCertificate(
                sslHostConfig, SSLHostConfigCertificate.Type.EC);
        cert.setCertificateKeystoreFile(certPath + "/akademiaplus-ca-keystore.p12");
        cert.setCertificateKeystorePassword(keystorePass);
        cert.setCertificateKeystoreType(KEYSTORE_TYPE);
        cert.setCertificateKeyAlias(CertificateAuthority.CA_KEY_ALIAS);

        sslHostConfig.addCertificate(cert);
        protocol.addSslHostConfig(sslHostConfig);

        return connector;
    }
}
