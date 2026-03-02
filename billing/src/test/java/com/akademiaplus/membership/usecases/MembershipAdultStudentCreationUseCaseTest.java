/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.membership.usecases;

import com.akademiaplus.billing.membership.MembershipAdultStudentDataModel;
import com.akademiaplus.billing.membership.MembershipDataModel;
import com.akademiaplus.courses.program.CourseDataModel;
import com.akademiaplus.customer.adultstudent.interfaceadapters.AdultStudentRepository;
import com.akademiaplus.infra.persistence.config.TenantContextHolder;
import com.akademiaplus.membership.interfaceadapters.MembershipAdultStudentRepository;
import com.akademiaplus.membership.interfaceadapters.MembershipRepository;
import com.akademiaplus.program.interfaceadapters.CourseRepository;
import com.akademiaplus.users.customer.AdultStudentDataModel;
import openapi.akademiaplus.domain.billing.dto.MembershipAdultStudentCreationRequestDTO;
import openapi.akademiaplus.domain.billing.dto.MembershipAdultStudentCreationResponseDTO;
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

@DisplayName("MembershipAdultStudentCreationUseCase")
@ExtendWith(MockitoExtension.class)
class MembershipAdultStudentCreationUseCaseTest {

    @Mock private ApplicationContext applicationContext;
    @Mock private MembershipAdultStudentRepository repository;
    @Mock private MembershipRepository membershipRepository;
    @Mock private CourseRepository courseRepository;
    @Mock private AdultStudentRepository adultStudentRepository;
    @Mock private TenantContextHolder tenantContextHolder;
    @Mock private ModelMapper modelMapper;

    private MembershipAdultStudentCreationUseCase useCase;

    private static final Long TENANT_ID = 1L;
    private static final LocalDate START_DATE = LocalDate.of(2026, 3, 1);
    private static final LocalDate DUE_DATE = LocalDate.of(2026, 3, 31);
    private static final Long MEMBERSHIP_ID = 2L;
    private static final Long COURSE_ID = 3L;
    private static final Long ADULT_STUDENT_ID = 4L;
    private static final Long SAVED_ID = 1L;

    @BeforeEach
    void setUp() {
        useCase = new MembershipAdultStudentCreationUseCase(
                applicationContext, repository, membershipRepository,
                courseRepository, adultStudentRepository, tenantContextHolder, modelMapper);
        lenient().when(tenantContextHolder.getTenantId()).thenReturn(Optional.of(TENANT_ID));
    }

    private MembershipAdultStudentCreationRequestDTO buildDto() {
        MembershipAdultStudentCreationRequestDTO dto = new MembershipAdultStudentCreationRequestDTO();
        dto.setStartDate(START_DATE);
        dto.setDueDate(DUE_DATE);
        dto.setMembershipId(MEMBERSHIP_ID);
        dto.setCourseId(COURSE_ID);
        dto.setAdultStudentId(ADULT_STUDENT_ID);
        return dto;
    }

    @Nested
    @DisplayName("Transformation")
    class Transformation {

        @Test
        @DisplayName("Should retrieve prototype bean from ApplicationContext")
        void shouldRetrievePrototypeBean_whenTransforming() {
            // Given
            MembershipAdultStudentCreationRequestDTO dto = buildDto();
            MembershipAdultStudentDataModel prototypeModel = new MembershipAdultStudentDataModel();
            when(applicationContext.getBean(MembershipAdultStudentDataModel.class)).thenReturn(prototypeModel);
            when(membershipRepository.findById(new MembershipDataModel.MembershipCompositeId(TENANT_ID, MEMBERSHIP_ID))).thenReturn(Optional.of(new MembershipDataModel()));
            when(courseRepository.findById(new CourseDataModel.CourseCompositeId(TENANT_ID, COURSE_ID))).thenReturn(Optional.of(new CourseDataModel()));
            when(adultStudentRepository.findById(new AdultStudentDataModel.AdultStudentCompositeId(TENANT_ID, ADULT_STUDENT_ID))).thenReturn(Optional.of(new AdultStudentDataModel()));

            // When
            useCase.transform(dto);

            // Then
            verify(applicationContext).getBean(MembershipAdultStudentDataModel.class);
        }

        @Test
        @DisplayName("Should delegate mapping to ModelMapper with named TypeMap")
        void shouldDelegateToModelMapper_whenTransforming() {
            // Given
            MembershipAdultStudentCreationRequestDTO dto = buildDto();
            MembershipAdultStudentDataModel prototypeModel = new MembershipAdultStudentDataModel();
            when(applicationContext.getBean(MembershipAdultStudentDataModel.class)).thenReturn(prototypeModel);
            when(membershipRepository.findById(new MembershipDataModel.MembershipCompositeId(TENANT_ID, MEMBERSHIP_ID))).thenReturn(Optional.of(new MembershipDataModel()));
            when(courseRepository.findById(new CourseDataModel.CourseCompositeId(TENANT_ID, COURSE_ID))).thenReturn(Optional.of(new CourseDataModel()));
            when(adultStudentRepository.findById(new AdultStudentDataModel.AdultStudentCompositeId(TENANT_ID, ADULT_STUDENT_ID))).thenReturn(Optional.of(new AdultStudentDataModel()));

            // When
            MembershipAdultStudentDataModel result = useCase.transform(dto);

            // Then
            verify(modelMapper).map(dto, prototypeModel, MembershipAdultStudentCreationUseCase.MAP_NAME);
            assertThat(result).isSameAs(prototypeModel);
        }

        @Test
        @DisplayName("Should resolve all FK relationships via repository lookups")
        void shouldResolveFKs_whenTransforming() {
            // Given
            MembershipAdultStudentCreationRequestDTO dto = buildDto();
            MembershipAdultStudentDataModel prototypeModel = new MembershipAdultStudentDataModel();
            MembershipDataModel membership = new MembershipDataModel();
            CourseDataModel course = new CourseDataModel();
            AdultStudentDataModel adultStudent = new AdultStudentDataModel();
            when(applicationContext.getBean(MembershipAdultStudentDataModel.class)).thenReturn(prototypeModel);
            when(membershipRepository.findById(new MembershipDataModel.MembershipCompositeId(TENANT_ID, MEMBERSHIP_ID))).thenReturn(Optional.of(membership));
            when(courseRepository.findById(new CourseDataModel.CourseCompositeId(TENANT_ID, COURSE_ID))).thenReturn(Optional.of(course));
            when(adultStudentRepository.findById(new AdultStudentDataModel.AdultStudentCompositeId(TENANT_ID, ADULT_STUDENT_ID))).thenReturn(Optional.of(adultStudent));

            // When
            MembershipAdultStudentDataModel result = useCase.transform(dto);

            // Then
            assertThat(result.getMembership()).isSameAs(membership);
            assertThat(result.getCourse()).isSameAs(course);
            assertThat(result.getAdultStudent()).isSameAs(adultStudent);
        }

        @Test
        @DisplayName("Should throw when membership is not found")
        void shouldThrow_whenMembershipNotFound() {
            // Given
            MembershipAdultStudentCreationRequestDTO dto = buildDto();
            MembershipAdultStudentDataModel prototypeModel = new MembershipAdultStudentDataModel();
            when(applicationContext.getBean(MembershipAdultStudentDataModel.class)).thenReturn(prototypeModel);
            when(membershipRepository.findById(new MembershipDataModel.MembershipCompositeId(TENANT_ID, MEMBERSHIP_ID))).thenReturn(Optional.empty());

            // When / Then
            assertThatThrownBy(() -> useCase.transform(dto))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining(String.valueOf(MEMBERSHIP_ID));
        }
    }

    @Nested
    @DisplayName("Persistence")
    class Persistence {

        @Test
        @DisplayName("Should save transformed model and return mapped DTO")
        void shouldSaveAndReturnDto_whenCreating() {
            // Given
            MembershipAdultStudentCreationRequestDTO dto = buildDto();
            MembershipAdultStudentDataModel prototypeModel = new MembershipAdultStudentDataModel();
            MembershipAdultStudentDataModel savedModel = new MembershipAdultStudentDataModel();
            savedModel.setMembershipAdultStudentId(SAVED_ID);
            MembershipAdultStudentCreationResponseDTO expectedDto = new MembershipAdultStudentCreationResponseDTO();
            expectedDto.setMembershipAdultStudentId(SAVED_ID);

            when(applicationContext.getBean(MembershipAdultStudentDataModel.class)).thenReturn(prototypeModel);
            doNothing().when(modelMapper).map(dto, prototypeModel, MembershipAdultStudentCreationUseCase.MAP_NAME);
            when(membershipRepository.findById(new MembershipDataModel.MembershipCompositeId(TENANT_ID, MEMBERSHIP_ID))).thenReturn(Optional.of(new MembershipDataModel()));
            when(courseRepository.findById(new CourseDataModel.CourseCompositeId(TENANT_ID, COURSE_ID))).thenReturn(Optional.of(new CourseDataModel()));
            when(adultStudentRepository.findById(new AdultStudentDataModel.AdultStudentCompositeId(TENANT_ID, ADULT_STUDENT_ID))).thenReturn(Optional.of(new AdultStudentDataModel()));
            when(repository.saveAndFlush(prototypeModel)).thenReturn(savedModel);
            when(modelMapper.map(savedModel, MembershipAdultStudentCreationResponseDTO.class)).thenReturn(expectedDto);

            // When
            MembershipAdultStudentCreationResponseDTO result = useCase.create(dto);

            // Then
            verify(repository).saveAndFlush(prototypeModel);
            assertThat(result.getMembershipAdultStudentId()).isEqualTo(SAVED_ID);
        }

        @Test
        @DisplayName("Should execute operations in correct order")
        void shouldExecuteInOrder_whenCreating() {
            // Given
            MembershipAdultStudentCreationRequestDTO dto = buildDto();
            MembershipAdultStudentDataModel prototypeModel = new MembershipAdultStudentDataModel();
            MembershipAdultStudentDataModel savedModel = new MembershipAdultStudentDataModel();
            MembershipAdultStudentCreationResponseDTO responseDto = new MembershipAdultStudentCreationResponseDTO();

            when(applicationContext.getBean(MembershipAdultStudentDataModel.class)).thenReturn(prototypeModel);
            doNothing().when(modelMapper).map(dto, prototypeModel, MembershipAdultStudentCreationUseCase.MAP_NAME);
            when(membershipRepository.findById(new MembershipDataModel.MembershipCompositeId(TENANT_ID, MEMBERSHIP_ID))).thenReturn(Optional.of(new MembershipDataModel()));
            when(courseRepository.findById(new CourseDataModel.CourseCompositeId(TENANT_ID, COURSE_ID))).thenReturn(Optional.of(new CourseDataModel()));
            when(adultStudentRepository.findById(new AdultStudentDataModel.AdultStudentCompositeId(TENANT_ID, ADULT_STUDENT_ID))).thenReturn(Optional.of(new AdultStudentDataModel()));
            when(repository.saveAndFlush(prototypeModel)).thenReturn(savedModel);
            when(modelMapper.map(savedModel, MembershipAdultStudentCreationResponseDTO.class)).thenReturn(responseDto);

            // When
            useCase.create(dto);

            // Then
            InOrder inOrder = inOrder(applicationContext, modelMapper, membershipRepository, courseRepository, adultStudentRepository, repository);
            inOrder.verify(applicationContext).getBean(MembershipAdultStudentDataModel.class);
            inOrder.verify(modelMapper).map(dto, prototypeModel, MembershipAdultStudentCreationUseCase.MAP_NAME);
            inOrder.verify(membershipRepository).findById(new MembershipDataModel.MembershipCompositeId(TENANT_ID, MEMBERSHIP_ID));
            inOrder.verify(courseRepository).findById(new CourseDataModel.CourseCompositeId(TENANT_ID, COURSE_ID));
            inOrder.verify(adultStudentRepository).findById(new AdultStudentDataModel.AdultStudentCompositeId(TENANT_ID, ADULT_STUDENT_ID));
            inOrder.verify(repository).saveAndFlush(prototypeModel);
            inOrder.verify(modelMapper).map(savedModel, MembershipAdultStudentCreationResponseDTO.class);
        }
    }
}
