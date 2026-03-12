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
import com.akademiaplus.customer.interfaceadapters.CustomerAuthRepository;
import com.akademiaplus.customer.interfaceadapters.TutorRepository;
import com.akademiaplus.customer.minorstudent.interfaceadapters.MinorStudentRepository;
import com.akademiaplus.customer.tutor.usecases.TutorCreationUseCase;
import com.akademiaplus.employee.interfaceadapters.EmployeeRepository;
import com.akademiaplus.employee.usecases.EmployeeCreationUseCase;
import com.akademiaplus.interfaceadapters.PersonPIIRepository;
import com.akademiaplus.internal.interfaceadapters.InternalAuthRepository;
import com.akademiaplus.security.CustomerAuthDataModel;
import com.akademiaplus.security.InternalAuthDataModel;
import com.akademiaplus.users.base.PersonPIIDataModel;
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
    public DataLoader<EmployeeCreationRequestDTO, EmployeeDataModel, EmployeeDataModel.EmployeeCompositeId> employeeDataLoader(
            EmployeeRepository repository,
            DataFactory<EmployeeCreationRequestDTO> employeeFactory,
            EmployeeCreationUseCase employeeCreationUseCase) {

        return new DataLoader<>(repository, employeeCreationUseCase::transform, employeeFactory);
    }

    @Bean
    public DataCleanUp<EmployeeDataModel, EmployeeDataModel.EmployeeCompositeId> employeeDataCleanUp(
            EntityManager entityManager,
            EmployeeRepository repository) {

        DataCleanUp<EmployeeDataModel, EmployeeDataModel.EmployeeCompositeId> cleanup = new DataCleanUp<>(entityManager);
        cleanup.setDataModel(EmployeeDataModel.class);
        cleanup.setRepository(repository);
        return cleanup;
    }

    // ── Collaborator ──

    @Bean
    public DataLoader<CollaboratorCreationRequestDTO, CollaboratorDataModel, CollaboratorDataModel.CollaboratorCompositeId> collaboratorDataLoader(
            CollaboratorRepository repository,
            DataFactory<CollaboratorCreationRequestDTO> collaboratorFactory,
            CollaboratorCreationUseCase collaboratorCreationUseCase) {

        return new DataLoader<>(repository, collaboratorCreationUseCase::transform, collaboratorFactory);
    }

    @Bean
    public DataCleanUp<CollaboratorDataModel, CollaboratorDataModel.CollaboratorCompositeId> collaboratorDataCleanUp(
            EntityManager entityManager,
            CollaboratorRepository repository) {

        DataCleanUp<CollaboratorDataModel, CollaboratorDataModel.CollaboratorCompositeId> cleanup = new DataCleanUp<>(entityManager);
        cleanup.setDataModel(CollaboratorDataModel.class);
        cleanup.setRepository(repository);
        return cleanup;
    }

    // ── AdultStudent ──

    @Bean
    public DataLoader<AdultStudentCreationRequestDTO, AdultStudentDataModel, AdultStudentDataModel.AdultStudentCompositeId> adultStudentDataLoader(
            AdultStudentRepository repository,
            DataFactory<AdultStudentCreationRequestDTO> adultStudentFactory,
            AdultStudentCreationUseCase adultStudentCreationUseCase) {

        return new DataLoader<>(repository, adultStudentCreationUseCase::transform, adultStudentFactory);
    }

    @Bean
    public DataCleanUp<AdultStudentDataModel, AdultStudentDataModel.AdultStudentCompositeId> adultStudentDataCleanUp(
            EntityManager entityManager,
            AdultStudentRepository repository) {

        DataCleanUp<AdultStudentDataModel, AdultStudentDataModel.AdultStudentCompositeId> cleanup = new DataCleanUp<>(entityManager);
        cleanup.setDataModel(AdultStudentDataModel.class);
        cleanup.setRepository(repository);
        return cleanup;
    }

    // ── Tutor ──

    @Bean
    public DataLoader<TutorCreationRequestDTO, TutorDataModel, TutorDataModel.TutorCompositeId> tutorDataLoader(
            TutorRepository repository,
            DataFactory<TutorCreationRequestDTO> tutorFactory,
            TutorCreationUseCase tutorCreationUseCase) {

        return new DataLoader<>(repository, tutorCreationUseCase::transformTutor, tutorFactory);
    }

    @Bean
    public DataCleanUp<TutorDataModel, TutorDataModel.TutorCompositeId> tutorDataCleanUp(
            EntityManager entityManager,
            TutorRepository repository) {

        DataCleanUp<TutorDataModel, TutorDataModel.TutorCompositeId> cleanup = new DataCleanUp<>(entityManager);
        cleanup.setDataModel(TutorDataModel.class);
        cleanup.setRepository(repository);
        return cleanup;
    }

    // ── MinorStudent ──

    @Bean
    public DataLoader<MinorStudentCreationRequestDTO, MinorStudentDataModel, MinorStudentDataModel.MinorStudentCompositeId> minorStudentDataLoader(
            MinorStudentRepository repository,
            DataFactory<MinorStudentCreationRequestDTO> minorStudentFactory,
            TutorCreationUseCase tutorCreationUseCase) {

        return new DataLoader<>(repository, tutorCreationUseCase::transformMinorStudent, minorStudentFactory);
    }

    @Bean
    public DataCleanUp<MinorStudentDataModel, MinorStudentDataModel.MinorStudentCompositeId> minorStudentDataCleanUp(
            EntityManager entityManager,
            MinorStudentRepository repository) {

        DataCleanUp<MinorStudentDataModel, MinorStudentDataModel.MinorStudentCompositeId> cleanup = new DataCleanUp<>(entityManager);
        cleanup.setDataModel(MinorStudentDataModel.class);
        cleanup.setRepository(repository);
        return cleanup;
    }

    // ── Shared tables (cleanup-only) ──

    @Bean
    public DataCleanUp<InternalAuthDataModel, InternalAuthDataModel.InternalAuthCompositeId> internalAuthDataCleanUp(
            EntityManager entityManager,
            InternalAuthRepository repository) {

        DataCleanUp<InternalAuthDataModel, InternalAuthDataModel.InternalAuthCompositeId> cleanup = new DataCleanUp<>(entityManager);
        cleanup.setDataModel(InternalAuthDataModel.class);
        cleanup.setRepository(repository);
        return cleanup;
    }

    @Bean
    public DataCleanUp<CustomerAuthDataModel, CustomerAuthDataModel.CustomerAuthCompositeId> customerAuthDataCleanUp(
            EntityManager entityManager,
            CustomerAuthRepository repository) {

        DataCleanUp<CustomerAuthDataModel, CustomerAuthDataModel.CustomerAuthCompositeId> cleanup = new DataCleanUp<>(entityManager);
        cleanup.setDataModel(CustomerAuthDataModel.class);
        cleanup.setRepository(repository);
        return cleanup;
    }

    @Bean
    public DataCleanUp<PersonPIIDataModel, PersonPIIDataModel.PersonPIICompositeId> personPIIDataCleanUp(
            EntityManager entityManager,
            PersonPIIRepository repository) {

        DataCleanUp<PersonPIIDataModel, PersonPIIDataModel.PersonPIICompositeId> cleanup = new DataCleanUp<>(entityManager);
        cleanup.setDataModel(PersonPIIDataModel.class);
        cleanup.setRepository(repository);
        return cleanup;
    }
}
