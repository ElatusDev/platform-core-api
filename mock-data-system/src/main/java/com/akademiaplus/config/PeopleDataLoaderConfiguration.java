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
import com.akademiaplus.customer.interfaceadapters.TutorRepository;
import com.akademiaplus.customer.minorstudent.interfaceadapters.MinorStudentRepository;
import com.akademiaplus.customer.tutor.usecases.TutorCreationUseCase;
import com.akademiaplus.employee.interfaceadapters.EmployeeRepository;
import com.akademiaplus.employee.usecases.EmployeeCreationUseCase;
import com.akademiaplus.users.collaborator.CollaboratorDataModel;
import com.akademiaplus.users.customer.AdultStudentDataModel;
import com.akademiaplus.users.customer.MinorStudentDataModel;
import com.akademiaplus.users.customer.TutorDataModel;
import com.akademiaplus.users.employee.EmployeeDataModel;
import com.akademiaplus.util.base.DataCleanUp;
import com.akademiaplus.util.base.DataFactory;
import com.akademiaplus.util.base.DataLoader;
import jakarta.persistence.EntityManager;
import openapi.akademiaplus.domain.user.management.dto.AdultStudentCreationRequestDTO;
import openapi.akademiaplus.domain.user.management.dto.CollaboratorCreationRequestDTO;
import openapi.akademiaplus.domain.user.management.dto.EmployeeCreationRequestDTO;
import openapi.akademiaplus.domain.user.management.dto.MinorStudentCreationRequestDTO;
import openapi.akademiaplus.domain.user.management.dto.TutorCreationRequestDTO;
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

    // ── Tutor ──

    @Bean
    public DataLoader<TutorCreationRequestDTO, TutorDataModel, Long> tutorDataLoader(
            TutorRepository repository,
            DataFactory<TutorCreationRequestDTO> tutorFactory,
            TutorCreationUseCase tutorCreationUseCase) {

        return new DataLoader<>(repository, tutorCreationUseCase::transformTutor, tutorFactory);
    }

    @Bean
    public DataCleanUp<TutorDataModel, Long> tutorDataCleanUp(
            EntityManager entityManager,
            TutorRepository repository) {

        DataCleanUp<TutorDataModel, Long> cleanup = new DataCleanUp<>(entityManager);
        cleanup.setDataModel(TutorDataModel.class);
        cleanup.setRepository(repository);
        return cleanup;
    }

    // ── MinorStudent ──

    @Bean
    public DataLoader<MinorStudentCreationRequestDTO, MinorStudentDataModel, Long> minorStudentDataLoader(
            MinorStudentRepository repository,
            DataFactory<MinorStudentCreationRequestDTO> minorStudentFactory,
            TutorCreationUseCase tutorCreationUseCase) {

        return new DataLoader<>(repository, tutorCreationUseCase::transformMinorStudent, minorStudentFactory);
    }

    @Bean
    public DataCleanUp<MinorStudentDataModel, Long> minorStudentDataCleanUp(
            EntityManager entityManager,
            MinorStudentRepository repository) {

        DataCleanUp<MinorStudentDataModel, Long> cleanup = new DataCleanUp<>(entityManager);
        cleanup.setDataModel(MinorStudentDataModel.class);
        cleanup.setRepository(repository);
        return cleanup;
    }
}
