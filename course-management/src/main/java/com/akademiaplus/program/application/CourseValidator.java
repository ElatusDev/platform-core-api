package com.akademiaplus.program.application;

import com.akademiaplus.collaborator.interfaceadapters.CollaboratorRepository;
import com.akademiaplus.courses.program.ScheduleDataModel;
import com.akademiaplus.exception.CollaboratorNotFoundException;
import com.akademiaplus.exception.ScheduleNotFoundException;
import com.akademiaplus.exception.ScheduleNotAvailableException;
import com.akademiaplus.users.collaborator.CollaboratorDataModel;
import com.akademiaplus.program.interfaceadapters.SchedulePersistenceOutputPort;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class CourseValidator {

    private final CollaboratorRepository collaboratorRepository;
    private final SchedulePersistenceOutputPort schedulePersistenceOutputPort;

    public CourseValidator(CollaboratorRepository collaboratorRepository,
                           SchedulePersistenceOutputPort schedulePersistenceOutputPort) {
        this.collaboratorRepository = collaboratorRepository;
        this.schedulePersistenceOutputPort = schedulePersistenceOutputPort;
    }

    public List<CollaboratorDataModel> validateCollaboratorsExist(List<Integer> collaboratorIds) {
        List<CollaboratorDataModel> foundCollaborator = collaboratorRepository.findAllById(collaboratorIds);

        if (foundCollaborator.size() != collaboratorIds.size()) {
            Set<Integer> foundIds = foundCollaborator.stream().map(CollaboratorDataModel::getCollaboratorId)
                                                              .collect(Collectors.toSet());
            String missingIds = collaboratorIds.stream()
                    .filter(id -> !foundIds.contains(id))
                    .map(String::valueOf)
                    .collect(Collectors.joining(", "));
            throw new CollaboratorNotFoundException(missingIds);
        } else {
            return foundCollaborator;
        }
    }

    /**
     * Validates the existence and availability of schedules by their IDs.
     * Throws ScheduleNotFoundException if any ID is not found.
     * Throws ScheduleNotAvailableException if any schedule is already assigned to a course.
     *
     * @param scheduleIds List of schedule IDs to validate.
     * @return List of found Schedule data models that are available.
     */
    public List<ScheduleDataModel> validateSchedulesAvailable(List<Integer> scheduleIds) {
        if (scheduleIds == null || scheduleIds.isEmpty()) {
            return List.of();
        }

        List<ScheduleDataModel> foundSchedules = schedulePersistenceOutputPort.findAllById(scheduleIds);
        if (foundSchedules.size() != scheduleIds.size()) {
            Set<Integer> foundIds = foundSchedules.stream().map(ScheduleDataModel::getScheduleId).collect(Collectors.toSet());
            String missingIds = scheduleIds.stream()
                    .filter(id -> !foundIds.contains(id))
                    .map(String::valueOf)
                    .collect(Collectors.joining(", "));
            throw new ScheduleNotFoundException(missingIds);
        }

        List<String> occupiedScheduleIds = foundSchedules.stream()
                .filter(this::isAssigned)
                .map(model -> model.getCourse().getName())
                .toList();

        if (!occupiedScheduleIds.isEmpty()) {
            throw new ScheduleNotAvailableException(String.join(", ", occupiedScheduleIds));
        }
        return foundSchedules;
    }

    private boolean isAssigned(ScheduleDataModel model) {
        return model.getCourse() != null;
    }
}