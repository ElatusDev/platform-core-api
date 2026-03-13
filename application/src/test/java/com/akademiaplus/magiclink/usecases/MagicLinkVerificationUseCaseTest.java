/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.magiclink.usecases;

import com.akademiaplus.customer.adultstudent.interfaceadapters.AdultStudentRepository;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationContext;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

import org.mockito.InOrder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link MagicLinkVerificationUseCase}.
 *
 * @author ElatusDev
 * @since 1.0
 */
@DisplayName("MagicLinkVerificationUseCase")
@ExtendWith(MockitoExtension.class)
class MagicLinkVerificationUseCaseTest {

    private static final String TEST_RAW_TOKEN = "dGVzdC10b2tlbi12YWx1ZS1mb3ItdW5pdC10ZXN0cw";
    private static final String TEST_TOKEN_HASH = "a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2";
    private static final String TEST_EMAIL = "user@example.com";
    private static final String TEST_NORMALIZED_EMAIL = "user@example.com";
    private static final String TEST_EMAIL_HASH = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855";
    private static final Long TEST_TENANT_ID = 1L;
    private static final String TEST_ACCESS_TOKEN = "jwt-access-token";
    private static final String TEST_REFRESH_TOKEN = "jwt-refresh-token";
    private static final String TEST_PHONE_HASH = "phone-hash-value";

    @Mock private MagicLinkTokenRepository magicLinkTokenRepository;
    @Mock private PersonPIIRepository personPIIRepository;
    @Mock private AdultStudentRepository adultStudentRepository;
    @Mock private com.akademiaplus.customer.interfaceadapters.TutorRepository tutorRepository;
    @Mock private JwtTokenProvider jwtTokenProvider;
    @Mock private HashingService hashingService;
    @Mock private PiiNormalizer piiNormalizer;
    @Mock private TenantContextHolder tenantContextHolder;
    @Mock private ApplicationContext applicationContext;

    @Captor private ArgumentCaptor<MagicLinkTokenDataModel> tokenCaptor;
    @Captor private ArgumentCaptor<AdultStudentDataModel> studentCaptor;

    private MagicLinkVerificationUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new MagicLinkVerificationUseCase(
                magicLinkTokenRepository, personPIIRepository, adultStudentRepository,
                tutorRepository, jwtTokenProvider, hashingService, piiNormalizer,
                tenantContextHolder, applicationContext);
    }

    private MagicLinkVerifyRequestDTO buildVerifyRequest() {
        return new MagicLinkVerifyRequestDTO(TEST_RAW_TOKEN, TEST_TENANT_ID);
    }

    private MagicLinkTokenDataModel buildValidToken() {
        MagicLinkTokenDataModel token = new MagicLinkTokenDataModel();
        token.setTenantId(TEST_TENANT_ID);
        token.setEmail(TEST_EMAIL);
        token.setTokenHash(TEST_TOKEN_HASH);
        token.setExpiresAt(Instant.now().plusSeconds(600));
        token.setUsedAt(null);
        return token;
    }

    private void stubTokenLookup(MagicLinkTokenDataModel token) {
        when(hashingService.generateHash(TEST_RAW_TOKEN)).thenReturn(TEST_TOKEN_HASH);
        when(magicLinkTokenRepository.findByTokenHash(TEST_TOKEN_HASH)).thenReturn(Optional.of(token));
    }

    private void stubEmailNormalization() {
        when(piiNormalizer.normalizeEmail(TEST_EMAIL)).thenReturn(TEST_NORMALIZED_EMAIL);
        when(hashingService.generateHash(TEST_NORMALIZED_EMAIL)).thenReturn(TEST_EMAIL_HASH);
    }

    @Nested
    @DisplayName("Token Validation")
    class TokenValidation {

        @Test
        @DisplayName("Should throw MagicLinkTokenNotFoundException when token hash not found")
        void shouldThrowTokenNotFound_whenTokenHashNotFound() {
            // Given
            when(hashingService.generateHash(TEST_RAW_TOKEN)).thenReturn(TEST_TOKEN_HASH);
            when(magicLinkTokenRepository.findByTokenHash(TEST_TOKEN_HASH)).thenReturn(Optional.empty());
            MagicLinkVerifyRequestDTO dto = buildVerifyRequest();

            // When / Then
            assertThatThrownBy(() -> useCase.verifyMagicLink(dto))
                    .isInstanceOf(MagicLinkTokenNotFoundException.class);

            InOrder inOrder = inOrder(tenantContextHolder, hashingService, magicLinkTokenRepository);
            inOrder.verify(tenantContextHolder, times(1)).setTenantId(TEST_TENANT_ID);
            inOrder.verify(hashingService, times(1)).generateHash(TEST_RAW_TOKEN);
            inOrder.verify(magicLinkTokenRepository, times(1)).findByTokenHash(TEST_TOKEN_HASH);
            inOrder.verifyNoMoreInteractions();
            verifyNoInteractions(piiNormalizer, personPIIRepository, adultStudentRepository,
                    jwtTokenProvider, applicationContext);
        }

        @Test
        @DisplayName("Should throw MagicLinkTokenAlreadyUsedException when token already used")
        void shouldThrowTokenAlreadyUsed_whenTokenAlreadyUsed() {
            // Given
            MagicLinkTokenDataModel token = buildValidToken();
            token.setUsedAt(Instant.now().minusSeconds(60));
            stubTokenLookup(token);
            MagicLinkVerifyRequestDTO dto = buildVerifyRequest();

            // When / Then
            assertThatThrownBy(() -> useCase.verifyMagicLink(dto))
                    .isInstanceOf(MagicLinkTokenAlreadyUsedException.class);

            InOrder inOrder = inOrder(tenantContextHolder, hashingService, magicLinkTokenRepository);
            inOrder.verify(tenantContextHolder, times(1)).setTenantId(TEST_TENANT_ID);
            inOrder.verify(hashingService, times(1)).generateHash(TEST_RAW_TOKEN);
            inOrder.verify(magicLinkTokenRepository, times(1)).findByTokenHash(TEST_TOKEN_HASH);
            inOrder.verifyNoMoreInteractions();
            verifyNoInteractions(piiNormalizer, personPIIRepository, adultStudentRepository,
                    jwtTokenProvider, applicationContext);
        }

        @Test
        @DisplayName("Should throw MagicLinkTokenExpiredException when token expired")
        void shouldThrowTokenExpired_whenTokenExpired() {
            // Given
            MagicLinkTokenDataModel token = buildValidToken();
            token.setExpiresAt(Instant.now().minusSeconds(60));
            stubTokenLookup(token);
            MagicLinkVerifyRequestDTO dto = buildVerifyRequest();

            // When / Then
            assertThatThrownBy(() -> useCase.verifyMagicLink(dto))
                    .isInstanceOf(MagicLinkTokenExpiredException.class);

            InOrder inOrder = inOrder(tenantContextHolder, hashingService, magicLinkTokenRepository);
            inOrder.verify(tenantContextHolder, times(1)).setTenantId(TEST_TENANT_ID);
            inOrder.verify(hashingService, times(1)).generateHash(TEST_RAW_TOKEN);
            inOrder.verify(magicLinkTokenRepository, times(1)).findByTokenHash(TEST_TOKEN_HASH);
            inOrder.verifyNoMoreInteractions();
            verifyNoInteractions(piiNormalizer, personPIIRepository, adultStudentRepository,
                    jwtTokenProvider, applicationContext);
        }

        @Test
        @DisplayName("Should mark token as used when token is valid")
        void shouldMarkTokenAsUsed_whenTokenIsValid() {
            // Given
            MagicLinkTokenDataModel token = buildValidToken();
            stubTokenLookup(token);
            stubEmailNormalization();
            when(personPIIRepository.findByEmailHash(TEST_EMAIL_HASH)).thenReturn(Optional.empty());
            stubNewUserCreation();
            stubJwtCreation(TEST_NORMALIZED_EMAIL);
            MagicLinkVerifyRequestDTO dto = buildVerifyRequest();

            // When
            LoginResult result = useCase.verifyMagicLink(dto);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.accessToken()).isEqualTo(TEST_ACCESS_TOKEN);

            InOrder inOrder = inOrder(tenantContextHolder, hashingService, magicLinkTokenRepository,
                    piiNormalizer, personPIIRepository, applicationContext, adultStudentRepository, jwtTokenProvider);
            inOrder.verify(tenantContextHolder, times(1)).setTenantId(TEST_TENANT_ID);
            inOrder.verify(hashingService, times(1)).generateHash(TEST_RAW_TOKEN);
            inOrder.verify(magicLinkTokenRepository, times(1)).findByTokenHash(TEST_TOKEN_HASH);
            inOrder.verify(magicLinkTokenRepository, times(1)).save(tokenCaptor.capture());
            assertThat(tokenCaptor.getValue().getUsedAt()).isNotNull();
            inOrder.verify(piiNormalizer, times(1)).normalizeEmail(TEST_EMAIL);
            inOrder.verify(hashingService, times(1)).generateHash(TEST_NORMALIZED_EMAIL);
            inOrder.verify(personPIIRepository, times(1)).findByEmailHash(TEST_EMAIL_HASH);
            inOrder.verify(applicationContext, times(1)).getBean(PersonPIIDataModel.class);
            inOrder.verify(hashingService, times(1)).generateHash(MagicLinkVerificationUseCase.PLACEHOLDER_PHONE);
            inOrder.verify(applicationContext, times(1)).getBean(CustomerAuthDataModel.class);
            inOrder.verify(applicationContext, times(1)).getBean(AdultStudentDataModel.class);
            inOrder.verify(adultStudentRepository, times(1)).save(studentCaptor.capture());
            inOrder.verify(jwtTokenProvider, times(1)).createAccessToken(
                    eq(TEST_NORMALIZED_EMAIL), eq(TEST_TENANT_ID), any(Map.class));
            inOrder.verify(jwtTokenProvider, times(1)).createRefreshToken(
                    eq(TEST_NORMALIZED_EMAIL), eq(TEST_TENANT_ID),
                    argThat(familyId -> familyId != null && !familyId.isEmpty()));
            inOrder.verifyNoMoreInteractions();
        }

        @Test
        @DisplayName("Should set tenant context when verifying magic link")
        void shouldSetTenantContext_whenVerifyingMagicLink() {
            // Given
            MagicLinkTokenDataModel token = buildValidToken();
            stubTokenLookup(token);
            stubEmailNormalization();
            when(personPIIRepository.findByEmailHash(TEST_EMAIL_HASH)).thenReturn(Optional.empty());
            stubNewUserCreation();
            stubJwtCreation(TEST_NORMALIZED_EMAIL);
            MagicLinkVerifyRequestDTO dto = buildVerifyRequest();

            // When
            LoginResult result = useCase.verifyMagicLink(dto);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.accessToken()).isEqualTo(TEST_ACCESS_TOKEN);

            InOrder inOrder = inOrder(tenantContextHolder, hashingService, magicLinkTokenRepository,
                    piiNormalizer, personPIIRepository, applicationContext, adultStudentRepository, jwtTokenProvider);
            inOrder.verify(tenantContextHolder, times(1)).setTenantId(TEST_TENANT_ID);
            inOrder.verify(hashingService, times(1)).generateHash(TEST_RAW_TOKEN);
            inOrder.verify(magicLinkTokenRepository, times(1)).findByTokenHash(TEST_TOKEN_HASH);
            inOrder.verify(magicLinkTokenRepository, times(1)).save(tokenCaptor.capture());
            inOrder.verify(piiNormalizer, times(1)).normalizeEmail(TEST_EMAIL);
            inOrder.verify(hashingService, times(1)).generateHash(TEST_NORMALIZED_EMAIL);
            inOrder.verify(personPIIRepository, times(1)).findByEmailHash(TEST_EMAIL_HASH);
            inOrder.verify(applicationContext, times(1)).getBean(PersonPIIDataModel.class);
            inOrder.verify(hashingService, times(1)).generateHash(MagicLinkVerificationUseCase.PLACEHOLDER_PHONE);
            inOrder.verify(applicationContext, times(1)).getBean(CustomerAuthDataModel.class);
            inOrder.verify(applicationContext, times(1)).getBean(AdultStudentDataModel.class);
            inOrder.verify(adultStudentRepository, times(1)).save(studentCaptor.capture());
            inOrder.verify(jwtTokenProvider, times(1)).createAccessToken(
                    eq(TEST_NORMALIZED_EMAIL), eq(TEST_TENANT_ID), any(Map.class));
            inOrder.verify(jwtTokenProvider, times(1)).createRefreshToken(
                    eq(TEST_NORMALIZED_EMAIL), eq(TEST_TENANT_ID),
                    argThat(familyId -> familyId != null && !familyId.isEmpty()));
            inOrder.verifyNoMoreInteractions();
        }
    }

    @Nested
    @DisplayName("Existing User Login")
    class ExistingUserLogin {

        @Test
        @DisplayName("Should issue JWT for existing user when email hash found")
        void shouldIssueJwt_whenEmailHashFound() {
            // Given
            MagicLinkTokenDataModel token = buildValidToken();
            stubTokenLookup(token);
            stubEmailNormalization();
            PersonPIIDataModel existingPii = new PersonPIIDataModel();
            when(personPIIRepository.findByEmailHash(TEST_EMAIL_HASH)).thenReturn(Optional.of(existingPii));
            stubJwtCreation(TEST_EMAIL);
            MagicLinkVerifyRequestDTO dto = buildVerifyRequest();

            // When
            LoginResult result = useCase.verifyMagicLink(dto);

            // Then
            assertThat(result.accessToken()).isEqualTo(TEST_ACCESS_TOKEN);
            assertThat(result.refreshToken()).isEqualTo(TEST_REFRESH_TOKEN);
            assertThat(result.username()).isEqualTo(TEST_EMAIL);

            InOrder inOrder = inOrder(tenantContextHolder, hashingService, magicLinkTokenRepository,
                    piiNormalizer, personPIIRepository, adultStudentRepository, tutorRepository, jwtTokenProvider);
            inOrder.verify(tenantContextHolder, times(1)).setTenantId(TEST_TENANT_ID);
            inOrder.verify(hashingService, times(1)).generateHash(TEST_RAW_TOKEN);
            inOrder.verify(magicLinkTokenRepository, times(1)).findByTokenHash(TEST_TOKEN_HASH);
            inOrder.verify(magicLinkTokenRepository, times(1)).save(tokenCaptor.capture());
            inOrder.verify(piiNormalizer, times(1)).normalizeEmail(TEST_EMAIL);
            inOrder.verify(hashingService, times(1)).generateHash(TEST_NORMALIZED_EMAIL);
            inOrder.verify(personPIIRepository, times(1)).findByEmailHash(TEST_EMAIL_HASH);
            inOrder.verify(adultStudentRepository, times(1)).findByPersonPiiId(null);
            inOrder.verify(tutorRepository, times(1)).findByPersonPiiId(null);
            inOrder.verify(jwtTokenProvider, times(1)).createAccessToken(
                    eq(TEST_EMAIL), eq(TEST_TENANT_ID), any(Map.class));
            inOrder.verify(jwtTokenProvider, times(1)).createRefreshToken(
                    eq(TEST_EMAIL), eq(TEST_TENANT_ID),
                    argThat(familyId -> familyId != null && !familyId.isEmpty()));
            inOrder.verifyNoMoreInteractions();
            verifyNoInteractions(applicationContext);
        }

        @Test
        @DisplayName("Should not create new user when email already exists")
        void shouldNotCreateNewUser_whenEmailAlreadyExists() {
            // Given
            MagicLinkTokenDataModel token = buildValidToken();
            stubTokenLookup(token);
            stubEmailNormalization();
            PersonPIIDataModel existingPii = new PersonPIIDataModel();
            when(personPIIRepository.findByEmailHash(TEST_EMAIL_HASH)).thenReturn(Optional.of(existingPii));
            stubJwtCreation(TEST_EMAIL);
            MagicLinkVerifyRequestDTO dto = buildVerifyRequest();

            // When
            LoginResult result = useCase.verifyMagicLink(dto);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.accessToken()).isEqualTo(TEST_ACCESS_TOKEN);
            assertThat(result.refreshToken()).isEqualTo(TEST_REFRESH_TOKEN);

            InOrder inOrder = inOrder(tenantContextHolder, hashingService, magicLinkTokenRepository,
                    piiNormalizer, personPIIRepository, jwtTokenProvider);
            inOrder.verify(tenantContextHolder, times(1)).setTenantId(TEST_TENANT_ID);
            inOrder.verify(hashingService, times(1)).generateHash(TEST_RAW_TOKEN);
            inOrder.verify(magicLinkTokenRepository, times(1)).findByTokenHash(TEST_TOKEN_HASH);
            inOrder.verify(magicLinkTokenRepository, times(1)).save(tokenCaptor.capture());
            assertThat(tokenCaptor.getValue().getUsedAt()).isNotNull();
            inOrder.verify(piiNormalizer, times(1)).normalizeEmail(TEST_EMAIL);
            inOrder.verify(hashingService, times(1)).generateHash(TEST_NORMALIZED_EMAIL);
            inOrder.verify(personPIIRepository, times(1)).findByEmailHash(TEST_EMAIL_HASH);
            inOrder.verify(jwtTokenProvider, times(1)).createAccessToken(
                    eq(TEST_EMAIL), eq(TEST_TENANT_ID), any(Map.class));
            inOrder.verify(jwtTokenProvider, times(1)).createRefreshToken(
                    eq(TEST_EMAIL), eq(TEST_TENANT_ID),
                    argThat(familyId -> familyId != null && !familyId.isEmpty()));
            inOrder.verifyNoMoreInteractions();
            verifyNoInteractions(applicationContext);
        }
    }

    @Nested
    @DisplayName("New User Creation")
    class NewUserCreation {

        @Test
        @DisplayName("Should create AdultStudent when email not found")
        void shouldCreateAdultStudent_whenEmailNotFound() {
            // Given
            MagicLinkTokenDataModel token = buildValidToken();
            stubTokenLookup(token);
            stubEmailNormalization();
            when(personPIIRepository.findByEmailHash(TEST_EMAIL_HASH)).thenReturn(Optional.empty());
            stubNewUserCreation();
            stubJwtCreation(TEST_NORMALIZED_EMAIL);
            MagicLinkVerifyRequestDTO dto = buildVerifyRequest();

            // When
            LoginResult result = useCase.verifyMagicLink(dto);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.accessToken()).isEqualTo(TEST_ACCESS_TOKEN);

            InOrder inOrder = inOrder(tenantContextHolder, hashingService, magicLinkTokenRepository,
                    piiNormalizer, personPIIRepository, applicationContext, adultStudentRepository, jwtTokenProvider);
            inOrder.verify(tenantContextHolder, times(1)).setTenantId(TEST_TENANT_ID);
            inOrder.verify(hashingService, times(1)).generateHash(TEST_RAW_TOKEN);
            inOrder.verify(magicLinkTokenRepository, times(1)).findByTokenHash(TEST_TOKEN_HASH);
            inOrder.verify(magicLinkTokenRepository, times(1)).save(tokenCaptor.capture());
            inOrder.verify(piiNormalizer, times(1)).normalizeEmail(TEST_EMAIL);
            inOrder.verify(hashingService, times(1)).generateHash(TEST_NORMALIZED_EMAIL);
            inOrder.verify(personPIIRepository, times(1)).findByEmailHash(TEST_EMAIL_HASH);
            inOrder.verify(applicationContext, times(1)).getBean(PersonPIIDataModel.class);
            inOrder.verify(hashingService, times(1)).generateHash(MagicLinkVerificationUseCase.PLACEHOLDER_PHONE);
            inOrder.verify(applicationContext, times(1)).getBean(CustomerAuthDataModel.class);
            inOrder.verify(applicationContext, times(1)).getBean(AdultStudentDataModel.class);
            inOrder.verify(adultStudentRepository, times(1)).save(studentCaptor.capture());
            AdultStudentDataModel saved = studentCaptor.getValue();
            assertThat(saved.getTenantId()).isEqualTo(TEST_TENANT_ID);
            assertThat(saved.getPersonPII()).isNotNull();
            assertThat(saved.getCustomerAuth()).isNotNull();
            inOrder.verify(jwtTokenProvider, times(1)).createAccessToken(
                    eq(TEST_NORMALIZED_EMAIL), eq(TEST_TENANT_ID), any(Map.class));
            inOrder.verify(jwtTokenProvider, times(1)).createRefreshToken(
                    eq(TEST_NORMALIZED_EMAIL), eq(TEST_TENANT_ID),
                    argThat(familyId -> familyId != null && !familyId.isEmpty()));
            inOrder.verifyNoMoreInteractions();
        }

        @Test
        @DisplayName("Should set placeholder values on new PersonPII when creating user")
        void shouldSetPlaceholderValues_whenCreatingUser() {
            // Given
            MagicLinkTokenDataModel token = buildValidToken();
            stubTokenLookup(token);
            stubEmailNormalization();
            when(personPIIRepository.findByEmailHash(TEST_EMAIL_HASH)).thenReturn(Optional.empty());
            stubNewUserCreation();
            stubJwtCreation(TEST_NORMALIZED_EMAIL);
            MagicLinkVerifyRequestDTO dto = buildVerifyRequest();

            // When
            LoginResult result = useCase.verifyMagicLink(dto);
            assertThat(result).isNotNull();

            // Then
            InOrder inOrder = inOrder(tenantContextHolder, hashingService, magicLinkTokenRepository,
                    piiNormalizer, personPIIRepository, applicationContext, adultStudentRepository, jwtTokenProvider);
            inOrder.verify(tenantContextHolder, times(1)).setTenantId(TEST_TENANT_ID);
            inOrder.verify(hashingService, times(1)).generateHash(TEST_RAW_TOKEN);
            inOrder.verify(magicLinkTokenRepository, times(1)).findByTokenHash(TEST_TOKEN_HASH);
            inOrder.verify(magicLinkTokenRepository, times(1)).save(tokenCaptor.capture());
            inOrder.verify(piiNormalizer, times(1)).normalizeEmail(TEST_EMAIL);
            inOrder.verify(hashingService, times(1)).generateHash(TEST_NORMALIZED_EMAIL);
            inOrder.verify(personPIIRepository, times(1)).findByEmailHash(TEST_EMAIL_HASH);
            inOrder.verify(applicationContext, times(1)).getBean(PersonPIIDataModel.class);
            inOrder.verify(hashingService, times(1)).generateHash(MagicLinkVerificationUseCase.PLACEHOLDER_PHONE);
            inOrder.verify(applicationContext, times(1)).getBean(CustomerAuthDataModel.class);
            inOrder.verify(applicationContext, times(1)).getBean(AdultStudentDataModel.class);
            inOrder.verify(adultStudentRepository, times(1)).save(studentCaptor.capture());
            PersonPIIDataModel pii = studentCaptor.getValue().getPersonPII();
            assertThat(pii.getEmail()).isEqualTo(TEST_NORMALIZED_EMAIL);
            assertThat(pii.getEmailHash()).isEqualTo(TEST_EMAIL_HASH);
            assertThat(pii.getLastName()).isEqualTo(MagicLinkVerificationUseCase.PLACEHOLDER_LAST_NAME);
            assertThat(pii.getPhoneNumber()).isEqualTo(MagicLinkVerificationUseCase.PLACEHOLDER_PHONE);
            assertThat(pii.getAddress()).isEqualTo(MagicLinkVerificationUseCase.PLACEHOLDER_ADDRESS);
            assertThat(pii.getZipCode()).isEqualTo(MagicLinkVerificationUseCase.PLACEHOLDER_ZIP);
            inOrder.verify(jwtTokenProvider, times(1)).createAccessToken(
                    eq(TEST_NORMALIZED_EMAIL), eq(TEST_TENANT_ID), any(Map.class));
            inOrder.verify(jwtTokenProvider, times(1)).createRefreshToken(
                    eq(TEST_NORMALIZED_EMAIL), eq(TEST_TENANT_ID),
                    argThat(familyId -> familyId != null && !familyId.isEmpty()));
            inOrder.verifyNoMoreInteractions();
        }

        @Test
        @DisplayName("Should set magic-link provider on CustomerAuth when creating user")
        void shouldSetMagicLinkProvider_whenCreatingUser() {
            // Given
            MagicLinkTokenDataModel token = buildValidToken();
            stubTokenLookup(token);
            stubEmailNormalization();
            when(personPIIRepository.findByEmailHash(TEST_EMAIL_HASH)).thenReturn(Optional.empty());
            stubNewUserCreation();
            stubJwtCreation(TEST_NORMALIZED_EMAIL);
            MagicLinkVerifyRequestDTO dto = buildVerifyRequest();

            // When
            LoginResult result = useCase.verifyMagicLink(dto);
            assertThat(result).isNotNull();

            // Then
            InOrder inOrder = inOrder(tenantContextHolder, hashingService, magicLinkTokenRepository,
                    piiNormalizer, personPIIRepository, applicationContext, adultStudentRepository, jwtTokenProvider);
            inOrder.verify(tenantContextHolder, times(1)).setTenantId(TEST_TENANT_ID);
            inOrder.verify(hashingService, times(1)).generateHash(TEST_RAW_TOKEN);
            inOrder.verify(magicLinkTokenRepository, times(1)).findByTokenHash(TEST_TOKEN_HASH);
            inOrder.verify(magicLinkTokenRepository, times(1)).save(tokenCaptor.capture());
            inOrder.verify(piiNormalizer, times(1)).normalizeEmail(TEST_EMAIL);
            inOrder.verify(hashingService, times(1)).generateHash(TEST_NORMALIZED_EMAIL);
            inOrder.verify(personPIIRepository, times(1)).findByEmailHash(TEST_EMAIL_HASH);
            inOrder.verify(applicationContext, times(1)).getBean(PersonPIIDataModel.class);
            inOrder.verify(hashingService, times(1)).generateHash(MagicLinkVerificationUseCase.PLACEHOLDER_PHONE);
            inOrder.verify(applicationContext, times(1)).getBean(CustomerAuthDataModel.class);
            inOrder.verify(applicationContext, times(1)).getBean(AdultStudentDataModel.class);
            inOrder.verify(adultStudentRepository, times(1)).save(studentCaptor.capture());
            CustomerAuthDataModel auth = studentCaptor.getValue().getCustomerAuth();
            assertThat(auth.getProvider()).isEqualTo(MagicLinkVerificationUseCase.PROVIDER_MAGIC_LINK);
            assertThat(auth.getToken()).isEqualTo(MagicLinkVerificationUseCase.AUTH_TOKEN_PLACEHOLDER);
            inOrder.verify(jwtTokenProvider, times(1)).createAccessToken(
                    eq(TEST_NORMALIZED_EMAIL), eq(TEST_TENANT_ID), any(Map.class));
            inOrder.verify(jwtTokenProvider, times(1)).createRefreshToken(
                    eq(TEST_NORMALIZED_EMAIL), eq(TEST_TENANT_ID),
                    argThat(familyId -> familyId != null && !familyId.isEmpty()));
            inOrder.verifyNoMoreInteractions();
        }

        @Test
        @DisplayName("Should issue JWT with CUSTOMER role when creating new user")
        void shouldIssueJwtWithCustomerRole_whenCreatingNewUser() {
            // Given
            MagicLinkTokenDataModel token = buildValidToken();
            stubTokenLookup(token);
            stubEmailNormalization();
            when(personPIIRepository.findByEmailHash(TEST_EMAIL_HASH)).thenReturn(Optional.empty());
            stubNewUserCreation();
            stubJwtCreation(TEST_NORMALIZED_EMAIL);
            MagicLinkVerifyRequestDTO dto = buildVerifyRequest();

            // When
            LoginResult result = useCase.verifyMagicLink(dto);

            // Then
            assertThat(result.accessToken()).isEqualTo(TEST_ACCESS_TOKEN);
            assertThat(result.refreshToken()).isEqualTo(TEST_REFRESH_TOKEN);
            assertThat(result.username()).isEqualTo(TEST_NORMALIZED_EMAIL);

            InOrder inOrder = inOrder(tenantContextHolder, hashingService, magicLinkTokenRepository,
                    piiNormalizer, personPIIRepository, applicationContext, adultStudentRepository, jwtTokenProvider);
            inOrder.verify(tenantContextHolder, times(1)).setTenantId(TEST_TENANT_ID);
            inOrder.verify(hashingService, times(1)).generateHash(TEST_RAW_TOKEN);
            inOrder.verify(magicLinkTokenRepository, times(1)).findByTokenHash(TEST_TOKEN_HASH);
            inOrder.verify(magicLinkTokenRepository, times(1)).save(tokenCaptor.capture());
            inOrder.verify(piiNormalizer, times(1)).normalizeEmail(TEST_EMAIL);
            inOrder.verify(hashingService, times(1)).generateHash(TEST_NORMALIZED_EMAIL);
            inOrder.verify(personPIIRepository, times(1)).findByEmailHash(TEST_EMAIL_HASH);
            inOrder.verify(applicationContext, times(1)).getBean(PersonPIIDataModel.class);
            inOrder.verify(hashingService, times(1)).generateHash(MagicLinkVerificationUseCase.PLACEHOLDER_PHONE);
            inOrder.verify(applicationContext, times(1)).getBean(CustomerAuthDataModel.class);
            inOrder.verify(applicationContext, times(1)).getBean(AdultStudentDataModel.class);
            inOrder.verify(adultStudentRepository, times(1)).save(studentCaptor.capture());
            inOrder.verify(jwtTokenProvider, times(1)).createAccessToken(
                    eq(TEST_NORMALIZED_EMAIL), eq(TEST_TENANT_ID), any(Map.class));
            inOrder.verify(jwtTokenProvider, times(1)).createRefreshToken(
                    eq(TEST_NORMALIZED_EMAIL), eq(TEST_TENANT_ID),
                    argThat(familyId -> familyId != null && !familyId.isEmpty()));
            inOrder.verifyNoMoreInteractions();
        }
    }

    private void stubNewUserCreation() {
        when(applicationContext.getBean(PersonPIIDataModel.class)).thenReturn(new PersonPIIDataModel());
        when(applicationContext.getBean(CustomerAuthDataModel.class)).thenReturn(new CustomerAuthDataModel());
        when(applicationContext.getBean(AdultStudentDataModel.class)).thenReturn(new AdultStudentDataModel());
        when(hashingService.generateHash(MagicLinkVerificationUseCase.PLACEHOLDER_PHONE)).thenReturn(TEST_PHONE_HASH);
    }

    @SuppressWarnings("unchecked")
    private void stubJwtCreation(String email) {
        when(jwtTokenProvider.createAccessToken(eq(email), eq(TEST_TENANT_ID), any(Map.class)))
                .thenReturn(TEST_ACCESS_TOKEN);
        when(jwtTokenProvider.createRefreshToken(
                eq(email),
                eq(TEST_TENANT_ID),
                argThat(familyId -> familyId != null && !familyId.isEmpty())))
                .thenReturn(TEST_REFRESH_TOKEN);
    }
}
