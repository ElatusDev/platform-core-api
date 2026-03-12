/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.passkey.config;

import com.akademiaplus.passkey.interfaceadapters.PasskeyCredentialRepositoryAdapter;
import com.yubico.webauthn.RelyingParty;
import com.yubico.webauthn.data.RelyingPartyIdentity;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashSet;

/**
 * Spring configuration for the WebAuthn Relying Party.
 *
 * <p>Creates the {@link RelyingParty} bean using the configured RP identity,
 * allowed origins, and credential repository adapter.</p>
 *
 * <p>Only activates when {@code security.passkey.rp-id} is configured,
 * allowing standalone services that depend on the security module to start
 * without passkey infrastructure.</p>
 *
 * @author ElatusDev
 * @since 1.0
 */
@Configuration
@ConditionalOnProperty(prefix = "security.passkey", name = "rp-id")
@EnableConfigurationProperties(PasskeyProperties.class)
public class RelyingPartyConfiguration {

    /**
     * Creates the WebAuthn RelyingParty bean.
     *
     * @param properties          passkey configuration properties
     * @param credentialRepository the credential repository adapter
     * @return configured RelyingParty instance
     */
    @Bean
    public RelyingParty relyingParty(PasskeyProperties properties,
                                      PasskeyCredentialRepositoryAdapter credentialRepository) {
        RelyingPartyIdentity rpIdentity = RelyingPartyIdentity.builder()
                .id(properties.getRpId())
                .name(properties.getRpName())
                .build();

        return RelyingParty.builder()
                .identity(rpIdentity)
                .credentialRepository(credentialRepository)
                .origins(new HashSet<>(properties.getAllowedOrigins()))
                .build();
    }
}
