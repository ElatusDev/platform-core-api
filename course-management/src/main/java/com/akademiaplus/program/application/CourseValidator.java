package com.akademiaplus.program.application;

import com.akademiaplus.collaborator.interfaceadapters.CollaboratorRepository;
import com.akademiaplus.courses.program.ScheduleDataModel;
import com.akademiaplus.exception.CollaboratorNotFoundException;
import com.akademiaplus.exception.ScheduleNotAvailableException;
import com.akademiaplus.infra.persistence.config.TenantContextHolder;
import com.akademiaplus.program.interfaceadapters.ScheduleRepository;
import com.akademiaplus.users.collaborator.CollaboratorDataModel;
import com.akademiaplus.utilities.EntityType;
import com.akademiaplus.utilities.exceptions.EntityNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class CourseValidator {

    private final CollaboratorRepository collaboratorRepository;
    private final ScheduleRepository scheduleRepository;
    private final TenantContextHolder tenantContextHolder;

    public CourseValidator(CollaboratorRepository collaboratorRepository,
                           ScheduleRepository scheduleRepository,
                           TenantContextHolder tenantContextHolder) {
        this.collaboratorRepository = collaboratorRepository;
        this.scheduleRepository = scheduleRepository;
        this.tenantContextHolder = tenantContextHolder;
    }

    public List<CollaboratorDataModel> validateCollaboratorsExist(List<Long> collaboratorIds) {
        Long tenantId = tenantContextHolder.getTenantId()
                .orElseThrow(() -> new IllegalArgumentException("Tenant context is required"));
        List<CollaboratorDataModel.CollaboratorCompositeId> compositeIds = collaboratorIds.stream()
                .map(id -> new CollaboratorDataModel.CollaboratorCompositeId(tenantId, id))
                .toList();
        List<CollaboratorDataModel> foundCollaborator = collaboratorRepository.findAllById(compositeIds);

        if (foundCollaborator.size() != collaboratorIds.size()) {
            Set<Long> foundIds = foundCollaborator.stream().map(CollaboratorDataModel::getCollaboratorId)
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
     * Throws EntityNotFoundException if any ID is not found.
     * Throws ScheduleNotAvailableException if any schedule is already assigned to a course.
     *
     * @param scheduleIds List of schedule IDs to validate.
     * @return List of found Schedule data models that are available.
     */
    public List<ScheduleDataModel> validateSchedulesAvailable(List<Long> scheduleIds) {
        if (scheduleIds == null || scheduleIds.isEmpty()) {
            return List.of();
        }

        Long tenantId = tenantContextHolder.getTenantId()
                .orElseThrow(() -> new IllegalArgumentException("Tenant context is required"));
        List<ScheduleDataModel.ScheduleCompositeId> compositeIds = scheduleIds.stream()
                .map(id -> new ScheduleDataModel.ScheduleCompositeId(tenantId, id))
                .toList();
        List<ScheduleDataModel> foundSchedules = scheduleRepository.findAllById(compositeIds);
        if (foundSchedules.size() != scheduleIds.size()) {
            Set<Long> foundIds = foundSchedules.stream().map(ScheduleDataModel::getScheduleId).collect(Collectors.toSet());
            String missingIds = scheduleIds.stream()
                    .filter(id -> !foundIds.contains(id))
                    .map(String::valueOf)
                    .collect(Collectors.joining(", "));
            throw new EntityNotFoundException(EntityType.SCHEDULE, missingIds);
        }

        List<String> occupiedScheduleIds = foundSchedules.stream()
                .filter(this::isAssigned)
                .map(model -> model.getCourse().getCourseName())
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