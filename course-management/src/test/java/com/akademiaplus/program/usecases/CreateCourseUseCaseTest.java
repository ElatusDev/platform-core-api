/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.program.usecases;

import com.akademiaplus.courses.program.CourseDataModel;
import com.akademiaplus.courses.program.ScheduleDataModel;
import com.akademiaplus.program.application.CourseValidator;
import com.akademiaplus.program.interfaceadapters.CourseRepository;
import com.akademiaplus.program.interfaceadapters.ScheduleRepository;
import com.akademiaplus.users.collaborator.CollaboratorDataModel;
import openapi.akademiaplus.domain.course.management.dto.CourseCreationRequestDTO;
import openapi.akademiaplus.domain.course.management.dto.CourseCreationResponseDTO;
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

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@DisplayName("CreateCourseUseCase")
@ExtendWith(MockitoExtension.class)
class CreateCourseUseCaseTest {

    @Mock private ApplicationContext applicationContext;
    @Mock private CourseRepository courseRepository;
    @Mock private ScheduleRepository scheduleRepository;
    @Mock private CourseValidator courseValidator;
    @Mock private ModelMapper modelMapper;

    private CreateCourseUseCase useCase;

    private static final String TEST_COURSE_NAME = "Mathematics 101";
    private static final String TEST_COURSE_DESCRIPTION = "Introduction to calculus";
    private static final Integer TEST_MAX_CAPACITY = 30;
    private static final Long TEST_COLLABORATOR_ID = 1L;
    private static final Long TEST_SCHEDULE_ID = 10L;
    private static final Long TEST_COURSE_ID = 100L;

    @BeforeEach
    void setUp() {
        useCase = new CreateCourseUseCase(
                applicationContext,
                courseRepository,
                scheduleRepository,
                courseValidator,
                modelMapper
        );
    }

    private CourseCreationRequestDTO buildDto() {
        CourseCreationRequestDTO dto = new CourseCreationRequestDTO();
        dto.setName(TEST_COURSE_NAME);
        dto.setDescription(TEST_COURSE_DESCRIPTION);
        dto.setMaxCapacity(TEST_MAX_CAPACITY);
        dto.setAvailableCollaboratorIds(List.of(TEST_COLLABORATOR_ID));
        dto.setTimeTableIds(List.of(TEST_SCHEDULE_ID));
        return dto;
    }

    @Nested
    @DisplayName("Transformation")
    class Transformation {

        @Test
        @DisplayName("Should retrieve prototype CourseDataModel from ApplicationContext")
        void shouldRetrievePrototypeBean_whenTransforming() {
            // Given
            CourseCreationRequestDTO dto = buildDto();
            CourseDataModel prototypeModel = new CourseDataModel();
            CollaboratorDataModel collaborator = new CollaboratorDataModel();
            when(applicationContext.getBean(CourseDataModel.class)).thenReturn(prototypeModel);
            when(courseValidator.validateCollaboratorsExist(dto.getAvailableCollaboratorIds()))
                    .thenReturn(List.of(collaborator));

            // When
            useCase.transform(dto);

            // Then
            verify(applicationContext).getBean(CourseDataModel.class);
        }

        @Test
        @DisplayName("Should delegate mapping to ModelMapper with named TypeMap")
        void shouldDelegateToModelMapper_whenTransforming() {
            // Given
            CourseCreationRequestDTO dto = buildDto();
            CourseDataModel prototypeModel = new CourseDataModel();
            CollaboratorDataModel collaborator = new CollaboratorDataModel();
            when(applicationContext.getBean(CourseDataModel.class)).thenReturn(prototypeModel);
            when(courseValidator.validateCollaboratorsExist(dto.getAvailableCollaboratorIds()))
                    .thenReturn(List.of(collaborator));

            // When
            CourseDataModel result = useCase.transform(dto);

            // Then
            verify(modelMapper).map(dto, prototypeModel, CreateCourseUseCase.MAP_NAME);
            assertThat(result).isSameAs(prototypeModel);
        }

        @Test
        @DisplayName("Should delegate collaborator validation to CourseValidator")
        void shouldDelegateCollaboratorValidation_whenTransforming() {
            // Given
            CourseCreationRequestDTO dto = buildDto();
            CourseDataModel prototypeModel = new CourseDataModel();
            CollaboratorDataModel collaborator = new CollaboratorDataModel();
            when(applicationContext.getBean(CourseDataModel.class)).thenReturn(prototypeModel);
            when(courseValidator.validateCollaboratorsExist(dto.getAvailableCollaboratorIds()))
                    .thenReturn(List.of(collaborator));

            // When
            useCase.transform(dto);

            // Then
            verify(courseValidator).validateCollaboratorsExist(dto.getAvailableCollaboratorIds());
        }

        @Test
        @DisplayName("Should set validated collaborators on the data model")
        void shouldSetCollaborators_whenTransforming() {
            // Given
            CourseCreationRequestDTO dto = buildDto();
            CourseDataModel prototypeModel = new CourseDataModel();
            CollaboratorDataModel collaborator = new CollaboratorDataModel();
            List<CollaboratorDataModel> collaborators = List.of(collaborator);
            when(applicationContext.getBean(CourseDataModel.class)).thenReturn(prototypeModel);
            when(courseValidator.validateCollaboratorsExist(dto.getAvailableCollaboratorIds()))
                    .thenReturn(collaborators);

            // When
            CourseDataModel result = useCase.transform(dto);

            // Then
            assertThat(result.getAvailableCollaborators()).isSameAs(collaborators);
        }
    }

    @Nested
    @DisplayName("Persistence")
    class Persistence {

        @Test
        @DisplayName("Should save transformed model and return mapped response DTO")
        void shouldSaveAndReturnDto_whenCreating() {
            // Given
            CourseCreationRequestDTO dto = buildDto();
            CourseDataModel prototypeModel = new CourseDataModel();
            CourseDataModel savedModel = new CourseDataModel();
            savedModel.setCourseId(TEST_COURSE_ID);
            CollaboratorDataModel collaborator = new CollaboratorDataModel();
            ScheduleDataModel schedule = new ScheduleDataModel();
            List<ScheduleDataModel> schedules = List.of(schedule);
            CourseCreationResponseDTO expectedDto = new CourseCreationResponseDTO();
            expectedDto.setCourseId(TEST_COURSE_ID);

            when(applicationContext.getBean(CourseDataModel.class)).thenReturn(prototypeModel);
            when(courseValidator.validateCollaboratorsExist(dto.getAvailableCollaboratorIds()))
                    .thenReturn(List.of(collaborator));
            doNothing().when(modelMapper).map(dto, prototypeModel, CreateCourseUseCase.MAP_NAME);
            when(courseRepository.save(prototypeModel)).thenReturn(savedModel);
            when(courseValidator.validateSchedulesAvailable(dto.getTimeTableIds()))
                    .thenReturn(schedules);
            when(scheduleRepository.saveAll(schedules)).thenReturn(schedules);
            when(modelMapper.map(savedModel, CourseCreationResponseDTO.class)).thenReturn(expectedDto);

            // When
            CourseCreationResponseDTO result = useCase.create(dto);

            // Then
            verify(courseRepository).save(prototypeModel);
            verify(modelMapper).map(savedModel, CourseCreationResponseDTO.class);
            assertThat(result.getCourseId()).isEqualTo(TEST_COURSE_ID);
        }

        @Test
        @DisplayName("Should save schedules after saving the course")
        void shouldSaveSchedules_whenCreating() {
            // Given
            CourseCreationRequestDTO dto = buildDto();
            CourseDataModel prototypeModel = new CourseDataModel();
            CourseDataModel savedModel = new CourseDataModel();
            CollaboratorDataModel collaborator = new CollaboratorDataModel();
            ScheduleDataModel schedule = new ScheduleDataModel();
            List<ScheduleDataModel> schedules = List.of(schedule);
            CourseCreationResponseDTO responseDto = new CourseCreationResponseDTO();

            when(applicationContext.getBean(CourseDataModel.class)).thenReturn(prototypeModel);
            when(courseValidator.validateCollaboratorsExist(dto.getAvailableCollaboratorIds()))
                    .thenReturn(List.of(collaborator));
            doNothing().when(modelMapper).map(dto, prototypeModel, CreateCourseUseCase.MAP_NAME);
            when(courseRepository.save(prototypeModel)).thenReturn(savedModel);
            when(courseValidator.validateSchedulesAvailable(dto.getTimeTableIds()))
                    .thenReturn(schedules);
            when(scheduleRepository.saveAll(schedules)).thenReturn(schedules);
            when(modelMapper.map(savedModel, CourseCreationResponseDTO.class)).thenReturn(responseDto);

            // When
            useCase.create(dto);

            // Then
            verify(scheduleRepository).saveAll(schedules);
        }

        @Test
        @DisplayName("Should execute operations in correct order: transform, save course, save schedules, map response")
        void shouldExecuteInOrder_whenCreating() {
            // Given
            CourseCreationRequestDTO dto = buildDto();
            CourseDataModel prototypeModel = new CourseDataModel();
            CourseDataModel savedModel = new CourseDataModel();
            CollaboratorDataModel collaborator = new CollaboratorDataModel();
            ScheduleDataModel schedule = new ScheduleDataModel();
            List<ScheduleDataModel> schedules = List.of(schedule);
            CourseCreationResponseDTO responseDto = new CourseCreationResponseDTO();

            when(applicationContext.getBean(CourseDataModel.class)).thenReturn(prototypeModel);
            when(courseValidator.validateCollaboratorsExist(dto.getAvailableCollaboratorIds()))
                    .thenReturn(List.of(collaborator));
            doNothing().when(modelMapper).map(dto, prototypeModel, CreateCourseUseCase.MAP_NAME);
            when(courseRepository.save(prototypeModel)).thenReturn(savedModel);
            when(courseValidator.validateSchedulesAvailable(dto.getTimeTableIds()))
                    .thenReturn(schedules);
            when(scheduleRepository.saveAll(schedules)).thenReturn(schedules);
            when(modelMapper.map(savedModel, CourseCreationResponseDTO.class)).thenReturn(responseDto);

            // When
            useCase.create(dto);

            // Then — verify the exact object flow
            InOrder inOrder = inOrder(applicationContext, modelMapper, courseRepository, scheduleRepository, courseValidator);
            inOrder.verify(courseValidator).validateCollaboratorsExist(dto.getAvailableCollaboratorIds());
            inOrder.verify(applicationContext).getBean(CourseDataModel.class);
            inOrder.verify(modelMapper).map(dto, prototypeModel, CreateCourseUseCase.MAP_NAME);
            inOrder.verify(courseRepository).save(prototypeModel);
            inOrder.verify(courseValidator).validateSchedulesAvailable(dto.getTimeTableIds());
            inOrder.verify(scheduleRepository).saveAll(schedules);
            inOrder.verify(modelMapper).map(savedModel, CourseCreationResponseDTO.class);
        }
    }
}
