/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
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
 * Configures the dual-port Tomcat setup for the CA service.
 *
 * <p>The CA service must serve two distinct TLS profiles simultaneously:
 * <ul>
 *   <li>Port 8082 — one-way TLS, serves {@code /ca/enroll} and {@code /ca/ca.crt}</li>
 *   <li>Port 8081 — mutual TLS (client-auth=required), serves {@code /ca/sign-cert}</li>
 * </ul>
 *
 * <p>The {@link CertificateAuthority} constructor dependency ensures the CA keystore
 * and truststore files exist on disk before Tomcat attempts to configure SSL.
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class DualPortConfiguration {

    /**
     * Dependency on {@link CertificateAuthority} guarantees that
     * {@link CertificateAuthorityConfig#certificateAuthority} runs first,
     * creating the PKCS12 keystore and truststore before Tomcat starts SSL.
     */
    private final CertificateAuthority certificateAuthority;

    private static final String KEYSTORE_TYPE = "PKCS12";
    private static final String HTTPS_SCHEME = "https";
    private static final String HTTP_1_1_PROTOCOL = "HTTP/1.1";

    @Bean
    public WebServerFactoryCustomizer<TomcatServletWebServerFactory> dualPortCustomizer(
            @Value("${ca.mtls.port:8081}") int mtlsPort,
            @Value("${ca.enrollment.port:8082}") int enrollmentPort,
            @Value("${ca.cert-path}") String certPath,
            @Value("${CA_KEYSTORE_PASS}") String keystorePass) {

        return factory -> {
            factory.setPort(enrollmentPort);

            factory.addConnectorCustomizers(connector ->
                    configureOneWayTls(connector, certPath, keystorePass));

            factory.addAdditionalConnectors(
                    buildMtlsConnector(mtlsPort, certPath, keystorePass));

            log.info("Dual-port SSL configured: enrollment={}  mTLS={}", enrollmentPort, mtlsPort);
        };
    }

    private void configureOneWayTls(Connector connector, String certPath, String keystorePass) {
        connector.setScheme(HTTPS_SCHEME);
        connector.setSecure(true);

        Http11NioProtocol protocol = (Http11NioProtocol) connector.getProtocolHandler();
        protocol.setSSLEnabled(true);

        SSLHostConfig sslHostConfig = buildSslHostConfig(
                certPath, keystorePass, SSLHostConfig.CertificateVerification.NONE);
        protocol.addSslHostConfig(sslHostConfig);
    }

    private Connector buildMtlsConnector(int port, String certPath, String keystorePass) {
        Connector connector = new Connector(HTTP_1_1_PROTOCOL);
        connector.setPort(port);
        connector.setScheme(HTTPS_SCHEME);
        connector.setSecure(true);

        Http11NioProtocol protocol = (Http11NioProtocol) connector.getProtocolHandler();
        protocol.setSSLEnabled(true);

        SSLHostConfig sslHostConfig = buildSslHostConfig(
                certPath, keystorePass, SSLHostConfig.CertificateVerification.REQUIRED);
        protocol.addSslHostConfig(sslHostConfig);

        return connector;
    }

    private SSLHostConfig buildSslHostConfig(String certPath, String keystorePass,
                                             SSLHostConfig.CertificateVerification clientAuth) {
        SSLHostConfig sslHostConfig = new SSLHostConfig();
        sslHostConfig.setCertificateVerification(clientAuth.name().toLowerCase());

        if (clientAuth == SSLHostConfig.CertificateVerification.REQUIRED) {
            sslHostConfig.setTruststoreFile(certPath + "/truststore.p12");
            sslHostConfig.setTruststorePassword(keystorePass);
            sslHostConfig.setTruststoreType(KEYSTORE_TYPE);
        }

        SSLHostConfigCertificate certificate = new SSLHostConfigCertificate(
                sslHostConfig, SSLHostConfigCertificate.Type.EC);
        certificate.setCertificateKeystoreFile(certPath + "/ca-keystore.p12");
        certificate.setCertificateKeystorePassword(keystorePass);
        certificate.setCertificateKeystoreType(KEYSTORE_TYPE);
        certificate.setCertificateKeyAlias(CertificateAuthority.CA_KEY_ALIAS);

        sslHostConfig.addCertificate(certificate);
        return sslHostConfig;
    }
}
