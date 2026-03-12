/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.usecases;

import com.akademiaplus.usecases.domain.BootstrapToken;
import com.akademiaplus.usecases.domain.TokenManifest;
import com.akademiaplus.usecases.exceptions.InvalidBootstrapTokenException;
import com.akademiaplus.usecases.exceptions.TokenAlreadyUsedException;
import com.akademiaplus.usecases.exceptions.TokenCnMismatchException;
import openapi.akademiaplus.domain.certificate.authority.dto.CertificateResponseDTO;
import openapi.akademiaplus.domain.certificate.authority.dto.EnrollmentRequestDTO;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("EnrollServiceUseCase Tests")
class EnrollServiceUseCaseTest {

    private static final String VALID_TOKEN = "valid-token-abc";
    private static final String BOUND_CN = "platform-core-api";
    private static final String ISSUED_TIMESTAMP = "2026-02-21T00:00:00Z";
    // Minimal valid base64(PEM CSR) — use a placeholder; real CSR parsing is tested in SignCertificateUseCaseTest
    private static final String FAKE_CSR_BASE64 = Base64.getEncoder()
            .encodeToString("-----BEGIN CERTIFICATE REQUEST-----\nfake\n-----END CERTIFICATE REQUEST-----".getBytes());

    @Mock
    private TokenManifest tokenManifest;

    @Mock
    private SignCertificateUseCase signCertificateUseCase;

    @InjectMocks
    private EnrollServiceUseCase useCase;

    private EnrollmentRequestDTO buildRequest(String token, String cn, String csrBase64) {
        EnrollmentRequestDTO dto = new EnrollmentRequestDTO();
        dto.setBootstrapToken(token);
        dto.setCommonName(cn);
        dto.setCertificateSigningRequest(csrBase64);
        return dto;
    }

    private BootstrapToken validToken() {
        return new BootstrapToken(VALID_TOKEN, BOUND_CN, ISSUED_TIMESTAMP, false);
    }

    @Nested
    @DisplayName("Successful Enrollment Tests")
    class SuccessfulEnrollmentTests {

        @Test
        @DisplayName("Should sign CSR and return certificate when given valid bootstrap token")
        void shouldSignCsr_whenGivenValidBootstrapToken() {
            // Given
            EnrollmentRequestDTO request = buildRequest(VALID_TOKEN, BOUND_CN, FAKE_CSR_BASE64);
            BootstrapToken token = validToken();
            PKCS10CertificationRequest mockCsr = mock(PKCS10CertificationRequest.class);
            CertificateResponseDTO expectedResponse = new CertificateResponseDTO();
            expectedResponse.setSignedCertificate("signed-cert");
            expectedResponse.setCaCertificate("ca-cert");

            when(tokenManifest.validate(VALID_TOKEN, BOUND_CN)).thenReturn(token);
            when(signCertificateUseCase.parseCsr(FAKE_CSR_BASE64)).thenReturn(mockCsr);
            when(signCertificateUseCase.signEnrollment(mockCsr, BOUND_CN)).thenReturn(expectedResponse);

            // When
            CertificateResponseDTO result = useCase.enroll(request);

            // Then
            assertThat(result).isSameAs(expectedResponse);

            var inOrder = inOrder(tokenManifest, signCertificateUseCase);
            inOrder.verify(tokenManifest, times(1)).validate(VALID_TOKEN, BOUND_CN);
            inOrder.verify(signCertificateUseCase, times(1)).parseCsr(FAKE_CSR_BASE64);
            inOrder.verify(signCertificateUseCase, times(1)).signEnrollment(mockCsr, BOUND_CN);
            inOrder.verify(tokenManifest, times(1)).invalidate(VALID_TOKEN);
            inOrder.verifyNoMoreInteractions();
        }

        @Test
        @DisplayName("Should invalidate token when enrollment succeeds")
        void shouldInvalidateToken_whenEnrollmentSucceeds() {
            // Given
            EnrollmentRequestDTO request = buildRequest(VALID_TOKEN, BOUND_CN, FAKE_CSR_BASE64);
            BootstrapToken token = validToken();
            PKCS10CertificationRequest mockCsr = mock(PKCS10CertificationRequest.class);
            CertificateResponseDTO response = new CertificateResponseDTO();
            response.setSignedCertificate("cert");
            response.setCaCertificate("ca");

            when(tokenManifest.validate(VALID_TOKEN, BOUND_CN)).thenReturn(token);
            when(signCertificateUseCase.parseCsr(FAKE_CSR_BASE64)).thenReturn(mockCsr);
            when(signCertificateUseCase.signEnrollment(mockCsr, BOUND_CN)).thenReturn(response);

            // When
            CertificateResponseDTO result = useCase.enroll(request);

            // Then
            assertThat(result).isSameAs(response);

            var inOrder = inOrder(tokenManifest, signCertificateUseCase);
            inOrder.verify(tokenManifest, times(1)).validate(VALID_TOKEN, BOUND_CN);
            inOrder.verify(signCertificateUseCase, times(1)).parseCsr(FAKE_CSR_BASE64);
            inOrder.verify(signCertificateUseCase, times(1)).signEnrollment(mockCsr, BOUND_CN);
            inOrder.verify(tokenManifest, times(1)).invalidate(VALID_TOKEN);
            inOrder.verifyNoMoreInteractions();
        }

        @Test
        @DisplayName("Should call validate then sign then invalidate in correct order")
        void shouldCallValidateThenSignThenInvalidate_whenEnrolling() {
            // Given
            EnrollmentRequestDTO request = buildRequest(VALID_TOKEN, BOUND_CN, FAKE_CSR_BASE64);
            BootstrapToken token = validToken();
            PKCS10CertificationRequest mockCsr = mock(PKCS10CertificationRequest.class);
            CertificateResponseDTO response = new CertificateResponseDTO();
            response.setSignedCertificate("cert");
            response.setCaCertificate("ca");

            when(tokenManifest.validate(VALID_TOKEN, BOUND_CN)).thenReturn(token);
            when(signCertificateUseCase.parseCsr(FAKE_CSR_BASE64)).thenReturn(mockCsr);
            when(signCertificateUseCase.signEnrollment(mockCsr, BOUND_CN)).thenReturn(response);

            // When
            CertificateResponseDTO result = useCase.enroll(request);

            // Then
            assertThat(result).isSameAs(response);

            // Then — verify order: validate → parseCsr → signEnrollment → invalidate
            var inOrder = inOrder(tokenManifest, signCertificateUseCase);
            inOrder.verify(tokenManifest, times(1)).validate(VALID_TOKEN, BOUND_CN);
            inOrder.verify(signCertificateUseCase, times(1)).parseCsr(FAKE_CSR_BASE64);
            inOrder.verify(signCertificateUseCase, times(1)).signEnrollment(mockCsr, BOUND_CN);
            inOrder.verify(tokenManifest, times(1)).invalidate(VALID_TOKEN);
            inOrder.verifyNoMoreInteractions();
        }
    }

    @Nested
    @DisplayName("Token Validation Failure Tests")
    class TokenValidationFailureTests {

        @Test
        @DisplayName("Should propagate InvalidBootstrapTokenException when token is not found")
        void shouldPropagateInvalidBootstrapTokenException_whenTokenNotFound() {
            // Given
            EnrollmentRequestDTO request = buildRequest(VALID_TOKEN, BOUND_CN, FAKE_CSR_BASE64);
            when(tokenManifest.validate(VALID_TOKEN, BOUND_CN))
                    .thenThrow(new InvalidBootstrapTokenException(TokenManifest.INVALID_TOKEN_MSG));

            // When & Then
            assertThatThrownBy(() -> useCase.enroll(request))
                    .isInstanceOf(InvalidBootstrapTokenException.class)
                    .hasMessage(TokenManifest.INVALID_TOKEN_MSG);

            verify(tokenManifest, times(1)).validate(VALID_TOKEN, BOUND_CN);
            verifyNoInteractions(signCertificateUseCase);
            verifyNoMoreInteractions(tokenManifest, signCertificateUseCase);
        }

        @Test
        @DisplayName("Should reject enrollment when token has already been used")
        void shouldRejectEnrollment_whenTokenAlreadyUsed() {
            // Given
            EnrollmentRequestDTO request = buildRequest(VALID_TOKEN, BOUND_CN, FAKE_CSR_BASE64);
            when(tokenManifest.validate(VALID_TOKEN, BOUND_CN))
                    .thenThrow(new TokenAlreadyUsedException(TokenManifest.TOKEN_ALREADY_USED_MSG));

            // When & Then
            assertThatThrownBy(() -> useCase.enroll(request))
                    .isInstanceOf(TokenAlreadyUsedException.class)
                    .hasMessage(TokenManifest.TOKEN_ALREADY_USED_MSG);

            verify(tokenManifest, times(1)).validate(VALID_TOKEN, BOUND_CN);
            verifyNoInteractions(signCertificateUseCase);
            verifyNoMoreInteractions(tokenManifest, signCertificateUseCase);
        }

        @Test
        @DisplayName("Should reject enrollment when CN does not match token bound CN")
        void shouldRejectEnrollment_whenCnDoesNotMatchTokenBoundCn() {
            // Given
            EnrollmentRequestDTO request = buildRequest(VALID_TOKEN, BOUND_CN, FAKE_CSR_BASE64);
            when(tokenManifest.validate(VALID_TOKEN, BOUND_CN))
                    .thenThrow(new TokenCnMismatchException(TokenManifest.TOKEN_CN_MISMATCH_MSG));

            // When & Then
            assertThatThrownBy(() -> useCase.enroll(request))
                    .isInstanceOf(TokenCnMismatchException.class)
                    .hasMessage(TokenManifest.TOKEN_CN_MISMATCH_MSG);

            verify(tokenManifest, times(1)).validate(VALID_TOKEN, BOUND_CN);
            verifyNoInteractions(signCertificateUseCase);
            verifyNoMoreInteractions(tokenManifest, signCertificateUseCase);
        }

        @Test
        @DisplayName("Should not invalidate token when CSR signing fails")
        void shouldNotInvalidateToken_whenCsrSigningFails() {
            // Given
            EnrollmentRequestDTO request = buildRequest(VALID_TOKEN, BOUND_CN, FAKE_CSR_BASE64);
            BootstrapToken token = validToken();
            PKCS10CertificationRequest mockCsr = mock(PKCS10CertificationRequest.class);

            when(tokenManifest.validate(VALID_TOKEN, BOUND_CN)).thenReturn(token);
            when(signCertificateUseCase.parseCsr(FAKE_CSR_BASE64)).thenReturn(mockCsr);
            when(signCertificateUseCase.signEnrollment(mockCsr, BOUND_CN))
                    .thenThrow(new IllegalStateException("Signing failed"));

            // When & Then
            assertThatThrownBy(() -> useCase.enroll(request))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("Signing failed");

            var inOrder = inOrder(tokenManifest, signCertificateUseCase);
            inOrder.verify(tokenManifest, times(1)).validate(VALID_TOKEN, BOUND_CN);
            inOrder.verify(signCertificateUseCase, times(1)).parseCsr(FAKE_CSR_BASE64);
            inOrder.verify(signCertificateUseCase, times(1)).signEnrollment(mockCsr, BOUND_CN);
            inOrder.verifyNoMoreInteractions();
        }
    }
}
