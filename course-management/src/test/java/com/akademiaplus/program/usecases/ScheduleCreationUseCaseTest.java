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
import com.akademiaplus.infra.persistence.config.TenantContextHolder;
import com.akademiaplus.program.interfaceadapters.CourseRepository;
import com.akademiaplus.program.interfaceadapters.ScheduleRepository;
import openapi.akademiaplus.domain.course.management.dto.ScheduleCreationRequestDTO;
import openapi.akademiaplus.domain.course.management.dto.ScheduleCreationResponseDTO;
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

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@DisplayName("ScheduleCreationUseCase")
@ExtendWith(MockitoExtension.class)
class ScheduleCreationUseCaseTest {

    @Mock private ApplicationContext applicationContext;
    @Mock private ScheduleRepository scheduleRepository;
    @Mock private CourseRepository courseRepository;
    @Mock private TenantContextHolder tenantContextHolder;
    @Mock private ModelMapper modelMapper;

    private ScheduleCreationUseCase useCase;

    private static final Long TENANT_ID = 1L;
    private static final String SCHEDULE_DAY = "Monday";
    private static final String START_TIME = "09:00";
    private static final String END_TIME = "10:30";
    private static final Long COURSE_ID = 5L;
    private static final Long SAVED_ID = 1L;

    @BeforeEach
    void setUp() {
        useCase = new ScheduleCreationUseCase(applicationContext, scheduleRepository, courseRepository, tenantContextHolder, modelMapper);
        lenient().when(tenantContextHolder.getTenantId()).thenReturn(Optional.of(TENANT_ID));
    }

    private ScheduleCreationRequestDTO buildDto() {
        ScheduleCreationRequestDTO dto = new ScheduleCreationRequestDTO();
        dto.setScheduleDay(SCHEDULE_DAY);
        dto.setStartTime(START_TIME);
        dto.setEndTime(END_TIME);
        dto.setCourseId(COURSE_ID);
        return dto;
    }

    @Nested
    @DisplayName("Transformation")
    class Transformation {

        @Test
        @DisplayName("Should retrieve prototype ScheduleDataModel from ApplicationContext")
        void shouldRetrievePrototypeBean_whenTransforming() {
            // Given
            ScheduleCreationRequestDTO dto = buildDto();
            ScheduleDataModel prototypeModel = new ScheduleDataModel();
            CourseDataModel course = new CourseDataModel();
            when(applicationContext.getBean(ScheduleDataModel.class)).thenReturn(prototypeModel);
            when(courseRepository.findById(new CourseDataModel.CourseCompositeId(TENANT_ID, COURSE_ID))).thenReturn(Optional.of(course));

            // When
            ScheduleDataModel result = useCase.transform(dto);

            // Then
            assertThat(result).isSameAs(prototypeModel);
            InOrder inOrder = inOrder(applicationContext, modelMapper, tenantContextHolder, courseRepository);
            inOrder.verify(applicationContext, times(1)).getBean(ScheduleDataModel.class);
            inOrder.verify(modelMapper, times(1)).map(dto, prototypeModel, ScheduleCreationUseCase.MAP_NAME);
            inOrder.verify(tenantContextHolder, times(1)).getTenantId();
            inOrder.verify(courseRepository, times(1)).findById(new CourseDataModel.CourseCompositeId(TENANT_ID, COURSE_ID));
            inOrder.verifyNoMoreInteractions();
            verifyNoInteractions(scheduleRepository);
        }

        @Test
        @DisplayName("Should delegate mapping to ModelMapper with named TypeMap")
        void shouldDelegateToModelMapper_whenTransforming() {
            // Given
            ScheduleCreationRequestDTO dto = buildDto();
            ScheduleDataModel prototypeModel = new ScheduleDataModel();
            CourseDataModel course = new CourseDataModel();
            when(applicationContext.getBean(ScheduleDataModel.class)).thenReturn(prototypeModel);
            when(courseRepository.findById(new CourseDataModel.CourseCompositeId(TENANT_ID, COURSE_ID))).thenReturn(Optional.of(course));

            // When
            ScheduleDataModel result = useCase.transform(dto);

            // Then
            verify(modelMapper, times(1)).map(dto, prototypeModel, ScheduleCreationUseCase.MAP_NAME);
            assertThat(result).isSameAs(prototypeModel);
            verify(tenantContextHolder, times(1)).getTenantId();
            verifyNoInteractions(scheduleRepository);
            verifyNoMoreInteractions(applicationContext, courseRepository, tenantContextHolder, modelMapper);
        }

        @Test
        @DisplayName("Should resolve course FK via repository lookup")
        void shouldResolveCourseFK_whenTransforming() {
            // Given
            ScheduleCreationRequestDTO dto = buildDto();
            ScheduleDataModel prototypeModel = new ScheduleDataModel();
            CourseDataModel course = new CourseDataModel();
            when(applicationContext.getBean(ScheduleDataModel.class)).thenReturn(prototypeModel);
            when(courseRepository.findById(new CourseDataModel.CourseCompositeId(TENANT_ID, COURSE_ID))).thenReturn(Optional.of(course));

            // When
            ScheduleDataModel result = useCase.transform(dto);

            // Then
            assertThat(result.getCourse()).isSameAs(course);
            InOrder inOrder = inOrder(applicationContext, modelMapper, tenantContextHolder, courseRepository);
            inOrder.verify(applicationContext, times(1)).getBean(ScheduleDataModel.class);
            inOrder.verify(modelMapper, times(1)).map(dto, prototypeModel, ScheduleCreationUseCase.MAP_NAME);
            inOrder.verify(tenantContextHolder, times(1)).getTenantId();
            inOrder.verify(courseRepository, times(1)).findById(new CourseDataModel.CourseCompositeId(TENANT_ID, COURSE_ID));
            inOrder.verifyNoMoreInteractions();
            verifyNoInteractions(scheduleRepository);
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when course is not found")
        void shouldThrowIllegalArgumentException_whenCourseNotFound() {
            // Given
            ScheduleCreationRequestDTO dto = buildDto();
            ScheduleDataModel prototypeModel = new ScheduleDataModel();
            when(applicationContext.getBean(ScheduleDataModel.class)).thenReturn(prototypeModel);
            when(courseRepository.findById(new CourseDataModel.CourseCompositeId(TENANT_ID, COURSE_ID))).thenReturn(Optional.empty());

            // When / Then
            assertThatThrownBy(() -> useCase.transform(dto))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining(ScheduleCreationUseCase.ERROR_COURSE_NOT_FOUND)
                    .hasMessageContaining(String.valueOf(COURSE_ID));

            verify(applicationContext, times(1)).getBean(ScheduleDataModel.class);
            verify(modelMapper, times(1)).map(dto, prototypeModel, ScheduleCreationUseCase.MAP_NAME);
            verify(tenantContextHolder, times(1)).getTenantId();
            verify(courseRepository, times(1)).findById(new CourseDataModel.CourseCompositeId(TENANT_ID, COURSE_ID));
            verifyNoInteractions(scheduleRepository);
            verifyNoMoreInteractions(applicationContext, courseRepository, tenantContextHolder, modelMapper);
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when tenant context is missing")
        void shouldThrowIllegalArgumentException_whenTenantContextMissing() {
            // Given
            ScheduleCreationRequestDTO dto = buildDto();
            ScheduleDataModel prototypeModel = new ScheduleDataModel();
            when(applicationContext.getBean(ScheduleDataModel.class)).thenReturn(prototypeModel);
            when(tenantContextHolder.getTenantId()).thenReturn(Optional.empty());

            // When / Then
            assertThatThrownBy(() -> useCase.transform(dto))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage(ScheduleCreationUseCase.ERROR_TENANT_CONTEXT_REQUIRED);

            verify(applicationContext, times(1)).getBean(ScheduleDataModel.class);
            verify(modelMapper, times(1)).map(dto, prototypeModel, ScheduleCreationUseCase.MAP_NAME);
            verify(tenantContextHolder, times(1)).getTenantId();
            verifyNoInteractions(courseRepository, scheduleRepository);
            verifyNoMoreInteractions(applicationContext, tenantContextHolder, modelMapper);
        }
    }

    @Nested
    @DisplayName("Collaborator Exception Propagation")
    class CollaboratorExceptionPropagation {

        @Test
        @DisplayName("Should propagate exception when scheduleRepository.saveAndFlush throws")
        void shouldPropagateException_whenSaveAndFlushThrows() {
            // Given
            ScheduleCreationRequestDTO dto = buildDto();
            ScheduleDataModel prototypeModel = new ScheduleDataModel();
            CourseDataModel course = new CourseDataModel();

            when(applicationContext.getBean(ScheduleDataModel.class)).thenReturn(prototypeModel);
            doNothing().when(modelMapper).map(dto, prototypeModel, ScheduleCreationUseCase.MAP_NAME);
            when(courseRepository.findById(new CourseDataModel.CourseCompositeId(TENANT_ID, COURSE_ID))).thenReturn(Optional.of(course));
            when(scheduleRepository.saveAndFlush(prototypeModel))
                    .thenThrow(new RuntimeException("DB connection lost"));

            // When / Then
            assertThatThrownBy(() -> useCase.create(dto))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("DB connection lost");

            verify(scheduleRepository, times(1)).saveAndFlush(prototypeModel);
            verifyNoMoreInteractions(scheduleRepository);
        }
    }

    @Nested
    @DisplayName("Persistence")
    class Persistence {

        @Test
        @DisplayName("Should save transformed model and return mapped DTO")
        void shouldSaveAndReturnDto_whenCreating() {
            // Given
            ScheduleCreationRequestDTO dto = buildDto();
            ScheduleDataModel prototypeModel = new ScheduleDataModel();
            ScheduleDataModel savedModel = new ScheduleDataModel();
            savedModel.setScheduleId(SAVED_ID);
            CourseDataModel course = new CourseDataModel();
            ScheduleCreationResponseDTO expectedDto = new ScheduleCreationResponseDTO();
            expectedDto.setScheduleId(SAVED_ID);

            when(applicationContext.getBean(ScheduleDataModel.class)).thenReturn(prototypeModel);
            doNothing().when(modelMapper).map(dto, prototypeModel, ScheduleCreationUseCase.MAP_NAME);
            when(courseRepository.findById(new CourseDataModel.CourseCompositeId(TENANT_ID, COURSE_ID))).thenReturn(Optional.of(course));
            when(scheduleRepository.saveAndFlush(prototypeModel)).thenReturn(savedModel);
            when(modelMapper.map(savedModel, ScheduleCreationResponseDTO.class)).thenReturn(expectedDto);

            // When
            ScheduleCreationResponseDTO result = useCase.create(dto);

            // Then
            assertThat(result.getScheduleId()).isEqualTo(SAVED_ID);
            verify(scheduleRepository, times(1)).saveAndFlush(prototypeModel);
            verify(modelMapper, times(1)).map(savedModel, ScheduleCreationResponseDTO.class);
            verify(tenantContextHolder, times(1)).getTenantId();
            verifyNoMoreInteractions(applicationContext, scheduleRepository, courseRepository,
                    tenantContextHolder, modelMapper);
        }

        @Test
        @DisplayName("Should pass transform result directly to repository save")
        void shouldPassTransformResultToSave_whenCreating() {
            // Given
            ScheduleCreationRequestDTO dto = buildDto();
            ScheduleDataModel prototypeModel = new ScheduleDataModel();
            ScheduleDataModel savedModel = new ScheduleDataModel();
            CourseDataModel course = new CourseDataModel();
            ScheduleCreationResponseDTO responseDto = new ScheduleCreationResponseDTO();

            when(applicationContext.getBean(ScheduleDataModel.class)).thenReturn(prototypeModel);
            doNothing().when(modelMapper).map(dto, prototypeModel, ScheduleCreationUseCase.MAP_NAME);
            when(courseRepository.findById(new CourseDataModel.CourseCompositeId(TENANT_ID, COURSE_ID))).thenReturn(Optional.of(course));
            when(scheduleRepository.saveAndFlush(prototypeModel)).thenReturn(savedModel);
            when(modelMapper.map(savedModel, ScheduleCreationResponseDTO.class)).thenReturn(responseDto);

            // When
            ScheduleCreationResponseDTO result = useCase.create(dto);

            // Then — verify the exact object from transform() is what gets saved
            assertThat(result).isSameAs(responseDto);
            InOrder inOrder = inOrder(applicationContext, modelMapper, tenantContextHolder, courseRepository, scheduleRepository);
            inOrder.verify(applicationContext, times(1)).getBean(ScheduleDataModel.class);
            inOrder.verify(modelMapper, times(1)).map(dto, prototypeModel, ScheduleCreationUseCase.MAP_NAME);
            inOrder.verify(tenantContextHolder, times(1)).getTenantId();
            inOrder.verify(courseRepository, times(1)).findById(new CourseDataModel.CourseCompositeId(TENANT_ID, COURSE_ID));
            inOrder.verify(scheduleRepository, times(1)).saveAndFlush(prototypeModel);
            inOrder.verify(modelMapper, times(1)).map(savedModel, ScheduleCreationResponseDTO.class);
            inOrder.verifyNoMoreInteractions();
        }
    }
}
