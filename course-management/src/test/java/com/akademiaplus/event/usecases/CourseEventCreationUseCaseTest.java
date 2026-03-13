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
            CourseEventDataModel result = useCase.transform(dto);

            // Then
            assertThat(result).isSameAs(prototypeModel);
            InOrder inOrder = inOrder(applicationContext, modelMapper, courseRepository, collaboratorRepository, scheduleRepository, tenantContextHolder);
            inOrder.verify(applicationContext, times(1)).getBean(CourseEventDataModel.class);
            inOrder.verify(modelMapper, times(1)).map(dto, prototypeModel, CourseEventCreationUseCase.MAP_NAME);
            inOrder.verify(tenantContextHolder, times(1)).getTenantId();
            inOrder.verify(courseRepository, times(1)).findById(new CourseDataModel.CourseCompositeId(TENANT_ID, COURSE_ID));
            inOrder.verify(collaboratorRepository, times(1)).findById(new CollaboratorDataModel.CollaboratorCompositeId(TENANT_ID, INSTRUCTOR_ID));
            inOrder.verify(scheduleRepository, times(1)).findById(new ScheduleDataModel.ScheduleCompositeId(TENANT_ID, SCHEDULE_ID));
            inOrder.verifyNoMoreInteractions();
            verifyNoInteractions(courseEventRepository);
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
            verify(modelMapper, times(1)).map(dto, prototypeModel, CourseEventCreationUseCase.MAP_NAME);
            assertThat(result).isSameAs(prototypeModel);
            verify(tenantContextHolder, times(1)).getTenantId();
            verifyNoMoreInteractions(applicationContext, courseEventRepository, courseRepository,
                    scheduleRepository, collaboratorRepository, tenantContextHolder, modelMapper);
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
            assertThat(result.getCourse()).isSameAs(course);
            InOrder inOrder = inOrder(applicationContext, modelMapper, tenantContextHolder, courseRepository, collaboratorRepository, scheduleRepository);
            inOrder.verify(applicationContext, times(1)).getBean(CourseEventDataModel.class);
            inOrder.verify(modelMapper, times(1)).map(dto, prototypeModel, CourseEventCreationUseCase.MAP_NAME);
            inOrder.verify(tenantContextHolder, times(1)).getTenantId();
            inOrder.verify(courseRepository, times(1)).findById(new CourseDataModel.CourseCompositeId(TENANT_ID, COURSE_ID));
            inOrder.verify(collaboratorRepository, times(1)).findById(new CollaboratorDataModel.CollaboratorCompositeId(TENANT_ID, INSTRUCTOR_ID));
            inOrder.verify(scheduleRepository, times(1)).findById(new ScheduleDataModel.ScheduleCompositeId(TENANT_ID, SCHEDULE_ID));
            inOrder.verifyNoMoreInteractions();
            verifyNoInteractions(courseEventRepository);
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
            assertThat(result.getCollaborator()).isSameAs(collaborator);
            InOrder inOrder = inOrder(applicationContext, modelMapper, tenantContextHolder, courseRepository, collaboratorRepository, scheduleRepository);
            inOrder.verify(applicationContext, times(1)).getBean(CourseEventDataModel.class);
            inOrder.verify(modelMapper, times(1)).map(dto, prototypeModel, CourseEventCreationUseCase.MAP_NAME);
            inOrder.verify(tenantContextHolder, times(1)).getTenantId();
            inOrder.verify(courseRepository, times(1)).findById(new CourseDataModel.CourseCompositeId(TENANT_ID, COURSE_ID));
            inOrder.verify(collaboratorRepository, times(1)).findById(new CollaboratorDataModel.CollaboratorCompositeId(TENANT_ID, INSTRUCTOR_ID));
            inOrder.verify(scheduleRepository, times(1)).findById(new ScheduleDataModel.ScheduleCompositeId(TENANT_ID, SCHEDULE_ID));
            inOrder.verifyNoMoreInteractions();
            verifyNoInteractions(courseEventRepository);
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
            assertThat(result.getSchedule()).isSameAs(schedule);
            InOrder inOrder = inOrder(applicationContext, modelMapper, tenantContextHolder, courseRepository, collaboratorRepository, scheduleRepository);
            inOrder.verify(applicationContext, times(1)).getBean(CourseEventDataModel.class);
            inOrder.verify(modelMapper, times(1)).map(dto, prototypeModel, CourseEventCreationUseCase.MAP_NAME);
            inOrder.verify(tenantContextHolder, times(1)).getTenantId();
            inOrder.verify(courseRepository, times(1)).findById(new CourseDataModel.CourseCompositeId(TENANT_ID, COURSE_ID));
            inOrder.verify(collaboratorRepository, times(1)).findById(new CollaboratorDataModel.CollaboratorCompositeId(TENANT_ID, INSTRUCTOR_ID));
            inOrder.verify(scheduleRepository, times(1)).findById(new ScheduleDataModel.ScheduleCompositeId(TENANT_ID, SCHEDULE_ID));
            inOrder.verifyNoMoreInteractions();
            verifyNoInteractions(courseEventRepository);
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when course is not found")
        void shouldThrowIllegalArgumentException_whenCourseNotFound() {
            // Given
            CourseEventCreateRequestDTO dto = buildDto();
            CourseEventDataModel prototypeModel = new CourseEventDataModel();
            when(applicationContext.getBean(CourseEventDataModel.class)).thenReturn(prototypeModel);
            when(courseRepository.findById(new CourseDataModel.CourseCompositeId(TENANT_ID, COURSE_ID))).thenReturn(Optional.empty());

            // When / Then
            assertThatThrownBy(() -> useCase.transform(dto))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining(CourseEventCreationUseCase.ERROR_COURSE_NOT_FOUND)
                    .hasMessageContaining(String.valueOf(COURSE_ID));

            verify(applicationContext, times(1)).getBean(CourseEventDataModel.class);
            verify(modelMapper, times(1)).map(dto, prototypeModel, CourseEventCreationUseCase.MAP_NAME);
            verify(tenantContextHolder, times(1)).getTenantId();
            verify(courseRepository, times(1)).findById(new CourseDataModel.CourseCompositeId(TENANT_ID, COURSE_ID));
            verifyNoInteractions(courseEventRepository, collaboratorRepository, scheduleRepository);
            verifyNoMoreInteractions(applicationContext, courseRepository, tenantContextHolder, modelMapper);
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when collaborator is not found")
        void shouldThrowIllegalArgumentException_whenCollaboratorNotFound() {
            // Given
            CourseEventCreateRequestDTO dto = buildDto();
            CourseEventDataModel prototypeModel = new CourseEventDataModel();
            when(applicationContext.getBean(CourseEventDataModel.class)).thenReturn(prototypeModel);
            when(courseRepository.findById(new CourseDataModel.CourseCompositeId(TENANT_ID, COURSE_ID))).thenReturn(Optional.of(new CourseDataModel()));
            when(collaboratorRepository.findById(new CollaboratorDataModel.CollaboratorCompositeId(TENANT_ID, INSTRUCTOR_ID))).thenReturn(Optional.empty());

            // When / Then
            assertThatThrownBy(() -> useCase.transform(dto))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining(CourseEventCreationUseCase.ERROR_COLLABORATOR_NOT_FOUND)
                    .hasMessageContaining(String.valueOf(INSTRUCTOR_ID));

            verify(applicationContext, times(1)).getBean(CourseEventDataModel.class);
            verify(modelMapper, times(1)).map(dto, prototypeModel, CourseEventCreationUseCase.MAP_NAME);
            verify(tenantContextHolder, times(1)).getTenantId();
            verify(courseRepository, times(1)).findById(new CourseDataModel.CourseCompositeId(TENANT_ID, COURSE_ID));
            verify(collaboratorRepository, times(1)).findById(new CollaboratorDataModel.CollaboratorCompositeId(TENANT_ID, INSTRUCTOR_ID));
            verifyNoInteractions(courseEventRepository, scheduleRepository);
            verifyNoMoreInteractions(applicationContext, courseRepository, collaboratorRepository,
                    tenantContextHolder, modelMapper);
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when schedule is not found")
        void shouldThrowIllegalArgumentException_whenScheduleNotFound() {
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
                    .hasMessageContaining(CourseEventCreationUseCase.ERROR_SCHEDULE_NOT_FOUND)
                    .hasMessageContaining(String.valueOf(SCHEDULE_ID));

            verify(applicationContext, times(1)).getBean(CourseEventDataModel.class);
            verify(modelMapper, times(1)).map(dto, prototypeModel, CourseEventCreationUseCase.MAP_NAME);
            verify(tenantContextHolder, times(1)).getTenantId();
            verify(courseRepository, times(1)).findById(new CourseDataModel.CourseCompositeId(TENANT_ID, COURSE_ID));
            verify(collaboratorRepository, times(1)).findById(new CollaboratorDataModel.CollaboratorCompositeId(TENANT_ID, INSTRUCTOR_ID));
            verify(scheduleRepository, times(1)).findById(new ScheduleDataModel.ScheduleCompositeId(TENANT_ID, SCHEDULE_ID));
            verifyNoInteractions(courseEventRepository);
            verifyNoMoreInteractions(applicationContext, courseRepository, collaboratorRepository,
                    scheduleRepository, tenantContextHolder, modelMapper);
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when tenant context is missing")
        void shouldThrowIllegalArgumentException_whenTenantContextMissing() {
            // Given
            CourseEventCreateRequestDTO dto = buildDto();
            CourseEventDataModel prototypeModel = new CourseEventDataModel();
            when(applicationContext.getBean(CourseEventDataModel.class)).thenReturn(prototypeModel);
            when(tenantContextHolder.getTenantId()).thenReturn(Optional.empty());

            // When / Then
            assertThatThrownBy(() -> useCase.transform(dto))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage(CourseEventCreationUseCase.ERROR_TENANT_CONTEXT_REQUIRED);

            verify(applicationContext, times(1)).getBean(CourseEventDataModel.class);
            verify(modelMapper, times(1)).map(dto, prototypeModel, CourseEventCreationUseCase.MAP_NAME);
            verify(tenantContextHolder, times(1)).getTenantId();
            verifyNoInteractions(courseEventRepository, courseRepository, collaboratorRepository, scheduleRepository);
            verifyNoMoreInteractions(applicationContext, tenantContextHolder, modelMapper);
        }
    }

    @Nested
    @DisplayName("Collaborator Exception Propagation")
    class CollaboratorExceptionPropagation {

        @Test
        @DisplayName("Should propagate exception when courseEventRepository.saveAndFlush throws")
        void shouldPropagateException_whenSaveAndFlushThrows() {
            // Given
            CourseEventCreateRequestDTO dto = buildDto();
            CourseEventDataModel prototypeModel = new CourseEventDataModel();

            when(applicationContext.getBean(CourseEventDataModel.class)).thenReturn(prototypeModel);
            doNothing().when(modelMapper).map(dto, prototypeModel, CourseEventCreationUseCase.MAP_NAME);
            when(courseRepository.findById(new CourseDataModel.CourseCompositeId(TENANT_ID, COURSE_ID))).thenReturn(Optional.of(new CourseDataModel()));
            when(collaboratorRepository.findById(new CollaboratorDataModel.CollaboratorCompositeId(TENANT_ID, INSTRUCTOR_ID))).thenReturn(Optional.of(new CollaboratorDataModel()));
            when(scheduleRepository.findById(new ScheduleDataModel.ScheduleCompositeId(TENANT_ID, SCHEDULE_ID))).thenReturn(Optional.of(new ScheduleDataModel()));
            when(courseEventRepository.saveAndFlush(prototypeModel))
                    .thenThrow(new RuntimeException("DB connection lost"));

            // When / Then
            assertThatThrownBy(() -> useCase.create(dto))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("DB connection lost");

            verify(courseEventRepository, times(1)).saveAndFlush(prototypeModel);
            verifyNoMoreInteractions(courseEventRepository);
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
            assertThat(result.getCourseEventId()).isEqualTo(SAVED_ID);
            verify(courseEventRepository, times(1)).saveAndFlush(prototypeModel);
            verify(modelMapper, times(1)).map(savedModel, CourseEventCreateResponseDTO.class);
            verify(tenantContextHolder, times(1)).getTenantId();
            verifyNoMoreInteractions(applicationContext, courseEventRepository, courseRepository,
                    scheduleRepository, collaboratorRepository, tenantContextHolder, modelMapper);
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
            CourseEventCreateResponseDTO result = useCase.create(dto);

            // Then — verify the exact object from transform() is what gets saved
            assertThat(result).isSameAs(responseDto);
            InOrder inOrder = inOrder(applicationContext, modelMapper, tenantContextHolder, courseRepository, collaboratorRepository, scheduleRepository, courseEventRepository);
            inOrder.verify(applicationContext, times(1)).getBean(CourseEventDataModel.class);
            inOrder.verify(modelMapper, times(1)).map(dto, prototypeModel, CourseEventCreationUseCase.MAP_NAME);
            inOrder.verify(tenantContextHolder, times(1)).getTenantId();
            inOrder.verify(courseRepository, times(1)).findById(new CourseDataModel.CourseCompositeId(TENANT_ID, COURSE_ID));
            inOrder.verify(collaboratorRepository, times(1)).findById(new CollaboratorDataModel.CollaboratorCompositeId(TENANT_ID, INSTRUCTOR_ID));
            inOrder.verify(scheduleRepository, times(1)).findById(new ScheduleDataModel.ScheduleCompositeId(TENANT_ID, SCHEDULE_ID));
            inOrder.verify(courseEventRepository, times(1)).saveAndFlush(prototypeModel);
            inOrder.verify(modelMapper, times(1)).map(savedModel, CourseEventCreateResponseDTO.class);
            inOrder.verifyNoMoreInteractions();
        }
    }
}
