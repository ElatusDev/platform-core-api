/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.oauth.usecases;

import com.akademiaplus.customer.adultstudent.interfaceadapters.AdultStudentRepository;
import com.akademiaplus.infra.persistence.config.TenantContextHolder;
import com.akademiaplus.interfaceadapters.PersonPIIRepository;
import com.akademiaplus.internal.interfaceadapters.jwt.JwtTokenProvider;
import com.akademiaplus.internal.usecases.domain.LoginResult;
import com.akademiaplus.oauth.exceptions.OAuthProviderException;
import com.akademiaplus.oauth.exceptions.UnsupportedProviderException;
import com.akademiaplus.oauth.interfaceadapters.OAuthProviderClient;
import com.akademiaplus.oauth.usecases.domain.OAuthUserProfile;
import com.akademiaplus.security.CustomerAuthDataModel;
import com.akademiaplus.users.base.PersonPIIDataModel;
import com.akademiaplus.users.customer.AdultStudentDataModel;
import com.akademiaplus.utilities.security.HashingService;
import com.akademiaplus.utilities.security.PiiNormalizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Cross-module orchestrator for OAuth2 social login authentication.
 *
 * <p>Validates the provider, exchanges the authorization code for a user
 * profile, looks up or creates a platform user by email, and issues a JWT.</p>
 *
 * @author ElatusDev
 * @since 1.0
 */
@Service
public class OAuthAuthenticationUseCase {

    private static final Logger LOG = LoggerFactory.getLogger(OAuthAuthenticationUseCase.class);

    /** Supported OAuth provider names. */
    public static final Set<String> SUPPORTED_PROVIDERS = Set.of("google", "facebook");

    /** JWT claim key for user role. */
    public static final String JWT_CLAIM_ROLE = "Has role";

    /** Role assigned to OAuth users. */
    public static final String ROLE_CUSTOMER = "CUSTOMER";

    /** Placeholder for fields on auto-created accounts. */
    public static final String PLACEHOLDER = "PENDING_UPDATE";

    private final Map<String, OAuthProviderClient> providerClients;
    private final PersonPIIRepository personPIIRepository;
    private final AdultStudentRepository adultStudentRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final HashingService hashingService;
    private final PiiNormalizer piiNormalizer;
    private final TenantContextHolder tenantContextHolder;
    private final ApplicationContext applicationContext;

    /**
     * Constructs the use case with all required dependencies.
     *
     * @param providerClients       map of provider name to client implementation
     * @param personPIIRepository   the person PII repository
     * @param adultStudentRepository the adult student repository
     * @param jwtTokenProvider      the JWT token provider
     * @param hashingService        the hashing service
     * @param piiNormalizer         the PII normalizer
     * @param tenantContextHolder   the tenant context holder
     * @param applicationContext    the Spring application context
     */
    public OAuthAuthenticationUseCase(Map<String, OAuthProviderClient> providerClients,
                                      PersonPIIRepository personPIIRepository,
                                      AdultStudentRepository adultStudentRepository,
                                      JwtTokenProvider jwtTokenProvider,
                                      HashingService hashingService,
                                      PiiNormalizer piiNormalizer,
                                      TenantContextHolder tenantContextHolder,
                                      ApplicationContext applicationContext) {
        this.providerClients = providerClients;
        this.personPIIRepository = personPIIRepository;
        this.adultStudentRepository = adultStudentRepository;
        this.jwtTokenProvider = jwtTokenProvider;
        this.hashingService = hashingService;
        this.piiNormalizer = piiNormalizer;
        this.tenantContextHolder = tenantContextHolder;
        this.applicationContext = applicationContext;
    }

    /**
     * Authenticates a user via OAuth2 social login.
     *
     * @param provider          the OAuth provider name (e.g., "google", "facebook")
     * @param authorizationCode the authorization code from the provider consent flow
     * @param redirectUri       the redirect URI used during the consent flow
     * @param tenantId          the tenant ID
     * @return the login result with access and refresh tokens
     * @throws UnsupportedProviderException if the provider is not supported
     * @throws OAuthProviderException       if the code exchange or profile fetch fails
     */
    @Transactional
    public LoginResult loginWithOAuth(String provider, String authorizationCode,
                                       String redirectUri, Long tenantId) {
        String normalizedProvider = provider.toLowerCase();

        if (!SUPPORTED_PROVIDERS.contains(normalizedProvider)) {
            throw new UnsupportedProviderException(provider);
        }

        tenantContextHolder.setTenantId(tenantId);

        OAuthProviderClient client = providerClients.get(normalizedProvider);
        if (client == null) {
            throw new UnsupportedProviderException(provider);
        }

        OAuthUserProfile profile = client.exchangeCodeForProfile(authorizationCode, redirectUri);

        String normalizedEmail = piiNormalizer.normalizeEmail(profile.email());
        String emailHash = hashingService.generateHash(normalizedEmail);

        return personPIIRepository.findByEmailHash(emailHash)
                .map(pii -> loginExistingUser(normalizedEmail, tenantId))
                .orElseGet(() -> createAndLoginNewUser(normalizedEmail, emailHash, profile, normalizedProvider, tenantId));
    }

    private LoginResult loginExistingUser(String email, Long tenantId) {
        LOG.info("OAuth login for existing user: {}", email);
        Map<String, Object> claims = Map.of(JWT_CLAIM_ROLE, ROLE_CUSTOMER);
        String accessToken = jwtTokenProvider.createAccessToken(email, tenantId, claims);
        String refreshToken = jwtTokenProvider.createRefreshToken(email, tenantId, UUID.randomUUID().toString());
        return new LoginResult(accessToken, refreshToken, email);
    }

    private LoginResult createAndLoginNewUser(String normalizedEmail, String emailHash,
                                                OAuthUserProfile profile, String provider,
                                                Long tenantId) {
        LOG.info("OAuth login creating new user for: {}", normalizedEmail);

        PersonPIIDataModel pii = applicationContext.getBean(PersonPIIDataModel.class);
        pii.setTenantId(tenantId);
        pii.setEmail(normalizedEmail);
        pii.setEmailHash(emailHash);
        pii.setFirstName(profile.name() != null ? extractFirstName(profile.name()) : extractNameFromEmail(normalizedEmail));
        pii.setLastName(profile.name() != null ? extractLastName(profile.name()) : PLACEHOLDER);
        pii.setPhoneNumber(PLACEHOLDER);
        pii.setPhoneHash(hashingService.generateHash(PLACEHOLDER));
        pii.setAddress(PLACEHOLDER);
        pii.setZipCode(PLACEHOLDER);

        CustomerAuthDataModel customerAuth = applicationContext.getBean(CustomerAuthDataModel.class);
        customerAuth.setTenantId(tenantId);
        customerAuth.setProvider(provider);
        customerAuth.setToken(profile.providerUserId());

        AdultStudentDataModel student = applicationContext.getBean(AdultStudentDataModel.class);
        student.setTenantId(tenantId);
        student.setPersonPII(pii);
        student.setCustomerAuth(customerAuth);
        student.setBirthDate(LocalDate.of(2000, 1, 1));
        student.setEntryDate(LocalDate.now());

        adultStudentRepository.save(student);

        Map<String, Object> claims = Map.of(JWT_CLAIM_ROLE, ROLE_CUSTOMER);
        String accessToken = jwtTokenProvider.createAccessToken(normalizedEmail, tenantId, claims);
        String refreshToken = jwtTokenProvider.createRefreshToken(normalizedEmail, tenantId, UUID.randomUUID().toString());
        return new LoginResult(accessToken, refreshToken, normalizedEmail);
    }

    private String extractNameFromEmail(String email) {
        int atIndex = email.indexOf('@');
        return atIndex > 0 ? email.substring(0, atIndex) : email;
    }

    private String extractFirstName(String fullName) {
        int spaceIndex = fullName.indexOf(' ');
        return spaceIndex > 0 ? fullName.substring(0, spaceIndex) : fullName;
    }

    private String extractLastName(String fullName) {
        int spaceIndex = fullName.indexOf(' ');
        return spaceIndex > 0 ? fullName.substring(spaceIndex + 1) : PLACEHOLDER;
    }
}
