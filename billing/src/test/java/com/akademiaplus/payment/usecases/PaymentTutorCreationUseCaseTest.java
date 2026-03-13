/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.payment.usecases;

import com.akademiaplus.billing.customerpayment.PaymentTutorDataModel;
import com.akademiaplus.billing.membership.MembershipTutorDataModel;
import com.akademiaplus.infra.persistence.config.TenantContextHolder;
import com.akademiaplus.membership.interfaceadapters.MembershipTutorRepository;
import com.akademiaplus.membership.interfaceadapters.PaymentTutorRepository;
import openapi.akademiaplus.domain.billing.dto.PaymentTutorCreationRequestDTO;
import openapi.akademiaplus.domain.billing.dto.PaymentTutorCreationResponseDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.springframework.context.ApplicationContext;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@DisplayName("PaymentTutorCreationUseCase")
@ExtendWith(MockitoExtension.class)
class PaymentTutorCreationUseCaseTest {

    @Mock private ApplicationContext applicationContext;
    @Mock private PaymentTutorRepository paymentRepository;
    @Mock private MembershipTutorRepository membershipTutorRepository;
    @Mock private TenantContextHolder tenantContextHolder;
    @Mock private ModelMapper modelMapper;

    private PaymentTutorCreationUseCase useCase;

    private static final Long TENANT_ID = 1L;
    private static final LocalDate PAYMENT_DATE = LocalDate.of(2026, 3, 15);
    private static final Double AMOUNT = 300.0;
    private static final String PAYMENT_METHOD = "TRANSFER";
    private static final Long MEMBERSHIP_TUTOR_ID = 2L;
    private static final Long SAVED_ID = 1L;

    @BeforeEach
    void setUp() {
        useCase = new PaymentTutorCreationUseCase(
                applicationContext, paymentRepository, membershipTutorRepository, tenantContextHolder, modelMapper);
        lenient().when(tenantContextHolder.getTenantId()).thenReturn(Optional.of(TENANT_ID));
    }

    private PaymentTutorCreationRequestDTO buildDto() {
        PaymentTutorCreationRequestDTO dto = new PaymentTutorCreationRequestDTO();
        dto.setPaymentDate(PAYMENT_DATE);
        dto.setAmount(AMOUNT);
        dto.setPaymentMethod(PAYMENT_METHOD);
        dto.setMembershipTutorId(MEMBERSHIP_TUTOR_ID);
        return dto;
    }

    @Nested
    @DisplayName("Transformation")
    class Transformation {

        @Test
        @DisplayName("Should retrieve prototype bean from ApplicationContext")
        void shouldRetrievePrototypeBean_whenTransforming() {
            // Given
            PaymentTutorCreationRequestDTO dto = buildDto();
            PaymentTutorDataModel prototypeModel = new PaymentTutorDataModel();
            when(applicationContext.getBean(PaymentTutorDataModel.class)).thenReturn(prototypeModel);
            when(membershipTutorRepository.findById(new MembershipTutorDataModel.MembershipTutorCompositeId(TENANT_ID, MEMBERSHIP_TUTOR_ID)))
                    .thenReturn(Optional.of(new MembershipTutorDataModel()));

            // When
            useCase.transform(dto);

            // Then
            verify(applicationContext, times(1)).getBean(PaymentTutorDataModel.class);
            verifyNoMoreInteractions(applicationContext, paymentRepository);
        }

        @Test
        @DisplayName("Should delegate mapping to ModelMapper with named TypeMap")
        void shouldDelegateToModelMapper_whenTransforming() {
            // Given
            PaymentTutorCreationRequestDTO dto = buildDto();
            PaymentTutorDataModel prototypeModel = new PaymentTutorDataModel();
            when(applicationContext.getBean(PaymentTutorDataModel.class)).thenReturn(prototypeModel);
            when(membershipTutorRepository.findById(new MembershipTutorDataModel.MembershipTutorCompositeId(TENANT_ID, MEMBERSHIP_TUTOR_ID)))
                    .thenReturn(Optional.of(new MembershipTutorDataModel()));

            // When
            PaymentTutorDataModel result = useCase.transform(dto);

            // Then
            verify(modelMapper, times(1)).map(dto, prototypeModel, PaymentTutorCreationUseCase.MAP_NAME);
            assertThat(result).isSameAs(prototypeModel);
            verifyNoMoreInteractions(applicationContext, paymentRepository);
        }

        @Test
        @DisplayName("Should resolve membershipTutor FK via repository lookup")
        void shouldResolveFK_whenTransforming() {
            // Given
            PaymentTutorCreationRequestDTO dto = buildDto();
            PaymentTutorDataModel prototypeModel = new PaymentTutorDataModel();
            MembershipTutorDataModel membershipTutor = new MembershipTutorDataModel();
            when(applicationContext.getBean(PaymentTutorDataModel.class)).thenReturn(prototypeModel);
            when(membershipTutorRepository.findById(new MembershipTutorDataModel.MembershipTutorCompositeId(TENANT_ID, MEMBERSHIP_TUTOR_ID)))
                    .thenReturn(Optional.of(membershipTutor));

            // When
            PaymentTutorDataModel result = useCase.transform(dto);

            // Then
            assertThat(result.getMembershipTutor()).isSameAs(membershipTutor);
            verifyNoMoreInteractions(applicationContext, paymentRepository);
        }

        @Test
        @DisplayName("Should throw when membershipTutor is not found")
        void shouldThrow_whenMembershipTutorNotFound() {
            // Given
            PaymentTutorCreationRequestDTO dto = buildDto();
            PaymentTutorDataModel prototypeModel = new PaymentTutorDataModel();
            when(applicationContext.getBean(PaymentTutorDataModel.class)).thenReturn(prototypeModel);
            when(membershipTutorRepository.findById(new MembershipTutorDataModel.MembershipTutorCompositeId(TENANT_ID, MEMBERSHIP_TUTOR_ID))).thenReturn(Optional.empty());

            // When / Then
            assertThatThrownBy(() -> useCase.transform(dto))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage(PaymentTutorCreationUseCase.ERROR_MEMBERSHIP_TUTOR_NOT_FOUND + MEMBERSHIP_TUTOR_ID);

            verify(applicationContext, times(1)).getBean(PaymentTutorDataModel.class);
            verify(membershipTutorRepository, times(1)).findById(new MembershipTutorDataModel.MembershipTutorCompositeId(TENANT_ID, MEMBERSHIP_TUTOR_ID));
            verifyNoInteractions(paymentRepository);
            verifyNoMoreInteractions(applicationContext, membershipTutorRepository);
        }

        @Test
        @DisplayName("Should throw when tenant context is missing")
        void shouldThrow_whenTenantContextMissing() {
            // Given
            PaymentTutorCreationRequestDTO dto = buildDto();
            PaymentTutorDataModel prototypeModel = new PaymentTutorDataModel();
            when(applicationContext.getBean(PaymentTutorDataModel.class)).thenReturn(prototypeModel);
            when(tenantContextHolder.getTenantId()).thenReturn(Optional.empty());

            // When / Then
            assertThatThrownBy(() -> useCase.transform(dto))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage(PaymentTutorCreationUseCase.ERROR_TENANT_CONTEXT_REQUIRED);

            verify(applicationContext, times(1)).getBean(PaymentTutorDataModel.class);
            verify(tenantContextHolder, times(1)).getTenantId();
            verifyNoInteractions(paymentRepository);
            verifyNoMoreInteractions(applicationContext, membershipTutorRepository);
        }
    }

    @Nested
    @DisplayName("Collaborator exception propagation")
    class CollaboratorExceptionPropagation {

        @Test
        @DisplayName("Should propagate exception when repository saveAndFlush throws")
        void shouldPropagateException_whenRepositorySaveAndFlushThrows() {
            // Given
            PaymentTutorCreationRequestDTO dto = buildDto();
            PaymentTutorDataModel prototypeModel = new PaymentTutorDataModel();
            when(applicationContext.getBean(PaymentTutorDataModel.class)).thenReturn(prototypeModel);
            doNothing().when(modelMapper).map(dto, prototypeModel, PaymentTutorCreationUseCase.MAP_NAME);
            when(membershipTutorRepository.findById(new MembershipTutorDataModel.MembershipTutorCompositeId(TENANT_ID, MEMBERSHIP_TUTOR_ID)))
                    .thenReturn(Optional.of(new MembershipTutorDataModel()));
            RuntimeException dbException = new RuntimeException("Database connection failed");
            when(paymentRepository.saveAndFlush(prototypeModel)).thenThrow(dbException);

            // When / Then
            assertThatThrownBy(() -> useCase.create(dto))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Database connection failed");

            verify(paymentRepository, times(1)).saveAndFlush(prototypeModel);
            verifyNoMoreInteractions(paymentRepository);
        }
    }

    @Nested
    @DisplayName("Persistence")
    class Persistence {

        @Test
        @DisplayName("Should save transformed model and return mapped DTO")
        void shouldSaveAndReturnDto_whenCreating() {
            // Given
            PaymentTutorCreationRequestDTO dto = buildDto();
            PaymentTutorDataModel prototypeModel = new PaymentTutorDataModel();
            PaymentTutorDataModel savedModel = new PaymentTutorDataModel();
            savedModel.setPaymentTutorId(SAVED_ID);
            PaymentTutorCreationResponseDTO expectedDto = new PaymentTutorCreationResponseDTO();
            expectedDto.setPaymentTutorId(SAVED_ID);

            when(applicationContext.getBean(PaymentTutorDataModel.class)).thenReturn(prototypeModel);
            doNothing().when(modelMapper).map(dto, prototypeModel, PaymentTutorCreationUseCase.MAP_NAME);
            when(membershipTutorRepository.findById(new MembershipTutorDataModel.MembershipTutorCompositeId(TENANT_ID, MEMBERSHIP_TUTOR_ID)))
                    .thenReturn(Optional.of(new MembershipTutorDataModel()));
            when(paymentRepository.saveAndFlush(prototypeModel)).thenReturn(savedModel);
            when(modelMapper.map(savedModel, PaymentTutorCreationResponseDTO.class)).thenReturn(expectedDto);

            // When
            PaymentTutorCreationResponseDTO result = useCase.create(dto);

            // Then
            verify(paymentRepository, times(1)).saveAndFlush(prototypeModel);
            assertThat(result.getPaymentTutorId()).isEqualTo(SAVED_ID);
            verifyNoMoreInteractions(applicationContext, paymentRepository, membershipTutorRepository, modelMapper);
        }

        @Test
        @DisplayName("Should execute operations in correct order")
        void shouldExecuteInOrder_whenCreating() {
            // Given
            PaymentTutorCreationRequestDTO dto = buildDto();
            PaymentTutorDataModel prototypeModel = new PaymentTutorDataModel();
            PaymentTutorDataModel savedModel = new PaymentTutorDataModel();
            PaymentTutorCreationResponseDTO responseDto = new PaymentTutorCreationResponseDTO();

            when(applicationContext.getBean(PaymentTutorDataModel.class)).thenReturn(prototypeModel);
            doNothing().when(modelMapper).map(dto, prototypeModel, PaymentTutorCreationUseCase.MAP_NAME);
            when(membershipTutorRepository.findById(new MembershipTutorDataModel.MembershipTutorCompositeId(TENANT_ID, MEMBERSHIP_TUTOR_ID)))
                    .thenReturn(Optional.of(new MembershipTutorDataModel()));
            when(paymentRepository.saveAndFlush(prototypeModel)).thenReturn(savedModel);
            when(modelMapper.map(savedModel, PaymentTutorCreationResponseDTO.class)).thenReturn(responseDto);

            // When
            useCase.create(dto);

            // Then
            InOrder inOrder = inOrder(applicationContext, modelMapper, membershipTutorRepository, paymentRepository);
            inOrder.verify(applicationContext, times(1)).getBean(PaymentTutorDataModel.class);
            inOrder.verify(modelMapper, times(1)).map(dto, prototypeModel, PaymentTutorCreationUseCase.MAP_NAME);
            inOrder.verify(membershipTutorRepository, times(1)).findById(new MembershipTutorDataModel.MembershipTutorCompositeId(TENANT_ID, MEMBERSHIP_TUTOR_ID));
            inOrder.verify(paymentRepository, times(1)).saveAndFlush(prototypeModel);
            inOrder.verify(modelMapper, times(1)).map(savedModel, PaymentTutorCreationResponseDTO.class);
            inOrder.verifyNoMoreInteractions();
        }
    }
}
