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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationContext;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link OAuthAuthenticationUseCase}.
 *
 * @author ElatusDev
 * @since 1.0
 */
@DisplayName("OAuthAuthenticationUseCase")
@ExtendWith(MockitoExtension.class)
class OAuthAuthenticationUseCaseTest {

    private static final String TEST_EMAIL = "user@example.com";
    private static final String TEST_EMAIL_HASH = "hash123";
    private static final String TEST_NEW_EMAIL = "new@example.com";
    private static final String TEST_NEW_EMAIL_HASH = "newhash";
    private static final String TEST_PLACEHOLDER_HASH = "placeholder-hash";
    private static final String TEST_AUTH_CODE = "valid-code";
    private static final String TEST_BAD_CODE = "bad-code";
    private static final String TEST_REDIRECT_URI = "uri";
    private static final Long TEST_TENANT_ID = 1L;
    private static final String TEST_ACCESS_TOKEN = "access-token";
    private static final String TEST_REFRESH_TOKEN = "refresh-token";
    private static final String TEST_NEW_ACCESS_TOKEN = "new-access";
    private static final String TEST_NEW_REFRESH_TOKEN = "new-refresh";
    private static final String TEST_PROVIDER_USER_ID = "gid-123";
    private static final String TEST_NEW_PROVIDER_USER_ID = "gid-456";
    private static final String TEST_FULL_NAME = "Test User";
    private static final String TEST_NEW_FULL_NAME = "New User";

    @Mock private OAuthProviderClient googleClient;
    @Mock private PersonPIIRepository personPIIRepository;
    @Mock private AdultStudentRepository adultStudentRepository;
    @Mock private JwtTokenProvider jwtTokenProvider;
    @Mock private HashingService hashingService;
    @Mock private PiiNormalizer piiNormalizer;
    @Mock private TenantContextHolder tenantContextHolder;
    @Mock private ApplicationContext applicationContext;

    @Captor private ArgumentCaptor<AdultStudentDataModel> studentCaptor;

    private OAuthAuthenticationUseCase useCase;

    @BeforeEach
    void setUp() {
        Map<String, OAuthProviderClient> clients = Map.of("google", googleClient);
        useCase = new OAuthAuthenticationUseCase(
                clients, personPIIRepository, adultStudentRepository,
                jwtTokenProvider, hashingService, piiNormalizer,
                tenantContextHolder, applicationContext
        );
    }

    @Nested
    @DisplayName("Unsupported Provider")
    class UnsupportedProvider {

        @Test
        @DisplayName("Should throw UnsupportedProviderException when provider is unknown")
        void shouldThrowUnsupportedProviderException_whenProviderIsUnknown() {
            // Given / When / Then
            assertThatThrownBy(() -> useCase.loginWithOAuth("twitter", "code", TEST_REDIRECT_URI, TEST_TENANT_ID))
                    .isInstanceOf(UnsupportedProviderException.class)
                    .hasMessageContaining("twitter");

            verifyNoInteractions(googleClient, personPIIRepository, adultStudentRepository,
                    jwtTokenProvider, hashingService, piiNormalizer,
                    tenantContextHolder, applicationContext);
        }
    }

    @Nested
    @DisplayName("Provider Exchange Failure")
    class ProviderExchangeFailure {

        @Test
        @DisplayName("Should throw OAuthProviderException when code exchange fails")
        void shouldThrowOAuthProviderException_whenCodeExchangeFails() {
            // Given
            when(googleClient.exchangeCodeForProfile(TEST_BAD_CODE, TEST_REDIRECT_URI))
                    .thenThrow(new OAuthProviderException("google"));

            // When / Then
            assertThatThrownBy(() -> useCase.loginWithOAuth("google", TEST_BAD_CODE, TEST_REDIRECT_URI, TEST_TENANT_ID))
                    .isInstanceOf(OAuthProviderException.class)
                    .hasMessageContaining("google");

            InOrder inOrder = inOrder(tenantContextHolder, googleClient);
            inOrder.verify(tenantContextHolder, times(1)).setTenantId(TEST_TENANT_ID);
            inOrder.verify(googleClient, times(1)).exchangeCodeForProfile(TEST_BAD_CODE, TEST_REDIRECT_URI);
            inOrder.verifyNoMoreInteractions();
            verifyNoInteractions(piiNormalizer, hashingService, personPIIRepository,
                    adultStudentRepository, jwtTokenProvider, applicationContext);
        }
    }

    @Nested
    @DisplayName("Existing User Login")
    class ExistingUserLogin {

        @Test
        @DisplayName("Should return LoginResult when existing user logs in via OAuth")
        void shouldReturnLoginResult_whenExistingUserLogsIn() {
            // Given
            OAuthUserProfile profile = new OAuthUserProfile(TEST_PROVIDER_USER_ID, TEST_EMAIL, TEST_FULL_NAME);
            when(googleClient.exchangeCodeForProfile(TEST_AUTH_CODE, TEST_REDIRECT_URI)).thenReturn(profile);
            when(piiNormalizer.normalizeEmail(TEST_EMAIL)).thenReturn(TEST_EMAIL);
            when(hashingService.generateHash(TEST_EMAIL)).thenReturn(TEST_EMAIL_HASH);

            PersonPIIDataModel existingPii = new PersonPIIDataModel();
            when(personPIIRepository.findByEmailHash(TEST_EMAIL_HASH)).thenReturn(Optional.of(existingPii));

            Map<String, Object> expectedClaims = Map.of(
                    OAuthAuthenticationUseCase.JWT_CLAIM_ROLE, OAuthAuthenticationUseCase.ROLE_CUSTOMER);
            when(jwtTokenProvider.createAccessToken(TEST_EMAIL, TEST_TENANT_ID, expectedClaims))
                    .thenReturn(TEST_ACCESS_TOKEN);
            when(jwtTokenProvider.createRefreshToken(
                    eq(TEST_EMAIL), eq(TEST_TENANT_ID),
                    argThat(familyId -> familyId != null && !familyId.isEmpty())))
                    .thenReturn(TEST_REFRESH_TOKEN);

            // When
            LoginResult result = useCase.loginWithOAuth("google", TEST_AUTH_CODE, TEST_REDIRECT_URI, TEST_TENANT_ID);

            // Then
            assertThat(result.accessToken()).isEqualTo(TEST_ACCESS_TOKEN);
            assertThat(result.refreshToken()).isEqualTo(TEST_REFRESH_TOKEN);
            assertThat(result.username()).isEqualTo(TEST_EMAIL);

            InOrder inOrder = inOrder(tenantContextHolder, googleClient, piiNormalizer,
                    hashingService, personPIIRepository, jwtTokenProvider);
            inOrder.verify(tenantContextHolder, times(1)).setTenantId(TEST_TENANT_ID);
            inOrder.verify(googleClient, times(1)).exchangeCodeForProfile(TEST_AUTH_CODE, TEST_REDIRECT_URI);
            inOrder.verify(piiNormalizer, times(1)).normalizeEmail(TEST_EMAIL);
            inOrder.verify(hashingService, times(1)).generateHash(TEST_EMAIL);
            inOrder.verify(personPIIRepository, times(1)).findByEmailHash(TEST_EMAIL_HASH);
            inOrder.verify(jwtTokenProvider, times(1)).createAccessToken(TEST_EMAIL, TEST_TENANT_ID, expectedClaims);
            inOrder.verify(jwtTokenProvider, times(1)).createRefreshToken(
                    eq(TEST_EMAIL), eq(TEST_TENANT_ID),
                    argThat(familyId -> familyId != null && !familyId.isEmpty()));
            inOrder.verifyNoMoreInteractions();
            verifyNoInteractions(adultStudentRepository, applicationContext);
        }
    }

    @Nested
    @DisplayName("New User Registration")
    class NewUserRegistration {

        @Test
        @DisplayName("Should create new user when email not found")
        void shouldCreateNewUser_whenEmailNotFound() {
            // Given
            OAuthUserProfile profile = new OAuthUserProfile(TEST_NEW_PROVIDER_USER_ID, TEST_NEW_EMAIL, TEST_NEW_FULL_NAME);
            when(googleClient.exchangeCodeForProfile(TEST_AUTH_CODE, TEST_REDIRECT_URI)).thenReturn(profile);
            when(piiNormalizer.normalizeEmail(TEST_NEW_EMAIL)).thenReturn(TEST_NEW_EMAIL);
            when(hashingService.generateHash(TEST_NEW_EMAIL)).thenReturn(TEST_NEW_EMAIL_HASH);
            when(hashingService.generateHash(OAuthAuthenticationUseCase.PLACEHOLDER)).thenReturn(TEST_PLACEHOLDER_HASH);
            when(personPIIRepository.findByEmailHash(TEST_NEW_EMAIL_HASH)).thenReturn(Optional.empty());

            when(applicationContext.getBean(PersonPIIDataModel.class)).thenReturn(new PersonPIIDataModel());
            when(applicationContext.getBean(CustomerAuthDataModel.class)).thenReturn(new CustomerAuthDataModel());
            when(applicationContext.getBean(AdultStudentDataModel.class)).thenReturn(new AdultStudentDataModel());

            Map<String, Object> expectedClaims = Map.of(
                    OAuthAuthenticationUseCase.JWT_CLAIM_ROLE, OAuthAuthenticationUseCase.ROLE_CUSTOMER);
            when(jwtTokenProvider.createAccessToken(TEST_NEW_EMAIL, TEST_TENANT_ID, expectedClaims))
                    .thenReturn(TEST_NEW_ACCESS_TOKEN);
            when(jwtTokenProvider.createRefreshToken(
                    eq(TEST_NEW_EMAIL), eq(TEST_TENANT_ID),
                    argThat(familyId -> familyId != null && !familyId.isEmpty())))
                    .thenReturn(TEST_NEW_REFRESH_TOKEN);

            // When
            LoginResult result = useCase.loginWithOAuth("google", TEST_AUTH_CODE, TEST_REDIRECT_URI, TEST_TENANT_ID);

            // Then
            assertThat(result.accessToken()).isEqualTo(TEST_NEW_ACCESS_TOKEN);
            assertThat(result.refreshToken()).isEqualTo(TEST_NEW_REFRESH_TOKEN);
            assertThat(result.username()).isEqualTo(TEST_NEW_EMAIL);

            InOrder inOrder = inOrder(tenantContextHolder, googleClient, piiNormalizer,
                    hashingService, personPIIRepository, applicationContext,
                    adultStudentRepository, jwtTokenProvider);
            inOrder.verify(tenantContextHolder, times(1)).setTenantId(TEST_TENANT_ID);
            inOrder.verify(googleClient, times(1)).exchangeCodeForProfile(TEST_AUTH_CODE, TEST_REDIRECT_URI);
            inOrder.verify(piiNormalizer, times(1)).normalizeEmail(TEST_NEW_EMAIL);
            inOrder.verify(hashingService, times(1)).generateHash(TEST_NEW_EMAIL);
            inOrder.verify(personPIIRepository, times(1)).findByEmailHash(TEST_NEW_EMAIL_HASH);
            inOrder.verify(applicationContext, times(1)).getBean(PersonPIIDataModel.class);
            inOrder.verify(hashingService, times(1)).generateHash(OAuthAuthenticationUseCase.PLACEHOLDER);
            inOrder.verify(applicationContext, times(1)).getBean(CustomerAuthDataModel.class);
            inOrder.verify(applicationContext, times(1)).getBean(AdultStudentDataModel.class);
            inOrder.verify(adultStudentRepository, times(1)).save(studentCaptor.capture());
            AdultStudentDataModel saved = studentCaptor.getValue();
            assertThat(saved.getTenantId()).isEqualTo(TEST_TENANT_ID);
            assertThat(saved.getPersonPII()).isNotNull();
            assertThat(saved.getPersonPII().getEmail()).isEqualTo(TEST_NEW_EMAIL);
            assertThat(saved.getCustomerAuth()).isNotNull();
            assertThat(saved.getCustomerAuth().getProvider()).isEqualTo("google");
            inOrder.verify(jwtTokenProvider, times(1)).createAccessToken(TEST_NEW_EMAIL, TEST_TENANT_ID, expectedClaims);
            inOrder.verify(jwtTokenProvider, times(1)).createRefreshToken(
                    eq(TEST_NEW_EMAIL), eq(TEST_TENANT_ID),
                    argThat(familyId -> familyId != null && !familyId.isEmpty()));
            inOrder.verifyNoMoreInteractions();
        }
    }
}
