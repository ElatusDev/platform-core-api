/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.config;

import com.akademiaplus.collaborator.usecases.CollaboratorCreationUseCase;
import com.akademiaplus.collaborator.usecases.CollaboratorUpdateUseCase;
import com.akademiaplus.customer.adultstudent.usecases.AdultStudentCreationUseCase;
import com.akademiaplus.customer.adultstudent.usecases.AdultStudentUpdateUseCase;
import com.akademiaplus.customer.minorstudent.usecases.MinorStudentUpdateUseCase;
import com.akademiaplus.customer.tutor.usecases.TutorCreationUseCase;
import com.akademiaplus.customer.tutor.usecases.TutorUpdateUseCase;
import com.akademiaplus.employee.usecases.EmployeeCreationUseCase;
import com.akademiaplus.employee.usecases.EmployeeUpdateUseCase;
import com.akademiaplus.security.InternalAuthDataModel;
import com.akademiaplus.users.base.PersonPIIDataModel;
import com.akademiaplus.users.collaborator.CollaboratorDataModel;
import com.akademiaplus.users.customer.AdultStudentDataModel;
import com.akademiaplus.users.customer.MinorStudentDataModel;
import com.akademiaplus.users.customer.TutorDataModel;
import com.akademiaplus.users.employee.EmployeeDataModel;
import openapi.akademiaplus.domain.user.management.dto.*;
import org.modelmapper.ModelMapper;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;
/**
 * Registers module-specific named {@link org.modelmapper.TypeMap TypeMaps}
 * for all people-entity DTO → DataModel conversions.
 * <p>
 * Each entity's use case maps DTO fields in three separate calls:
 * <ol>
 *   <li>DTO → entity model (via named TypeMap — this config)</li>
 *   <li>DTO → {@code PersonPIIDataModel} (default unnamed TypeMap)</li>
 *   <li>Auth model wired manually (CustomerAuth or InternalAuth)</li>
 * </ol>
 * The named TypeMaps declared here prevent ModelMapper from deep-matching
 * DTO fields into nested JPA relationships ({@code personPII}, {@code customerAuth},
 * {@code internalAuth}, {@code tutor}), which would create phantom detached
 * entities and corrupt persistence operations.
 * <p>
 * Implicit mapping is temporarily disabled during registration so that
 * skip rules are applied <em>before</em> ModelMapper eagerly resolves
 * nested paths like {@code tutorId → tutor.tutorId}.
 *
 * @see TutorCreationUseCase
 * @see AdultStudentCreationUseCase
 * @see EmployeeCreationUseCase
 * @see CollaboratorCreationUseCase
 */
@Configuration
public class PeopleModelMapperConfiguration {

    private final ModelMapper modelMapper;

    public PeopleModelMapperConfiguration(ModelMapper modelMapper) {
        this.modelMapper = modelMapper;
    }

    @PostConstruct
    void registerTypeMaps() {
        modelMapper.getConfiguration().setImplicitMappingEnabled(false);

        registerTutorMap();
        registerTutorUpdateMap();
        registerMinorStudentMap();
        registerMinorStudentUpdateMap();
        registerAdultStudentMap();
        registerAdultStudentUpdateMap();
        registerEmployeeMap();
        registerEmployeeUpdateMap();
        registerCollaboratorMap();
        registerCollaboratorUpdateMap();

        modelMapper.getConfiguration().setImplicitMappingEnabled(true);
    }

    private void registerTutorMap() {
        modelMapper.createTypeMap(
                TutorCreationRequestDTO.class,
                TutorDataModel.class,
                TutorCreationUseCase.TUTOR_MAP_NAME
        ).addMappings(mapper -> {
            mapper.skip(TutorDataModel::setTutorId);
            mapper.skip(TutorDataModel::setPersonPII);
            mapper.skip(TutorDataModel::setCustomerAuth);
        }).implicitMappings();
    }

    private void registerTutorUpdateMap() {
        modelMapper.createTypeMap(
                TutorUpdateRequestDTO.class,
                TutorDataModel.class,
                TutorUpdateUseCase.MAP_NAME
        ).addMappings(mapper -> {
            mapper.skip(TutorDataModel::setTutorId);
            mapper.skip(TutorDataModel::setPersonPII);
            mapper.skip(TutorDataModel::setCustomerAuth);
        }).implicitMappings();
    }

    private void registerMinorStudentMap() {
        modelMapper.createTypeMap(
                MinorStudentCreationRequestDTO.class,
                MinorStudentDataModel.class,
                TutorCreationUseCase.MINOR_STUDENT_MAP_NAME
        ).addMappings(mapper -> {
            mapper.skip(MinorStudentDataModel::setMinorStudentId);
            mapper.skip(MinorStudentDataModel::setTutor);
            mapper.skip(MinorStudentDataModel::setCustomerAuth);
            mapper.skip(MinorStudentDataModel::setPersonPII);
        }).implicitMappings();
    }

    private void registerMinorStudentUpdateMap() {
        modelMapper.createTypeMap(
                MinorStudentUpdateRequestDTO.class,
                MinorStudentDataModel.class,
                MinorStudentUpdateUseCase.MAP_NAME
        ).addMappings(mapper -> {
            mapper.skip(MinorStudentDataModel::setMinorStudentId);
            mapper.skip(MinorStudentDataModel::setTutor);
            mapper.skip(MinorStudentDataModel::setCustomerAuth);
            mapper.skip(MinorStudentDataModel::setPersonPII);
        }).implicitMappings();
    }

    private void registerAdultStudentMap() {
        modelMapper.createTypeMap(
                AdultStudentCreationRequestDTO.class,
                AdultStudentDataModel.class,
                AdultStudentCreationUseCase.MAP_NAME
        ).addMappings(mapper -> {
            mapper.skip(AdultStudentDataModel::setAdultStudentId);
            mapper.skip(AdultStudentDataModel::setPersonPII);
            mapper.skip(AdultStudentDataModel::setCustomerAuth);
        }).implicitMappings();
    }

    private void registerAdultStudentUpdateMap() {
        modelMapper.createTypeMap(
                AdultStudentUpdateRequestDTO.class,
                AdultStudentDataModel.class,
                AdultStudentUpdateUseCase.MAP_NAME
        ).addMappings(mapper -> {
            mapper.skip(AdultStudentDataModel::setAdultStudentId);
            mapper.skip(AdultStudentDataModel::setPersonPII);
            mapper.skip(AdultStudentDataModel::setCustomerAuth);
        }).implicitMappings();
    }

    private void registerEmployeeMap() {
        modelMapper.createTypeMap(
                EmployeeCreationRequestDTO.class,
                EmployeeDataModel.class,
                EmployeeCreationUseCase.MAP_NAME
        ).addMappings(mapper -> {
            mapper.skip(EmployeeDataModel::setEmployeeId);
            mapper.skip(EmployeeDataModel::setPersonPII);
            mapper.skip(EmployeeDataModel::setInternalAuth);
        }).implicitMappings();
    }

    private void registerEmployeeUpdateMap() {
        modelMapper.createTypeMap(
                EmployeeUpdateRequestDTO.class,
                EmployeeDataModel.class,
                EmployeeUpdateUseCase.MAP_NAME
        ).addMappings(mapper -> {
            mapper.skip(EmployeeDataModel::setEmployeeId);
            mapper.skip(EmployeeDataModel::setPersonPII);
            mapper.skip(EmployeeDataModel::setInternalAuth);
        }).implicitMappings();
    }

    private void registerCollaboratorMap() {
        modelMapper.createTypeMap(
                CollaboratorCreationRequestDTO.class,
                CollaboratorDataModel.class,
                CollaboratorCreationUseCase.TYPE_MAP
        ).addMappings(mapper -> {
            mapper.skip(CollaboratorDataModel::setCollaboratorId);
            mapper.skip(CollaboratorDataModel::setPersonPII);
            mapper.skip(CollaboratorDataModel::setInternalAuth);
        }).implicitMappings();
    }

    private void registerCollaboratorUpdateMap() {
        modelMapper.createTypeMap(
                CollaboratorUpdateRequestDTO.class,
                CollaboratorDataModel.class,
                CollaboratorUpdateUseCase.MAP_NAME
        ).addMappings(mapper -> {
            mapper.skip(CollaboratorDataModel::setCollaboratorId);
            mapper.skip(CollaboratorDataModel::setPersonPII);
            mapper.skip(CollaboratorDataModel::setInternalAuth);
        }).implicitMappings();
    }
}
