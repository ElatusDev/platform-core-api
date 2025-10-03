package com.akademiaplus.config;

import com.akademiaplus.employee.interfaceadapters.EmployeeRepository;
import com.akademiaplus.employee.usecases.EmployeeCreationUseCase;
import com.akademiaplus.users.collaborator.CollaboratorDataModel;
import com.akademiaplus.users.employee.EmployeeDataModel;
import com.akademiaplus.util.base.DataCleanUp;
import com.akademiaplus.util.base.DataFactory;
import com.akademiaplus.util.base.DataLoader;
import jakarta.persistence.EntityManager;
import openapi.akademiaplus.domain.user_management.dto.CollaboratorCreationRequestDTO;
import openapi.akademiaplus.domain.user_management.dto.EmployeeCreationRequestDTO;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PeopleDataLoderConfiguration {

    @Bean
    public DataLoader<EmployeeCreationRequestDTO, EmployeeDataModel, Integer> employeeDataLoader(
            EmployeeRepository repository,
            DataFactory<EmployeeCreationRequestDTO> employeeFactory,
            EmployeeCreationUseCase employeeCreationUseCase) {

        return new DataLoader<>(
                repository,
                employeeCreationUseCase::transform,
                employeeFactory
        );
    }

    @Bean
    public DataCleanUp<EmployeeDataModel, Integer> employeeDataCleanUp(
            EntityManager entityManager,
            EmployeeRepository repository) {

        DataCleanUp<EmployeeDataModel, Integer> cleanup =
                new DataCleanUp<>(entityManager);
        cleanup.setDataModel(EmployeeDataModel.class);
        cleanup.setRepository(repository);

        return cleanup;
    }

}
