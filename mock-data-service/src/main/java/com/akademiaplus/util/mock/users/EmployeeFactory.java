package com.akademiaplus.util.mock.users;

import com.akademiaplus.util.base.DataFactory;
import openapi.akademiaplus.domain.user.management.dto.EmployeeCreationRequestDTO;
import org.springframework.stereotype.Component;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Factory for creating EmployeeCreationRequestDTO instances
 */
@Component
@RequiredArgsConstructor
public class EmployeeFactory implements DataFactory<EmployeeCreationRequestDTO> {

    private final EmployeeDataGenerator generator;

    @Override
    public List<EmployeeCreationRequestDTO> generate(int count) {
        List<EmployeeCreationRequestDTO> employees = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            employees.add(createEmployee());
        }
        return employees;
    }

    private EmployeeCreationRequestDTO createEmployee() {
        String firstName = generator.firstName();
        String lastName = generator.lastName();

        EmployeeCreationRequestDTO dto = new EmployeeCreationRequestDTO(
                generator.employeeType(),
                generator.birthdate(),
                generator.entryDate(),
                firstName,
                lastName,
                generator.email(firstName, lastName),
                generator.phoneNumber(),
                generator.address(),
                generator.zipCode(),
                generator.username(firstName, lastName),
                generator.password(),
                generator.role()
        );
        dto.setEmployeeType(generator.employeeType());
        dto.setProfilePicture(generator.profilePicture());
        return dto;
    }

}
