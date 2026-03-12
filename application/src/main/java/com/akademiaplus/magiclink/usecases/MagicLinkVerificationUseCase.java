/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.magiclink.usecases;

import com.akademiaplus.customer.adultstudent.interfaceadapters.AdultStudentRepository;
import com.akademiaplus.customer.interfaceadapters.TutorRepository;
import com.akademiaplus.infra.persistence.config.TenantContextHolder;
import com.akademiaplus.interfaceadapters.PersonPIIRepository;
import com.akademiaplus.internal.interfaceadapters.jwt.JwtTokenProvider;
import com.akademiaplus.internal.usecases.domain.LoginResult;
import com.akademiaplus.magiclink.exceptions.MagicLinkTokenAlreadyUsedException;
import com.akademiaplus.magiclink.exceptions.MagicLinkTokenExpiredException;
import com.akademiaplus.magiclink.exceptions.MagicLinkTokenNotFoundException;
import com.akademiaplus.magiclink.interfaceadapters.MagicLinkTokenRepository;
import com.akademiaplus.security.CustomerAuthDataModel;
import com.akademiaplus.security.MagicLinkTokenDataModel;
import com.akademiaplus.users.base.PersonPIIDataModel;
import com.akademiaplus.users.customer.AdultStudentDataModel;
import com.akademiaplus.utilities.security.HashingService;
import com.akademiaplus.utilities.security.PiiNormalizer;
import openapi.akademiaplus.domain.security.dto.MagicLinkVerifyRequestDTO;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

/**
 * Cross-module orchestrator for magic link token verification.
 *
 * <p>Validates the token hash, checks expiry and single-use constraints,
 * looks up or creates a user by email, and issues a platform JWT.</p>
 *
 * @author ElatusDev
 * @since 1.0
 */
@Service
public class MagicLinkVerificationUseCase {

    /** Placeholder for phone number on auto-created accounts. */
    public static final String PLACEHOLDER_PHONE = "PENDING_UPDATE";

    /** Placeholder for address on auto-created accounts. */
    public static final String PLACEHOLDER_ADDRESS = "PENDING_UPDATE";

    /** Placeholder for zip code on auto-created accounts. */
    public static final String PLACEHOLDER_ZIP = "PENDING_UPDATE";

    /** Placeholder for last name on auto-created accounts. */
    public static final String PLACEHOLDER_LAST_NAME = "PENDING_UPDATE";

    /** JWT claim key for user role. */
    public static final String JWT_CLAIM_ROLE = "Has role";

    /** Role assigned to magic link users. */
    public static final String ROLE_CUSTOMER = "CUSTOMER";

    /** Authentication provider identifier for magic link users. */
    public static final String PROVIDER_MAGIC_LINK = "magic-link";

    /** Token value stored for magic link CustomerAuth records. */
    public static final String AUTH_TOKEN_PLACEHOLDER = "magic-link";

    private final MagicLinkTokenRepository magicLinkTokenRepository;
    private final PersonPIIRepository personPIIRepository;
    private final AdultStudentRepository adultStudentRepository;
    private final TutorRepository tutorRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final HashingService hashingService;
    private final PiiNormalizer piiNormalizer;
    private final TenantContextHolder tenantContextHolder;
    private final ApplicationContext applicationContext;

    /**
     * Constructs the verification use case with required dependencies.
     *
     * @param magicLinkTokenRepository the token repository
     * @param personPIIRepository      the person PII repository
     * @param adultStudentRepository   the adult student repository
     * @param tutorRepository          the tutor repository
     * @param jwtTokenProvider         the JWT token provider
     * @param hashingService           the SHA-256 hashing service
     * @param piiNormalizer            the PII normalization service
     * @param tenantContextHolder      the tenant context holder
     * @param applicationContext       the Spring application context
     */
    public MagicLinkVerificationUseCase(MagicLinkTokenRepository magicLinkTokenRepository,
                                        PersonPIIRepository personPIIRepository,
                                        AdultStudentRepository adultStudentRepository,
                                        TutorRepository tutorRepository,
                                        JwtTokenProvider jwtTokenProvider,
                                        HashingService hashingService,
                                        PiiNormalizer piiNormalizer,
                                        TenantContextHolder tenantContextHolder,
                                        ApplicationContext applicationContext) {
        this.magicLinkTokenRepository = magicLinkTokenRepository;
        this.personPIIRepository = personPIIRepository;
        this.adultStudentRepository = adultStudentRepository;
        this.tutorRepository = tutorRepository;
        this.jwtTokenProvider = jwtTokenProvider;
        this.hashingService = hashingService;
        this.piiNormalizer = piiNormalizer;
        this.tenantContextHolder = tenantContextHolder;
        this.applicationContext = applicationContext;
    }

    /**
     * Verifies a magic link token and issues a platform JWT.
     *
     * <p>If the user does not exist, creates a new AdultStudent account
     * with placeholder values for profile completion later.</p>
     *
     * @param dto the verification request containing token and tenant ID
     * @return the login result with access and refresh tokens
     */
    @Transactional
    public LoginResult verifyMagicLink(MagicLinkVerifyRequestDTO dto) {
        tenantContextHolder.setTenantId(dto.getTenantId());

        MagicLinkTokenDataModel token = validateAndConsumeToken(dto);

        String normalizedEmail = piiNormalizer.normalizeEmail(token.getEmail());
        String emailHash = hashingService.generateHash(normalizedEmail);

        return personPIIRepository.findByEmailHash(emailHash)
                .map(pii -> loginExistingUser(pii, token.getEmail(), dto.getTenantId()))
                .orElseGet(() -> createAndLoginNewUser(normalizedEmail, emailHash, dto.getTenantId()));
    }

    private MagicLinkTokenDataModel validateAndConsumeToken(MagicLinkVerifyRequestDTO dto) {
        String tokenHash = hashingService.generateHash(dto.getToken());

        MagicLinkTokenDataModel token = magicLinkTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(MagicLinkTokenNotFoundException::new);

        if (token.getUsedAt() != null) {
            throw new MagicLinkTokenAlreadyUsedException();
        }
        if (Instant.now().isAfter(token.getExpiresAt())) {
            throw new MagicLinkTokenExpiredException();
        }

        token.setUsedAt(Instant.now());
        magicLinkTokenRepository.save(token);
        return token;
    }

    private LoginResult loginExistingUser(PersonPIIDataModel pii, String email, Long tenantId) {
        Map<String, Object> claims = buildCustomerClaims(pii.getPersonPiiId());
        String accessToken = jwtTokenProvider.createAccessToken(email, tenantId, claims);
        String refreshToken = jwtTokenProvider.createRefreshToken(email, tenantId,
                java.util.UUID.randomUUID().toString());
        return new LoginResult(accessToken, refreshToken, email);
    }

    private LoginResult createAndLoginNewUser(String normalizedEmail, String emailHash, Long tenantId) {
        PersonPIIDataModel pii = buildPersonPII(normalizedEmail, emailHash, tenantId);
        CustomerAuthDataModel customerAuth = buildCustomerAuth(tenantId);
        AdultStudentDataModel student = buildAdultStudent(pii, customerAuth, tenantId);

        adultStudentRepository.save(student);

        Map<String, Object> claims = new HashMap<>();
        claims.put(JWT_CLAIM_ROLE, ROLE_CUSTOMER);
        claims.put(JwtTokenProvider.PROFILE_TYPE_CLAIM, JwtTokenProvider.PROFILE_TYPE_ADULT_STUDENT);
        claims.put(JwtTokenProvider.PROFILE_ID_CLAIM, student.getAdultStudentId());

        String accessToken = jwtTokenProvider.createAccessToken(normalizedEmail, tenantId, claims);
        String refreshToken = jwtTokenProvider.createRefreshToken(normalizedEmail, tenantId,
                java.util.UUID.randomUUID().toString());
        return new LoginResult(accessToken, refreshToken, normalizedEmail);
    }

    /**
     * Builds JWT claims with profile_type and profile_id for an existing customer.
     *
     * @param personPiiId the person PII ID to resolve the profile entity
     * @return claims map with role, profile type, and profile ID
     */
    private Map<String, Object> buildCustomerClaims(Long personPiiId) {
        Map<String, Object> claims = new HashMap<>();
        claims.put(JWT_CLAIM_ROLE, ROLE_CUSTOMER);

        adultStudentRepository.findByPersonPiiId(personPiiId).ifPresentOrElse(
                student -> {
                    claims.put(JwtTokenProvider.PROFILE_TYPE_CLAIM, JwtTokenProvider.PROFILE_TYPE_ADULT_STUDENT);
                    claims.put(JwtTokenProvider.PROFILE_ID_CLAIM, student.getAdultStudentId());
                },
                () -> tutorRepository.findByPersonPiiId(personPiiId).ifPresent(tutor -> {
                    claims.put(JwtTokenProvider.PROFILE_TYPE_CLAIM, JwtTokenProvider.PROFILE_TYPE_TUTOR);
                    claims.put(JwtTokenProvider.PROFILE_ID_CLAIM, tutor.getTutorId());
                })
        );

        return claims;
    }

    private PersonPIIDataModel buildPersonPII(String email, String emailHash, Long tenantId) {
        PersonPIIDataModel pii = applicationContext.getBean(PersonPIIDataModel.class);
        pii.setTenantId(tenantId);
        pii.setEmail(email);
        pii.setEmailHash(emailHash);
        pii.setFirstName(extractNameFromEmail(email));
        pii.setLastName(PLACEHOLDER_LAST_NAME);
        pii.setPhoneNumber(PLACEHOLDER_PHONE);
        pii.setPhoneHash(hashingService.generateHash(PLACEHOLDER_PHONE));
        pii.setAddress(PLACEHOLDER_ADDRESS);
        pii.setZipCode(PLACEHOLDER_ZIP);
        return pii;
    }

    private CustomerAuthDataModel buildCustomerAuth(Long tenantId) {
        CustomerAuthDataModel auth = applicationContext.getBean(CustomerAuthDataModel.class);
        auth.setTenantId(tenantId);
        auth.setProvider(PROVIDER_MAGIC_LINK);
        auth.setToken(AUTH_TOKEN_PLACEHOLDER);
        return auth;
    }

    private AdultStudentDataModel buildAdultStudent(PersonPIIDataModel pii,
                                                     CustomerAuthDataModel customerAuth,
                                                     Long tenantId) {
        AdultStudentDataModel student = applicationContext.getBean(AdultStudentDataModel.class);
        student.setTenantId(tenantId);
        student.setPersonPII(pii);
        student.setCustomerAuth(customerAuth);
        student.setBirthDate(LocalDate.of(2000, 1, 1));
        student.setEntryDate(LocalDate.now());
        return student;
    }

    private String extractNameFromEmail(String email) {
        int atIndex = email.indexOf('@');
        if (atIndex > 0) {
            return email.substring(0, atIndex);
        }
        return email;
    }
}
