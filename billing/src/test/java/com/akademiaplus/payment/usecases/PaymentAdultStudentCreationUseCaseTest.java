/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.payment.usecases;

import com.akademiaplus.billing.customerpayment.PaymentAdultStudentDataModel;
import com.akademiaplus.billing.membership.MembershipAdultStudentDataModel;
import com.akademiaplus.infra.persistence.config.TenantContextHolder;
import com.akademiaplus.membership.interfaceadapters.MembershipAdultStudentRepository;
import com.akademiaplus.membership.interfaceadapters.PaymentAdultStudentRepository;
import openapi.akademiaplus.domain.billing.dto.PaymentAdultStudentCreationRequestDTO;
import openapi.akademiaplus.domain.billing.dto.PaymentAdultStudentCreationResponseDTO;
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

@DisplayName("PaymentAdultStudentCreationUseCase")
@ExtendWith(MockitoExtension.class)
class PaymentAdultStudentCreationUseCaseTest {

    @Mock private ApplicationContext applicationContext;
    @Mock private PaymentAdultStudentRepository paymentRepository;
    @Mock private MembershipAdultStudentRepository membershipAdultStudentRepository;
    @Mock private TenantContextHolder tenantContextHolder;
    @Mock private ModelMapper modelMapper;

    private PaymentAdultStudentCreationUseCase useCase;

    private static final Long TENANT_ID = 1L;
    private static final LocalDate PAYMENT_DATE = LocalDate.of(2026, 3, 15);
    private static final Double AMOUNT = 500.0;
    private static final String PAYMENT_METHOD = "CASH";
    private static final Long MEMBERSHIP_ADULT_STUDENT_ID = 2L;
    private static final Long SAVED_ID = 1L;

    @BeforeEach
    void setUp() {
        useCase = new PaymentAdultStudentCreationUseCase(
                applicationContext, paymentRepository, membershipAdultStudentRepository, tenantContextHolder, modelMapper);
        lenient().when(tenantContextHolder.getTenantId()).thenReturn(Optional.of(TENANT_ID));
    }

    private PaymentAdultStudentCreationRequestDTO buildDto() {
        PaymentAdultStudentCreationRequestDTO dto = new PaymentAdultStudentCreationRequestDTO();
        dto.setPaymentDate(PAYMENT_DATE);
        dto.setAmount(AMOUNT);
        dto.setPaymentMethod(PAYMENT_METHOD);
        dto.setMembershipAdultStudentId(MEMBERSHIP_ADULT_STUDENT_ID);
        return dto;
    }

    @Nested
    @DisplayName("Transformation")
    class Transformation {

        @Test
        @DisplayName("Should retrieve prototype bean from ApplicationContext")
        void shouldRetrievePrototypeBean_whenTransforming() {
            // Given
            PaymentAdultStudentCreationRequestDTO dto = buildDto();
            PaymentAdultStudentDataModel prototypeModel = new PaymentAdultStudentDataModel();
            when(applicationContext.getBean(PaymentAdultStudentDataModel.class)).thenReturn(prototypeModel);
            when(membershipAdultStudentRepository.findById(new MembershipAdultStudentDataModel.MembershipAdultStudentCompositeId(TENANT_ID, MEMBERSHIP_ADULT_STUDENT_ID)))
                    .thenReturn(Optional.of(new MembershipAdultStudentDataModel()));

            // When
            useCase.transform(dto);

            // Then
            verify(applicationContext, times(1)).getBean(PaymentAdultStudentDataModel.class);
            verifyNoMoreInteractions(applicationContext, paymentRepository);
        }

        @Test
        @DisplayName("Should delegate mapping to ModelMapper with named TypeMap")
        void shouldDelegateToModelMapper_whenTransforming() {
            // Given
            PaymentAdultStudentCreationRequestDTO dto = buildDto();
            PaymentAdultStudentDataModel prototypeModel = new PaymentAdultStudentDataModel();
            when(applicationContext.getBean(PaymentAdultStudentDataModel.class)).thenReturn(prototypeModel);
            when(membershipAdultStudentRepository.findById(new MembershipAdultStudentDataModel.MembershipAdultStudentCompositeId(TENANT_ID, MEMBERSHIP_ADULT_STUDENT_ID)))
                    .thenReturn(Optional.of(new MembershipAdultStudentDataModel()));

            // When
            PaymentAdultStudentDataModel result = useCase.transform(dto);

            // Then
            verify(modelMapper, times(1)).map(dto, prototypeModel, PaymentAdultStudentCreationUseCase.MAP_NAME);
            assertThat(result).isSameAs(prototypeModel);
            verifyNoMoreInteractions(applicationContext, paymentRepository);
        }

        @Test
        @DisplayName("Should resolve membershipAdultStudent FK via repository lookup")
        void shouldResolveFK_whenTransforming() {
            // Given
            PaymentAdultStudentCreationRequestDTO dto = buildDto();
            PaymentAdultStudentDataModel prototypeModel = new PaymentAdultStudentDataModel();
            MembershipAdultStudentDataModel membershipAdultStudent = new MembershipAdultStudentDataModel();
            when(applicationContext.getBean(PaymentAdultStudentDataModel.class)).thenReturn(prototypeModel);
            when(membershipAdultStudentRepository.findById(new MembershipAdultStudentDataModel.MembershipAdultStudentCompositeId(TENANT_ID, MEMBERSHIP_ADULT_STUDENT_ID)))
                    .thenReturn(Optional.of(membershipAdultStudent));

            // When
            PaymentAdultStudentDataModel result = useCase.transform(dto);

            // Then
            assertThat(result.getMembershipAdultStudent()).isSameAs(membershipAdultStudent);
            verifyNoMoreInteractions(applicationContext, paymentRepository);
        }

        @Test
        @DisplayName("Should throw when membershipAdultStudent is not found")
        void shouldThrow_whenMembershipAdultStudentNotFound() {
            // Given
            PaymentAdultStudentCreationRequestDTO dto = buildDto();
            PaymentAdultStudentDataModel prototypeModel = new PaymentAdultStudentDataModel();
            when(applicationContext.getBean(PaymentAdultStudentDataModel.class)).thenReturn(prototypeModel);
            when(membershipAdultStudentRepository.findById(new MembershipAdultStudentDataModel.MembershipAdultStudentCompositeId(TENANT_ID, MEMBERSHIP_ADULT_STUDENT_ID)))
                    .thenReturn(Optional.empty());

            // When / Then
            assertThatThrownBy(() -> useCase.transform(dto))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage(PaymentAdultStudentCreationUseCase.ERROR_MEMBERSHIP_ADULT_STUDENT_NOT_FOUND + MEMBERSHIP_ADULT_STUDENT_ID);

            verify(applicationContext, times(1)).getBean(PaymentAdultStudentDataModel.class);
            verify(membershipAdultStudentRepository, times(1)).findById(new MembershipAdultStudentDataModel.MembershipAdultStudentCompositeId(TENANT_ID, MEMBERSHIP_ADULT_STUDENT_ID));
            verifyNoInteractions(paymentRepository);
            verifyNoMoreInteractions(applicationContext, membershipAdultStudentRepository);
        }

        @Test
        @DisplayName("Should throw when tenant context is missing")
        void shouldThrow_whenTenantContextMissing() {
            // Given
            PaymentAdultStudentCreationRequestDTO dto = buildDto();
            PaymentAdultStudentDataModel prototypeModel = new PaymentAdultStudentDataModel();
            when(applicationContext.getBean(PaymentAdultStudentDataModel.class)).thenReturn(prototypeModel);
            when(tenantContextHolder.getTenantId()).thenReturn(Optional.empty());

            // When / Then
            assertThatThrownBy(() -> useCase.transform(dto))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage(PaymentAdultStudentCreationUseCase.ERROR_TENANT_CONTEXT_REQUIRED);

            verify(applicationContext, times(1)).getBean(PaymentAdultStudentDataModel.class);
            verify(tenantContextHolder, times(1)).getTenantId();
            verifyNoInteractions(paymentRepository);
            verifyNoMoreInteractions(applicationContext, membershipAdultStudentRepository);
        }
    }

    @Nested
    @DisplayName("Collaborator exception propagation")
    class CollaboratorExceptionPropagation {

        @Test
        @DisplayName("Should propagate exception when repository saveAndFlush throws")
        void shouldPropagateException_whenRepositorySaveAndFlushThrows() {
            // Given
            PaymentAdultStudentCreationRequestDTO dto = buildDto();
            PaymentAdultStudentDataModel prototypeModel = new PaymentAdultStudentDataModel();
            when(applicationContext.getBean(PaymentAdultStudentDataModel.class)).thenReturn(prototypeModel);
            doNothing().when(modelMapper).map(dto, prototypeModel, PaymentAdultStudentCreationUseCase.MAP_NAME);
            when(membershipAdultStudentRepository.findById(new MembershipAdultStudentDataModel.MembershipAdultStudentCompositeId(TENANT_ID, MEMBERSHIP_ADULT_STUDENT_ID)))
                    .thenReturn(Optional.of(new MembershipAdultStudentDataModel()));
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
            PaymentAdultStudentCreationRequestDTO dto = buildDto();
            PaymentAdultStudentDataModel prototypeModel = new PaymentAdultStudentDataModel();
            PaymentAdultStudentDataModel savedModel = new PaymentAdultStudentDataModel();
            savedModel.setPaymentAdultStudentId(SAVED_ID);
            PaymentAdultStudentCreationResponseDTO expectedDto = new PaymentAdultStudentCreationResponseDTO();
            expectedDto.setPaymentAdultStudentId(SAVED_ID);

            when(applicationContext.getBean(PaymentAdultStudentDataModel.class)).thenReturn(prototypeModel);
            doNothing().when(modelMapper).map(dto, prototypeModel, PaymentAdultStudentCreationUseCase.MAP_NAME);
            when(membershipAdultStudentRepository.findById(new MembershipAdultStudentDataModel.MembershipAdultStudentCompositeId(TENANT_ID, MEMBERSHIP_ADULT_STUDENT_ID)))
                    .thenReturn(Optional.of(new MembershipAdultStudentDataModel()));
            when(paymentRepository.saveAndFlush(prototypeModel)).thenReturn(savedModel);
            when(modelMapper.map(savedModel, PaymentAdultStudentCreationResponseDTO.class)).thenReturn(expectedDto);

            // When
            PaymentAdultStudentCreationResponseDTO result = useCase.create(dto);

            // Then
            verify(paymentRepository, times(1)).saveAndFlush(prototypeModel);
            assertThat(result.getPaymentAdultStudentId()).isEqualTo(SAVED_ID);
            verifyNoMoreInteractions(applicationContext, paymentRepository, membershipAdultStudentRepository, modelMapper);
        }

        @Test
        @DisplayName("Should execute operations in correct order")
        void shouldExecuteInOrder_whenCreating() {
            // Given
            PaymentAdultStudentCreationRequestDTO dto = buildDto();
            PaymentAdultStudentDataModel prototypeModel = new PaymentAdultStudentDataModel();
            PaymentAdultStudentDataModel savedModel = new PaymentAdultStudentDataModel();
            PaymentAdultStudentCreationResponseDTO responseDto = new PaymentAdultStudentCreationResponseDTO();

            when(applicationContext.getBean(PaymentAdultStudentDataModel.class)).thenReturn(prototypeModel);
            doNothing().when(modelMapper).map(dto, prototypeModel, PaymentAdultStudentCreationUseCase.MAP_NAME);
            when(membershipAdultStudentRepository.findById(new MembershipAdultStudentDataModel.MembershipAdultStudentCompositeId(TENANT_ID, MEMBERSHIP_ADULT_STUDENT_ID)))
                    .thenReturn(Optional.of(new MembershipAdultStudentDataModel()));
            when(paymentRepository.saveAndFlush(prototypeModel)).thenReturn(savedModel);
            when(modelMapper.map(savedModel, PaymentAdultStudentCreationResponseDTO.class)).thenReturn(responseDto);

            // When
            useCase.create(dto);

            // Then
            InOrder inOrder = inOrder(applicationContext, modelMapper, membershipAdultStudentRepository, paymentRepository);
            inOrder.verify(applicationContext, times(1)).getBean(PaymentAdultStudentDataModel.class);
            inOrder.verify(modelMapper, times(1)).map(dto, prototypeModel, PaymentAdultStudentCreationUseCase.MAP_NAME);
            inOrder.verify(membershipAdultStudentRepository, times(1)).findById(new MembershipAdultStudentDataModel.MembershipAdultStudentCompositeId(TENANT_ID, MEMBERSHIP_ADULT_STUDENT_ID));
            inOrder.verify(paymentRepository, times(1)).saveAndFlush(prototypeModel);
            inOrder.verify(modelMapper, times(1)).map(savedModel, PaymentAdultStudentCreationResponseDTO.class);
            inOrder.verifyNoMoreInteractions();
        }
    }
}
