package com.akademiaplus.program.usecases;

import com.akademiaplus.courses.program.CourseDataModel;
import com.akademiaplus.courses.program.ScheduleDataModel;
import com.akademiaplus.users.collaborator.CollaboratorDataModel;
import com.akademiaplus.program.application.CourseValidator;
import com.akademiaplus.program.interfaceadapters.CourseRepository;

import com.akademiaplus.program.interfaceadapters.SchedulePersistenceOutputPort;
import openapi.akademiaplus.domain.course_management.dto.CourseCreationRequestDTO;
import openapi.akademiaplus.domain.course_management.dto.CourseCreationResponseDTO;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CreateCourseUseCase {

    private final CourseRepository courseRepository;
    private final SchedulePersistenceOutputPort schedulePersistenceOutputPort;
    private final CourseValidator courseValidator;
    private final ModelMapper modelMapper;

    public CreateCourseUseCase(CourseRepository courseRepository,
                               SchedulePersistenceOutputPort schedulePersistenceOutputPort,
                               CourseValidator courseValidator,
                               ModelMapper modelMapper) {
        this.courseRepository = courseRepository;
        this.schedulePersistenceOutputPort = schedulePersistenceOutputPort; // Inject the port
        this.courseValidator = courseValidator;
        this.modelMapper = modelMapper;
    }

    @Transactional
    public CourseCreationResponseDTO create(CourseCreationRequestDTO dto) {
        List<CollaboratorDataModel> existingCollaborators = courseValidator.validateCollaboratorsExist(dto.getAvailableCollaboratorIds());
        List<ScheduleDataModel> existingSchedules = courseValidator.validateSchedulesAvailable(dto.getTimeTableIds());

        CourseDataModel courseDataModel = modelMapper.map(dto, CourseDataModel.class);
        courseDataModel.setAvailableCollaborators(existingCollaborators);

        CourseDataModel savedCourse = courseRepository.save(courseDataModel);
        schedulePersistenceOutputPort.update(existingSchedules);
        return modelMapper.map(savedCourse, CourseCreationResponseDTO.class);
    }
}