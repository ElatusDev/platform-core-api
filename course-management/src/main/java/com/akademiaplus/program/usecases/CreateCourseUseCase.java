package com.akademiaplus.program.usecases;

import com.akademiaplus.courses.program.CourseDataModel;
import com.akademiaplus.courses.program.ScheduleDataModel;
import com.akademiaplus.program.interfaceadapters.ScheduleRepository;
import com.akademiaplus.users.collaborator.CollaboratorDataModel;
import com.akademiaplus.program.application.CourseValidator;
import com.akademiaplus.program.interfaceadapters.CourseRepository;

import openapi.akademiaplus.domain.course.management.dto.CourseCreationRequestDTO;
import openapi.akademiaplus.domain.course.management.dto.CourseCreationResponseDTO;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CreateCourseUseCase {

    private final CourseRepository courseRepository;
    private final ScheduleRepository scheduleRepository;
    private final CourseValidator courseValidator;
    private final ModelMapper modelMapper;

    public CreateCourseUseCase(CourseRepository courseRepository,
                               ScheduleRepository scheduleRepository,
                               CourseValidator courseValidator,
                               ModelMapper modelMapper) {
        this.courseRepository = courseRepository;
        this.courseValidator = courseValidator;
        this.modelMapper = modelMapper;
        this.scheduleRepository = scheduleRepository;
    }

    @Transactional
    public CourseCreationResponseDTO create(CourseCreationRequestDTO dto) {
        List<CollaboratorDataModel> existingCollaborators = courseValidator.validateCollaboratorsExist(dto.getAvailableCollaboratorIds());
        List<ScheduleDataModel> existingSchedules = courseValidator.validateSchedulesAvailable(dto.getTimeTableIds());

        CourseDataModel courseDataModel = modelMapper.map(dto, CourseDataModel.class);
        courseDataModel.setAvailableCollaborators(existingCollaborators);

        CourseDataModel savedCourse = courseRepository.save(courseDataModel);
        scheduleRepository.saveAll(existingSchedules);
        return modelMapper.map(savedCourse, CourseCreationResponseDTO.class);
    }
}