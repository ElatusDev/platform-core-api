/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.config;

import com.akademiaplus.collaborator.interfaceadapters.CollaboratorRepository;
import com.akademiaplus.collaborator.usecases.CollaboratorCreationUseCase;
import com.akademiaplus.customer.adultstudent.interfaceadapters.AdultStudentRepository;
import com.akademiaplus.customer.adultstudent.usecases.AdultStudentCreationUseCase;
import com.akademiaplus.employee.interfaceadapters.EmployeeRepository;
import com.akademiaplus.employee.usecases.EmployeeCreationUseCase;
import com.akademiaplus.users.collaborator.CollaboratorDataModel;
import com.akademiaplus.users.customer.AdultStudentDataModel;
import com.akademiaplus.users.employee.EmployeeDataModel;
import com.akademiaplus.util.base.DataCleanUp;
import com.akademiaplus.util.base.DataFactory;
import com.akademiaplus.util.base.DataLoader;
import jakarta.persistence.EntityManager;
import openapi.akademiaplus.domain.user.management.dto.AdultStudentCreationRequestDTO;
import openapi.akademiaplus.domain.user.management.dto.CollaboratorCreationRequestDTO;
import openapi.akademiaplus.domain.user.management.dto.EmployeeCreationRequestDTO;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PeopleDataLoaderConfiguration {

    // ── Employee ──

    @Bean
    public DataLoader<EmployeeCreationRequestDTO, EmployeeDataModel, Long> employeeDataLoader(
            EmployeeRepository repository,
            DataFactory<EmployeeCreationRequestDTO> employeeFactory,
            EmployeeCreationUseCase employeeCreationUseCase) {

        return new DataLoader<>(repository, employeeCreationUseCase::transform, employeeFactory);
    }

    @Bean
    public DataCleanUp<EmployeeDataModel, Long> employeeDataCleanUp(
            EntityManager entityManager,
            EmployeeRepository repository) {

        DataCleanUp<EmployeeDataModel, Long> cleanup = new DataCleanUp<>(entityManager);
        cleanup.setDataModel(EmployeeDataModel.class);
        cleanup.setRepository(repository);
        return cleanup;
    }

    // ── Collaborator ──

    @Bean
    public DataLoader<CollaboratorCreationRequestDTO, CollaboratorDataModel, Long> collaboratorDataLoader(
            CollaboratorRepository repository,
            DataFactory<CollaboratorCreationRequestDTO> collaboratorFactory,
            CollaboratorCreationUseCase collaboratorCreationUseCase) {

        return new DataLoader<>(repository, collaboratorCreationUseCase::transform, collaboratorFactory);
    }

    @Bean
    public DataCleanUp<CollaboratorDataModel, Long> collaboratorDataCleanUp(
            EntityManager entityManager,
            CollaboratorRepository repository) {

        DataCleanUp<CollaboratorDataModel, Long> cleanup = new DataCleanUp<>(entityManager);
        cleanup.setDataModel(CollaboratorDataModel.class);
        cleanup.setRepository(repository);
        return cleanup;
    }

    // ── AdultStudent ──

    @Bean
    public DataLoader<AdultStudentCreationRequestDTO, AdultStudentDataModel, Long> adultStudentDataLoader(
            AdultStudentRepository repository,
            DataFactory<AdultStudentCreationRequestDTO> adultStudentFactory,
            AdultStudentCreationUseCase adultStudentCreationUseCase) {

        return new DataLoader<>(repository, adultStudentCreationUseCase::transform, adultStudentFactory);
    }

    @Bean
    public DataCleanUp<AdultStudentDataModel, Long> adultStudentDataCleanUp(
            EntityManager entityManager,
            AdultStudentRepository repository) {

        DataCleanUp<AdultStudentDataModel, Long> cleanup = new DataCleanUp<>(entityManager);
        cleanup.setDataModel(AdultStudentDataModel.class);
        cleanup.setRepository(repository);
        return cleanup;
    }
}
