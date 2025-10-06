package com.akademiaplus.util.mock.users;

import net.datafaker.Faker;
import org.apache.catalina.core.ApplicationContext;
import org.openapitools.jackson.nullable.JsonNullable;
import org.springframework.stereotype.Component;
import java.time.LocalDate;
import java.util.*;

/**
 * Handles the actual data generation using DataFaker
 */
@Component
public class EmployeeDataGenerator {

    private final Faker faker;
    private final Random random;

    private static final List<String> EMPLOYEE_TYPES = Arrays.asList(
            "INSTRUCTOR", "ADMINISTRATOR", "COORDINATOR", "MANAGER", "ASSISTANT"
    );

    private static final List<String> ROLES = Arrays.asList(
            "EMPLOYEE", "ADMIN", "SUPERVISOR", "MANAGER", "USER"
    );

    public EmployeeDataGenerator() {
        this.faker = new Faker(new Locale("es", "MX"));
        this.random = new Random();
    }

    public String firstName() {
        return faker.name().firstName();
    }

    public String lastName() {
        return faker.name().lastName();
    }

    public String email(String firstName, String lastName) {
        String clean = (firstName.charAt(0) + lastName).toLowerCase().replaceAll("[^a-z0-9]", "");
        // Using username provider instead of deprecated emailAddress with parameter
        return clean + "@" + faker.internet().domainName();
    }

    public String username(String firstName, String lastName) {
        String base = (firstName.charAt(0) + lastName).toLowerCase().replaceAll("[^a-z0-9]", "");
        return base + faker.number().numberBetween(100, 999);
    }

    public String password() {
        // DataFaker 2.5.0 - generate strong password without deprecated methods
        // Create a password with letters, numbers, and special chars
        String upper = faker.text().text(2, 3).toUpperCase().replaceAll("[^A-Z]", "AB");
        String lower = faker.text().text(4, 6).toLowerCase().replaceAll("[^a-z]", "xyz");
        String numbers = String.valueOf(faker.number().numberBetween(1000, 9999));
        String special = faker.options().option("@", "#", "$", "%", "!", "&");

        return upper + lower + numbers + special + faker.number().numberBetween(10, 99);
    }

    public String phoneNumber() {
        // Using phoneNumber() without deprecated cellPhone()
        return faker.phoneNumber().phoneNumber();
    }

    public String address() {
        // Using fullAddress() or building it manually
        return faker.address().streetAddress() + ", " +
                faker.address().city() + ", " +
                faker.address().state();
    }

    public String zipCode() {
        return String.format("%05d", faker.number().numberBetween(10000, 99999));
    }

    public LocalDate birthdate() {
        // DataFaker 2.5.0 - correct non-deprecated approach
        // Generate age between 22-65 years
        int age = faker.number().numberBetween(22, 66);
        return LocalDate.now().minusYears(age)
                .minusDays(faker.number().numberBetween(0, 365));
    }

    public LocalDate entryDate() {
        // DataFaker 2.5.0 - generate entry date within last 5 years
        int daysAgo = faker.number().numberBetween(0, 1825); // 0 to 5 years
        return LocalDate.now().minusDays(daysAgo);
    }

    public String employeeType() {
        return EMPLOYEE_TYPES.get(random.nextInt(EMPLOYEE_TYPES.size()));
    }

    public String role() {
        return ROLES.get(random.nextInt(ROLES.size()));
    }

    public JsonNullable<byte[]> profilePicture() {
        // Simple 1x1 transparent PNG
        String base64 = "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNkYPhfDwAChwGA60e6kgAAAABJRU5ErkJggg==";
        return JsonNullable.of(base64.getBytes());
    }
}
