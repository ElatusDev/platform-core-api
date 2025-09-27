package com.akademiaplus.util.factories;

import net.datafaker.Faker;
import openapi.akademiaplus.domain.user_management.dto.EmployeeCreationRequestDTO;
import openapi.akademiaplus.domain.user_management.dto.InternalAuthDTO;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Factory for generating EmployeeCreationRequestDTO with all fields
 */
@Component
public class EmployeeCreationRequestDTOFactory {

    private final Faker faker = new Faker();
    private final Set<String> usedEmails = ConcurrentHashMap.newKeySet();

    /**
     * Generate complete EmployeeCreationRequestDTO with all fields including InternalAuth
     */
    public EmployeeCreationRequestDTO createEmployee() {
        EmployeeCreationRequestDTO employee = new EmployeeCreationRequestDTO(
                generateFirstName(),      // @NotNull @Size(max = 30)
                generateLastName(),       // @NotNull @Size(max = 30)
                generateBirthDate(),      // @NotNull @Valid
                generateUniqueEmail(),    // @NotNull @Email @Size(max = 50)
                generatePhone(),          // @NotNull @Size(max = 15)
                generateAddress(),        // @NotNull @Size(max = 50)
                generateZipCode(),        // @NotNull @Size(max = 8)
                generateEmployeeType()    // @NotNull
        );

        // Set optional InternalAuth field (@Nullable)
        employee.setInternalAuth(generateInternalAuth());

        return employee;
    }

    /**
     * Generate employee without InternalAuth (null)
     */
    public EmployeeCreationRequestDTO createEmployeeWithoutAuth() {
        return new EmployeeCreationRequestDTO(
                generateFirstName(),
                generateLastName(),
                generateBirthDate(),
                generateUniqueEmail(),
                generatePhone(),
                generateAddress(),
                generateZipCode(),
                generateEmployeeType()
        );
        // internalAuth remains null
    }

    /**
     * Generate multiple employees
     */
    public List<EmployeeCreationRequestDTO> createEmployees(int count) {
        return IntStream.range(0, count)
                .mapToObj(i -> createEmployee())
                .collect(Collectors.toList());
    }

    /**
     * Generate multiple employees without auth
     */
    public List<EmployeeCreationRequestDTO> createEmployeesWithoutAuth(int count) {
        return IntStream.range(0, count)
                .mapToObj(i -> createEmployeeWithoutAuth())
                .collect(Collectors.toList());
    }

    // ========== Field Generation Methods ==========

    /**
     * Generate firstName - @NotNull @Size(max = 30)
     */
    private String generateFirstName() {
        String name = faker.name().firstName();
        return name.length() > 30 ? name.substring(0, 30) : name;
    }

    /**
     * Generate lastName - @NotNull @Size(max = 30)
     */
    private String generateLastName() {
        String name = faker.name().lastName();
        return name.length() > 30 ? name.substring(0, 30) : name;
    }

    /**
     * Generate birthDate - @NotNull @Valid
     * Generates realistic employee birth date (age 18-65)
     */
    private LocalDate generateBirthDate() {
        int age = faker.number().numberBetween(18, 65);
        return LocalDate.now()
                .minusYears(age)
                .minusDays(faker.number().numberBetween(0, 365));
    }

    /**
     * Generate unique email - @NotNull @Size(max = 50) @Email
     */
    private String generateUniqueEmail() {
        String email;
        int attempts = 0;

        do {
            String firstName = faker.name().firstName().toLowerCase()
                    .replaceAll("[^a-z]", "");
            String lastName = faker.name().lastName().toLowerCase()
                    .replaceAll("[^a-z]", "");
            String domain = faker.options().option("company.com", "makani.org", "corp.net");

            email = firstName + "." + lastName + "@" + domain;

            // Ensure @Size(max = 50) constraint
            if (email.length() > 50) {
                String shortFirst = firstName.length() > 5 ? firstName.substring(0, 5) : firstName;
                String shortLast = lastName.length() > 5 ? lastName.substring(0, 5) : lastName;
                email = shortFirst + "." + shortLast + "@" + domain;
            }

            // Prevent infinite loop
            attempts++;
            if (attempts > 100) {
                email = "emp" + System.currentTimeMillis() + "@co.com";
                break;
            }

        } while (usedEmails.contains(email));

        usedEmails.add(email);
        return email;
    }

    /**
     * Generate phone - @NotNull @Size(max = 15)
     */
    private String generatePhone() {
        String phone = faker.phoneNumber().phoneNumber();
        // Clean non-standard characters and ensure max length
        phone = phone.replaceAll("[^0-9+()\\-\\s]", "");
        return phone.length() > 15 ? phone.substring(0, 15) : phone;
    }

    /**
     * Generate address - @NotNull @Size(max = 50)
     */
    private String generateAddress() {
        String address = faker.address().streetAddress();
        return address.length() > 50 ? address.substring(0, 50) : address;
    }

    /**
     * Generate zipCode - @NotNull @Size(max = 8)
     */
    private String generateZipCode() {
        String zip = faker.address().zipCode();
        return zip.length() > 8 ? zip.substring(0, 8) : zip;
    }

    /**
     * Generate employeeType - @NotNull
     */
    private String generateEmployeeType() {
        return faker.options().option(
                "FULL_TIME",
                "PART_TIME"
        );
    }

    /**
     * Generate InternalAuthDTO - @Nullable @Valid
     * 70% chance of having auth, 30% null
     */
    private InternalAuthDTO generateInternalAuth() {
        // 70% chance of including internal auth
        if (faker.random().nextDouble() < 0.7) {
            return createInternalAuth();
        }
        return null;
    }

    /**
     * Create InternalAuthDTO instance
     * Note: This method assumes InternalAuthDTO structure - adjust as needed
     */
    private InternalAuthDTO createInternalAuth() {
        // Since we don't have the InternalAuthDTO structure,
        // this is a placeholder implementation
        InternalAuthDTO auth = new InternalAuthDTO();

        // Example fields - adjust based on actual InternalAuthDTO structure:
        // auth.setUsername(generateUsername());
        // auth.setPassword(generatePassword());
        // auth.setRole(generateRole());

        return auth;
    }

    // ========== Utility Methods ==========

    /**
     * Clear unique email constraints (useful for testing)
     */
    public void clearUniqueEmails() {
        usedEmails.clear();
    }

    /**
     * Get count of unique emails generated
     */
    public int getUniqueEmailCount() {
        return usedEmails.size();
    }

    /**
     * Generate employee with specific employee type
     */
    public EmployeeCreationRequestDTO createEmployeeWithType(String employeeType) {
        EmployeeCreationRequestDTO employee = new EmployeeCreationRequestDTO(
                generateFirstName(),
                generateLastName(),
                generateBirthDate(),
                generateUniqueEmail(),
                generatePhone(),
                generateAddress(),
                generateZipCode(),
                employeeType  // Use specified type
        );

        employee.setInternalAuth(generateInternalAuth());
        return employee;
    }

    /**
     * Generate employee with specific age range
     */
    public EmployeeCreationRequestDTO createEmployeeWithAge(int minAge, int maxAge) {
        if (minAge < 18 || maxAge > 80 || minAge >= maxAge) {
            throw new IllegalArgumentException("Invalid age range. Must be 18-80 and minAge < maxAge");
        }

        int age = faker.number().numberBetween(minAge, maxAge);
        LocalDate birthDate = LocalDate.now()
                .minusYears(age)
                .minusDays(faker.number().numberBetween(0, 365));

        EmployeeCreationRequestDTO employee = new EmployeeCreationRequestDTO(
                generateFirstName(),
                generateLastName(),
                birthDate,  // Use specific birth date
                generateUniqueEmail(),
                generatePhone(),
                generateAddress(),
                generateZipCode(),
                generateEmployeeType()
        );

        employee.setInternalAuth(generateInternalAuth());
        return employee;
    }
}