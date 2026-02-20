/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.membership.usecases;

import com.akademiaplus.billing.membership.MembershipDataModel;
import com.akademiaplus.billing.membership.MembershipTutorDataModel;
import com.akademiaplus.courses.program.CourseDataModel;
import com.akademiaplus.customer.interfaceadapters.TutorRepository;
import com.akademiaplus.infra.persistence.config.TenantContextHolder;
import com.akademiaplus.membership.interfaceadapters.MembershipRepository;
import com.akademiaplus.membership.interfaceadapters.MembershipTutorRepository;
import com.akademiaplus.program.interfaceadapters.CourseRepository;
import com.akademiaplus.users.customer.TutorDataModel;
import openapi.akademiaplus.domain.billing.dto.MembershipTutorCreationRequestDTO;
import openapi.akademiaplus.domain.billing.dto.MembershipTutorCreationResponseDTO;
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

@DisplayName("MembershipTutorCreationUseCase")
@ExtendWith(MockitoExtension.class)
class MembershipTutorCreationUseCaseTest {

    @Mock private ApplicationContext applicationContext;
    @Mock private MembershipTutorRepository repository;
    @Mock private MembershipRepository membershipRepository;
    @Mock private CourseRepository courseRepository;
    @Mock private TutorRepository tutorRepository;
    @Mock private TenantContextHolder tenantContextHolder;
    @Mock private ModelMapper modelMapper;

    private MembershipTutorCreationUseCase useCase;

    private static final Long TENANT_ID = 1L;
    private static final LocalDate START_DATE = LocalDate.of(2026, 3, 1);
    private static final LocalDate DUE_DATE = LocalDate.of(2026, 3, 31);
    private static final Long MEMBERSHIP_ID = 2L;
    private static final Long COURSE_ID = 3L;
    private static final Long TUTOR_ID = 4L;
    private static final Long SAVED_ID = 1L;

    @BeforeEach
    void setUp() {
        useCase = new MembershipTutorCreationUseCase(
                applicationContext, repository, membershipRepository,
                courseRepository, tutorRepository, tenantContextHolder, modelMapper);
        lenient().when(tenantContextHolder.getTenantId()).thenReturn(Optional.of(TENANT_ID));
    }

    private MembershipTutorCreationRequestDTO buildDto() {
        MembershipTutorCreationRequestDTO dto = new MembershipTutorCreationRequestDTO();
        dto.setStartDate(START_DATE);
        dto.setDueDate(DUE_DATE);
        dto.setMembershipId(MEMBERSHIP_ID);
        dto.setCourseId(COURSE_ID);
        dto.setTutorId(TUTOR_ID);
        return dto;
    }

    @Nested
    @DisplayName("Transformation")
    class Transformation {

        @Test
        @DisplayName("Should retrieve prototype bean from ApplicationContext")
        void shouldRetrievePrototypeBean_whenTransforming() {
            // Given
            MembershipTutorCreationRequestDTO dto = buildDto();
            MembershipTutorDataModel prototypeModel = new MembershipTutorDataModel();
            when(applicationContext.getBean(MembershipTutorDataModel.class)).thenReturn(prototypeModel);
            when(membershipRepository.findById(new MembershipDataModel.MembershipCompositeId(TENANT_ID, MEMBERSHIP_ID))).thenReturn(Optional.of(new MembershipDataModel()));
            when(courseRepository.findById(new CourseDataModel.CourseCompositeId(TENANT_ID, COURSE_ID))).thenReturn(Optional.of(new CourseDataModel()));
            when(tutorRepository.findById(new TutorDataModel.TutorCompositeId(TENANT_ID, TUTOR_ID))).thenReturn(Optional.of(new TutorDataModel()));

            // When
            useCase.transform(dto);

            // Then
            verify(applicationContext).getBean(MembershipTutorDataModel.class);
        }

        @Test
        @DisplayName("Should delegate mapping to ModelMapper with named TypeMap")
        void shouldDelegateToModelMapper_whenTransforming() {
            // Given
            MembershipTutorCreationRequestDTO dto = buildDto();
            MembershipTutorDataModel prototypeModel = new MembershipTutorDataModel();
            when(applicationContext.getBean(MembershipTutorDataModel.class)).thenReturn(prototypeModel);
            when(membershipRepository.findById(new MembershipDataModel.MembershipCompositeId(TENANT_ID, MEMBERSHIP_ID))).thenReturn(Optional.of(new MembershipDataModel()));
            when(courseRepository.findById(new CourseDataModel.CourseCompositeId(TENANT_ID, COURSE_ID))).thenReturn(Optional.of(new CourseDataModel()));
            when(tutorRepository.findById(new TutorDataModel.TutorCompositeId(TENANT_ID, TUTOR_ID))).thenReturn(Optional.of(new TutorDataModel()));

            // When
            MembershipTutorDataModel result = useCase.transform(dto);

            // Then
            verify(modelMapper).map(dto, prototypeModel, MembershipTutorCreationUseCase.MAP_NAME);
            assertThat(result).isSameAs(prototypeModel);
        }

        @Test
        @DisplayName("Should resolve all FK relationships via repository lookups")
        void shouldResolveFKs_whenTransforming() {
            // Given
            MembershipTutorCreationRequestDTO dto = buildDto();
            MembershipTutorDataModel prototypeModel = new MembershipTutorDataModel();
            MembershipDataModel membership = new MembershipDataModel();
            CourseDataModel course = new CourseDataModel();
            TutorDataModel tutor = new TutorDataModel();
            when(applicationContext.getBean(MembershipTutorDataModel.class)).thenReturn(prototypeModel);
            when(membershipRepository.findById(new MembershipDataModel.MembershipCompositeId(TENANT_ID, MEMBERSHIP_ID))).thenReturn(Optional.of(membership));
            when(courseRepository.findById(new CourseDataModel.CourseCompositeId(TENANT_ID, COURSE_ID))).thenReturn(Optional.of(course));
            when(tutorRepository.findById(new TutorDataModel.TutorCompositeId(TENANT_ID, TUTOR_ID))).thenReturn(Optional.of(tutor));

            // When
            MembershipTutorDataModel result = useCase.transform(dto);

            // Then
            assertThat(result.getMembership()).isSameAs(membership);
            assertThat(result.getCourse()).isSameAs(course);
            assertThat(result.getTutor()).isSameAs(tutor);
        }

        @Test
        @DisplayName("Should throw when membership is not found")
        void shouldThrow_whenMembershipNotFound() {
            // Given
            MembershipTutorCreationRequestDTO dto = buildDto();
            MembershipTutorDataModel prototypeModel = new MembershipTutorDataModel();
            when(applicationContext.getBean(MembershipTutorDataModel.class)).thenReturn(prototypeModel);
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
            MembershipTutorCreationRequestDTO dto = buildDto();
            MembershipTutorDataModel prototypeModel = new MembershipTutorDataModel();
            MembershipTutorDataModel savedModel = new MembershipTutorDataModel();
            savedModel.setMembershipTutorId(SAVED_ID);
            MembershipTutorCreationResponseDTO expectedDto = new MembershipTutorCreationResponseDTO();
            expectedDto.setMembershipTutorId(SAVED_ID);

            when(applicationContext.getBean(MembershipTutorDataModel.class)).thenReturn(prototypeModel);
            doNothing().when(modelMapper).map(dto, prototypeModel, MembershipTutorCreationUseCase.MAP_NAME);
            when(membershipRepository.findById(new MembershipDataModel.MembershipCompositeId(TENANT_ID, MEMBERSHIP_ID))).thenReturn(Optional.of(new MembershipDataModel()));
            when(courseRepository.findById(new CourseDataModel.CourseCompositeId(TENANT_ID, COURSE_ID))).thenReturn(Optional.of(new CourseDataModel()));
            when(tutorRepository.findById(new TutorDataModel.TutorCompositeId(TENANT_ID, TUTOR_ID))).thenReturn(Optional.of(new TutorDataModel()));
            when(repository.save(prototypeModel)).thenReturn(savedModel);
            when(modelMapper.map(savedModel, MembershipTutorCreationResponseDTO.class)).thenReturn(expectedDto);

            // When
            MembershipTutorCreationResponseDTO result = useCase.create(dto);

            // Then
            verify(repository).save(prototypeModel);
            assertThat(result.getMembershipTutorId()).isEqualTo(SAVED_ID);
        }

        @Test
        @DisplayName("Should execute operations in correct order")
        void shouldExecuteInOrder_whenCreating() {
            // Given
            MembershipTutorCreationRequestDTO dto = buildDto();
            MembershipTutorDataModel prototypeModel = new MembershipTutorDataModel();
            MembershipTutorDataModel savedModel = new MembershipTutorDataModel();
            MembershipTutorCreationResponseDTO responseDto = new MembershipTutorCreationResponseDTO();

            when(applicationContext.getBean(MembershipTutorDataModel.class)).thenReturn(prototypeModel);
            doNothing().when(modelMapper).map(dto, prototypeModel, MembershipTutorCreationUseCase.MAP_NAME);
            when(membershipRepository.findById(new MembershipDataModel.MembershipCompositeId(TENANT_ID, MEMBERSHIP_ID))).thenReturn(Optional.of(new MembershipDataModel()));
            when(courseRepository.findById(new CourseDataModel.CourseCompositeId(TENANT_ID, COURSE_ID))).thenReturn(Optional.of(new CourseDataModel()));
            when(tutorRepository.findById(new TutorDataModel.TutorCompositeId(TENANT_ID, TUTOR_ID))).thenReturn(Optional.of(new TutorDataModel()));
            when(repository.save(prototypeModel)).thenReturn(savedModel);
            when(modelMapper.map(savedModel, MembershipTutorCreationResponseDTO.class)).thenReturn(responseDto);

            // When
            useCase.create(dto);

            // Then
            InOrder inOrder = inOrder(applicationContext, modelMapper, membershipRepository, courseRepository, tutorRepository, repository);
            inOrder.verify(applicationContext).getBean(MembershipTutorDataModel.class);
            inOrder.verify(modelMapper).map(dto, prototypeModel, MembershipTutorCreationUseCase.MAP_NAME);
            inOrder.verify(membershipRepository).findById(new MembershipDataModel.MembershipCompositeId(TENANT_ID, MEMBERSHIP_ID));
            inOrder.verify(courseRepository).findById(new CourseDataModel.CourseCompositeId(TENANT_ID, COURSE_ID));
            inOrder.verify(tutorRepository).findById(new TutorDataModel.TutorCompositeId(TENANT_ID, TUTOR_ID));
            inOrder.verify(repository).save(prototypeModel);
            inOrder.verify(modelMapper).map(savedModel, MembershipTutorCreationResponseDTO.class);
        }
    }
}
