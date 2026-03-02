/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.event.usecases;

import com.akademiaplus.collaborator.interfaceadapters.CollaboratorRepository;
import com.akademiaplus.courses.event.CourseEventDataModel;
import com.akademiaplus.courses.program.CourseDataModel;
import com.akademiaplus.courses.program.ScheduleDataModel;
import com.akademiaplus.event.interfaceadapters.CourseEventRepository;
import com.akademiaplus.infra.persistence.config.TenantContextHolder;
import com.akademiaplus.program.interfaceadapters.CourseRepository;
import com.akademiaplus.program.interfaceadapters.ScheduleRepository;
import com.akademiaplus.users.collaborator.CollaboratorDataModel;
import openapi.akademiaplus.domain.course.management.dto.CourseEventCreateRequestDTO;
import openapi.akademiaplus.domain.course.management.dto.CourseEventCreateResponseDTO;
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
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@DisplayName("CourseEventCreationUseCase")
@ExtendWith(MockitoExtension.class)
class CourseEventCreationUseCaseTest {

    @Mock private ApplicationContext applicationContext;
    @Mock private CourseEventRepository courseEventRepository;
    @Mock private CourseRepository courseRepository;
    @Mock private ScheduleRepository scheduleRepository;
    @Mock private CollaboratorRepository collaboratorRepository;
    @Mock private TenantContextHolder tenantContextHolder;
    @Mock private ModelMapper modelMapper;

    private CourseEventCreationUseCase useCase;

    private static final Long TENANT_ID = 1L;
    private static final LocalDate EVENT_DATE = LocalDate.of(2026, 4, 15);
    private static final String EVENT_TITLE = "Lesson 1";
    private static final String EVENT_DESCRIPTION = "Introduction to the course";
    private static final Long SCHEDULE_ID = 3L;
    private static final Long COURSE_ID = 5L;
    private static final Long INSTRUCTOR_ID = 7L;
    private static final Long SAVED_ID = 1L;

    @BeforeEach
    void setUp() {
        useCase = new CourseEventCreationUseCase(
                applicationContext,
                courseEventRepository,
                courseRepository,
                scheduleRepository,
                collaboratorRepository,
                tenantContextHolder,
                modelMapper
        );
        lenient().when(tenantContextHolder.getTenantId()).thenReturn(Optional.of(TENANT_ID));
    }

    private CourseEventCreateRequestDTO buildDto() {
        CourseEventCreateRequestDTO dto = new CourseEventCreateRequestDTO();
        dto.setDate(EVENT_DATE);
        dto.setTitle(EVENT_TITLE);
        dto.setDescription(EVENT_DESCRIPTION);
        dto.setScheduleId(SCHEDULE_ID);
        dto.setCourseId(COURSE_ID);
        dto.setInstructorId(INSTRUCTOR_ID);
        dto.setAdultAttendeeIds(List.of());
        dto.setMinorAttendeeIds(List.of());
        return dto;
    }

    @Nested
    @DisplayName("Transformation")
    class Transformation {

        @Test
        @DisplayName("Should retrieve prototype CourseEventDataModel from ApplicationContext")
        void shouldRetrievePrototypeBean_whenTransforming() {
            // Given
            CourseEventCreateRequestDTO dto = buildDto();
            CourseEventDataModel prototypeModel = new CourseEventDataModel();
            when(applicationContext.getBean(CourseEventDataModel.class)).thenReturn(prototypeModel);
            when(courseRepository.findById(new CourseDataModel.CourseCompositeId(TENANT_ID, COURSE_ID))).thenReturn(Optional.of(new CourseDataModel()));
            when(collaboratorRepository.findById(new CollaboratorDataModel.CollaboratorCompositeId(TENANT_ID, INSTRUCTOR_ID))).thenReturn(Optional.of(new CollaboratorDataModel()));
            when(scheduleRepository.findById(new ScheduleDataModel.ScheduleCompositeId(TENANT_ID, SCHEDULE_ID))).thenReturn(Optional.of(new ScheduleDataModel()));

            // When
            useCase.transform(dto);

            // Then
            verify(applicationContext).getBean(CourseEventDataModel.class);
        }

        @Test
        @DisplayName("Should delegate mapping to ModelMapper with named TypeMap")
        void shouldDelegateToModelMapper_whenTransforming() {
            // Given
            CourseEventCreateRequestDTO dto = buildDto();
            CourseEventDataModel prototypeModel = new CourseEventDataModel();
            when(applicationContext.getBean(CourseEventDataModel.class)).thenReturn(prototypeModel);
            when(courseRepository.findById(new CourseDataModel.CourseCompositeId(TENANT_ID, COURSE_ID))).thenReturn(Optional.of(new CourseDataModel()));
            when(collaboratorRepository.findById(new CollaboratorDataModel.CollaboratorCompositeId(TENANT_ID, INSTRUCTOR_ID))).thenReturn(Optional.of(new CollaboratorDataModel()));
            when(scheduleRepository.findById(new ScheduleDataModel.ScheduleCompositeId(TENANT_ID, SCHEDULE_ID))).thenReturn(Optional.of(new ScheduleDataModel()));

            // When
            CourseEventDataModel result = useCase.transform(dto);

            // Then
            verify(modelMapper).map(dto, prototypeModel, CourseEventCreationUseCase.MAP_NAME);
            assertThat(result).isSameAs(prototypeModel);
        }

        @Test
        @DisplayName("Should resolve course FK via repository lookup")
        void shouldResolveCourseFK_whenTransforming() {
            // Given
            CourseEventCreateRequestDTO dto = buildDto();
            CourseEventDataModel prototypeModel = new CourseEventDataModel();
            CourseDataModel course = new CourseDataModel();
            when(applicationContext.getBean(CourseEventDataModel.class)).thenReturn(prototypeModel);
            when(courseRepository.findById(new CourseDataModel.CourseCompositeId(TENANT_ID, COURSE_ID))).thenReturn(Optional.of(course));
            when(collaboratorRepository.findById(new CollaboratorDataModel.CollaboratorCompositeId(TENANT_ID, INSTRUCTOR_ID))).thenReturn(Optional.of(new CollaboratorDataModel()));
            when(scheduleRepository.findById(new ScheduleDataModel.ScheduleCompositeId(TENANT_ID, SCHEDULE_ID))).thenReturn(Optional.of(new ScheduleDataModel()));

            // When
            CourseEventDataModel result = useCase.transform(dto);

            // Then
            verify(courseRepository).findById(new CourseDataModel.CourseCompositeId(TENANT_ID, COURSE_ID));
            assertThat(result.getCourse()).isSameAs(course);
        }

        @Test
        @DisplayName("Should resolve collaborator FK via repository lookup")
        void shouldResolveCollaboratorFK_whenTransforming() {
            // Given
            CourseEventCreateRequestDTO dto = buildDto();
            CourseEventDataModel prototypeModel = new CourseEventDataModel();
            CollaboratorDataModel collaborator = new CollaboratorDataModel();
            when(applicationContext.getBean(CourseEventDataModel.class)).thenReturn(prototypeModel);
            when(courseRepository.findById(new CourseDataModel.CourseCompositeId(TENANT_ID, COURSE_ID))).thenReturn(Optional.of(new CourseDataModel()));
            when(collaboratorRepository.findById(new CollaboratorDataModel.CollaboratorCompositeId(TENANT_ID, INSTRUCTOR_ID))).thenReturn(Optional.of(collaborator));
            when(scheduleRepository.findById(new ScheduleDataModel.ScheduleCompositeId(TENANT_ID, SCHEDULE_ID))).thenReturn(Optional.of(new ScheduleDataModel()));

            // When
            CourseEventDataModel result = useCase.transform(dto);

            // Then
            verify(collaboratorRepository).findById(new CollaboratorDataModel.CollaboratorCompositeId(TENANT_ID, INSTRUCTOR_ID));
            assertThat(result.getCollaborator()).isSameAs(collaborator);
        }

        @Test
        @DisplayName("Should resolve schedule FK via repository lookup")
        void shouldResolveScheduleFK_whenTransforming() {
            // Given
            CourseEventCreateRequestDTO dto = buildDto();
            CourseEventDataModel prototypeModel = new CourseEventDataModel();
            ScheduleDataModel schedule = new ScheduleDataModel();
            when(applicationContext.getBean(CourseEventDataModel.class)).thenReturn(prototypeModel);
            when(courseRepository.findById(new CourseDataModel.CourseCompositeId(TENANT_ID, COURSE_ID))).thenReturn(Optional.of(new CourseDataModel()));
            when(collaboratorRepository.findById(new CollaboratorDataModel.CollaboratorCompositeId(TENANT_ID, INSTRUCTOR_ID))).thenReturn(Optional.of(new CollaboratorDataModel()));
            when(scheduleRepository.findById(new ScheduleDataModel.ScheduleCompositeId(TENANT_ID, SCHEDULE_ID))).thenReturn(Optional.of(schedule));

            // When
            CourseEventDataModel result = useCase.transform(dto);

            // Then
            verify(scheduleRepository).findById(new ScheduleDataModel.ScheduleCompositeId(TENANT_ID, SCHEDULE_ID));
            assertThat(result.getSchedule()).isSameAs(schedule);
        }

        @Test
        @DisplayName("Should throw when course is not found")
        void shouldThrow_whenCourseNotFound() {
            // Given
            CourseEventCreateRequestDTO dto = buildDto();
            CourseEventDataModel prototypeModel = new CourseEventDataModel();
            when(applicationContext.getBean(CourseEventDataModel.class)).thenReturn(prototypeModel);
            when(courseRepository.findById(new CourseDataModel.CourseCompositeId(TENANT_ID, COURSE_ID))).thenReturn(Optional.empty());

            // When / Then
            assertThatThrownBy(() -> useCase.transform(dto))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining(String.valueOf(COURSE_ID));
        }

        @Test
        @DisplayName("Should throw when collaborator is not found")
        void shouldThrow_whenCollaboratorNotFound() {
            // Given
            CourseEventCreateRequestDTO dto = buildDto();
            CourseEventDataModel prototypeModel = new CourseEventDataModel();
            when(applicationContext.getBean(CourseEventDataModel.class)).thenReturn(prototypeModel);
            when(courseRepository.findById(new CourseDataModel.CourseCompositeId(TENANT_ID, COURSE_ID))).thenReturn(Optional.of(new CourseDataModel()));
            when(collaboratorRepository.findById(new CollaboratorDataModel.CollaboratorCompositeId(TENANT_ID, INSTRUCTOR_ID))).thenReturn(Optional.empty());

            // When / Then
            assertThatThrownBy(() -> useCase.transform(dto))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining(String.valueOf(INSTRUCTOR_ID));
        }

        @Test
        @DisplayName("Should throw when schedule is not found")
        void shouldThrow_whenScheduleNotFound() {
            // Given
            CourseEventCreateRequestDTO dto = buildDto();
            CourseEventDataModel prototypeModel = new CourseEventDataModel();
            when(applicationContext.getBean(CourseEventDataModel.class)).thenReturn(prototypeModel);
            when(courseRepository.findById(new CourseDataModel.CourseCompositeId(TENANT_ID, COURSE_ID))).thenReturn(Optional.of(new CourseDataModel()));
            when(collaboratorRepository.findById(new CollaboratorDataModel.CollaboratorCompositeId(TENANT_ID, INSTRUCTOR_ID))).thenReturn(Optional.of(new CollaboratorDataModel()));
            when(scheduleRepository.findById(new ScheduleDataModel.ScheduleCompositeId(TENANT_ID, SCHEDULE_ID))).thenReturn(Optional.empty());

            // When / Then
            assertThatThrownBy(() -> useCase.transform(dto))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining(String.valueOf(SCHEDULE_ID));
        }
    }

    @Nested
    @DisplayName("Persistence")
    class Persistence {

        @Test
        @DisplayName("Should save transformed model and return mapped DTO")
        void shouldSaveAndReturnDto_whenCreating() {
            // Given
            CourseEventCreateRequestDTO dto = buildDto();
            CourseEventDataModel prototypeModel = new CourseEventDataModel();
            CourseEventDataModel savedModel = new CourseEventDataModel();
            savedModel.setCourseEventId(SAVED_ID);
            CourseEventCreateResponseDTO expectedDto = new CourseEventCreateResponseDTO();
            expectedDto.setCourseEventId(SAVED_ID);

            when(applicationContext.getBean(CourseEventDataModel.class)).thenReturn(prototypeModel);
            doNothing().when(modelMapper).map(dto, prototypeModel, CourseEventCreationUseCase.MAP_NAME);
            when(courseRepository.findById(new CourseDataModel.CourseCompositeId(TENANT_ID, COURSE_ID))).thenReturn(Optional.of(new CourseDataModel()));
            when(collaboratorRepository.findById(new CollaboratorDataModel.CollaboratorCompositeId(TENANT_ID, INSTRUCTOR_ID))).thenReturn(Optional.of(new CollaboratorDataModel()));
            when(scheduleRepository.findById(new ScheduleDataModel.ScheduleCompositeId(TENANT_ID, SCHEDULE_ID))).thenReturn(Optional.of(new ScheduleDataModel()));
            when(courseEventRepository.saveAndFlush(prototypeModel)).thenReturn(savedModel);
            when(modelMapper.map(savedModel, CourseEventCreateResponseDTO.class)).thenReturn(expectedDto);

            // When
            CourseEventCreateResponseDTO result = useCase.create(dto);

            // Then
            verify(courseEventRepository).saveAndFlush(prototypeModel);
            verify(modelMapper).map(savedModel, CourseEventCreateResponseDTO.class);
            assertThat(result.getCourseEventId()).isEqualTo(SAVED_ID);
        }

        @Test
        @DisplayName("Should pass transform result directly to repository save")
        void shouldPassTransformResultToSave_whenCreating() {
            // Given
            CourseEventCreateRequestDTO dto = buildDto();
            CourseEventDataModel prototypeModel = new CourseEventDataModel();
            CourseEventDataModel savedModel = new CourseEventDataModel();
            CourseEventCreateResponseDTO responseDto = new CourseEventCreateResponseDTO();

            when(applicationContext.getBean(CourseEventDataModel.class)).thenReturn(prototypeModel);
            doNothing().when(modelMapper).map(dto, prototypeModel, CourseEventCreationUseCase.MAP_NAME);
            when(courseRepository.findById(new CourseDataModel.CourseCompositeId(TENANT_ID, COURSE_ID))).thenReturn(Optional.of(new CourseDataModel()));
            when(collaboratorRepository.findById(new CollaboratorDataModel.CollaboratorCompositeId(TENANT_ID, INSTRUCTOR_ID))).thenReturn(Optional.of(new CollaboratorDataModel()));
            when(scheduleRepository.findById(new ScheduleDataModel.ScheduleCompositeId(TENANT_ID, SCHEDULE_ID))).thenReturn(Optional.of(new ScheduleDataModel()));
            when(courseEventRepository.saveAndFlush(prototypeModel)).thenReturn(savedModel);
            when(modelMapper.map(savedModel, CourseEventCreateResponseDTO.class)).thenReturn(responseDto);

            // When
            useCase.create(dto);

            // Then — verify the exact object from transform() is what gets saved
            InOrder inOrder = inOrder(applicationContext, modelMapper, courseRepository, collaboratorRepository, scheduleRepository, courseEventRepository);
            inOrder.verify(applicationContext).getBean(CourseEventDataModel.class);
            inOrder.verify(modelMapper).map(dto, prototypeModel, CourseEventCreationUseCase.MAP_NAME);
            inOrder.verify(courseRepository).findById(new CourseDataModel.CourseCompositeId(TENANT_ID, COURSE_ID));
            inOrder.verify(collaboratorRepository).findById(new CollaboratorDataModel.CollaboratorCompositeId(TENANT_ID, INSTRUCTOR_ID));
            inOrder.verify(scheduleRepository).findById(new ScheduleDataModel.ScheduleCompositeId(TENANT_ID, SCHEDULE_ID));
            inOrder.verify(courseEventRepository).saveAndFlush(prototypeModel);
            inOrder.verify(modelMapper).map(savedModel, CourseEventCreateResponseDTO.class);
        }
    }
}
