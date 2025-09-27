package com.akademiaplus.util.factories;

import net.datafaker.Faker;
import openapi.akademiaplus.domain.user_management.dto.InternalAuthDTO;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Factory for generating InternalAuthDTO with all fields
 */
@Component
public class InternalAuthDTOFactory {

    private final Faker faker = new Faker();
    private final Set<String> usedUsernames = ConcurrentHashMap.newKeySet();

    /**
     * Generate complete InternalAuthDTO with all fields
     */
    public InternalAuthDTO createInternalAuth() {
        InternalAuthDTO auth = new InternalAuthDTO(
                generateRole()  // @NotNull - required parameter
        );

        // Set optional fields (@Nullable)
        auth.setUsername(generateUniqueUsername());
        auth.setPassword(generatePassword());

        return auth;
    }

    /**
     * Generate InternalAuthDTO with only required fields (role)
     */
    public InternalAuthDTO createInternalAuthMinimal() {
        return new InternalAuthDTO(generateRole());
        // username and password remain null
    }

    /**
     * Generate InternalAuthDTO with specific role
     */
    public InternalAuthDTO createInternalAuthWithRole(String role) {
        InternalAuthDTO auth = new InternalAuthDTO(role);
        auth.setUsername(generateUniqueUsername());
        auth.setPassword(generatePassword());
        return auth;
    }

    /**
     * Generate InternalAuthDTO without username (password only)
     */
    public InternalAuthDTO createInternalAuthWithoutUsername() {
        InternalAuthDTO auth = new InternalAuthDTO(generateRole());
        auth.setPassword(generatePassword());
        // username remains null
        return auth;
    }

    /**
     * Generate InternalAuthDTO without password (username only)
     */
    public InternalAuthDTO createInternalAuthWithoutPassword() {
        InternalAuthDTO auth = new InternalAuthDTO(generateRole());
        auth.setUsername(generateUniqueUsername());
        // password remains null
        return auth;
    }

    /**
     * Generate multiple InternalAuthDTO instances
     */
    public List<InternalAuthDTO> createInternalAuths(int count) {
        return IntStream.range(0, count)
                .mapToObj(i -> createInternalAuth())
                .collect(Collectors.toList());
    }

    // ========== Field Generation Methods ==========

    /**
     * Generate unique username - @Nullable
     * 90% chance of generating username, 10% null
     */
    private String generateUniqueUsername() {
        // 90% chance of having username
        if (faker.random().nextDouble() < 0.9) {
            String username;
            int attempts = 0;

            do {
                // Generate realistic username patterns
                String pattern = faker.options().option(
                        "firstlast",        // john.smith
                        "first_last",       // john_smith
                        "firstinitial_last", // j_smith
                        "first.lastinitial", // john.s
                        "employee_id"       // emp12345
                );

                username = generateUsernameByPattern(pattern);

                attempts++;
                if (attempts > 100) {
                    username = "user" + System.currentTimeMillis();
                    break;
                }

            } while (usedUsernames.contains(username));

            usedUsernames.add(username);
            return username;
        }
        return null; // 10% chance of null username
    }

    /**
     * Generate username based on pattern
     */
    private String generateUsernameByPattern(String pattern) {
        String firstName = faker.name().firstName().toLowerCase();
        String lastName = faker.name().lastName().toLowerCase();

        return switch (pattern) {
            case "firstlast" -> firstName + "." + lastName;
            case "first_last" -> firstName + "_" + lastName;
            case "firstinitial_last" -> firstName.charAt(0) + "_" + lastName;
            case "first.lastinitial" -> firstName + "." + lastName.charAt(0);
            case "employee_id" -> "emp" + faker.number().digits(5);
            default -> firstName + lastName;
        };
    }

    /**
     * Generate password - @Nullable
     * 85% chance of generating password, 15% null
     */
    private String generatePassword() {
        // 85% chance of having password
        if (faker.random().nextDouble() < 0.85) {
            // Generate secure-looking passwords
            String passwordType = faker.options().option(
                    "complex",      // Aa1!bcde
                    "simple",       // password123
                    "phrase",       // Coffee2024!
                    "random"        // aB3$kL9#
            );

            return generatePasswordByType(passwordType);
        }
        return null; // 15% chance of null password
    }

    /**
     * Generate password based on type
     */
    private String generatePasswordByType(String type) {
        return switch (type) {
            case "complex" -> {
                String base = faker.text().text(6, 10);
                yield base + faker.number().digits(2) + "!";
            }
            case "simple" -> faker.text().text(5, 8) + faker.number().digits(3);
            case "phrase" -> {
                String word = faker.text().text(5, 8);
                String capitalized = word.substring(0, 1).toUpperCase() + word.substring(1);
                yield capitalized + faker.date().birthday().getYear() + "!";
            }
            case "random" -> faker.internet().password(8, 12, true, true, true);
            default -> faker.internet().password();
        };
    }

    /**
     * Generate role - @NotNull (required field)
     */
    private String generateRole() {
        return faker.options().option(
                "ADMIN",
                "USER",
                "MANAGER",
                "EMPLOYEE",
                "HR",
                "FINANCE",
                "IT_SUPPORT",
                "DEVELOPER",
                "ANALYST",
                "SUPERVISOR",
                "DIRECTOR",
                "COORDINATOR",
                "SPECIALIST",
                "INTERN",
                "CONTRACTOR"
        );
    }

    // ========== Role-Specific Generators ==========

    /**
     * Generate admin-level InternalAuth
     */
    public InternalAuthDTO createAdminAuth() {
        InternalAuthDTO auth = new InternalAuthDTO("ADMIN");
        auth.setUsername("admin." + faker.name().lastName().toLowerCase());
        auth.setPassword(generateStrongPassword());
        return auth;
    }

    /**
     * Generate employee-level InternalAuth
     */
    public InternalAuthDTO createEmployeeAuth() {
        String[] employeeRoles = {"EMPLOYEE", "USER", "SPECIALIST", "ANALYST"};
        InternalAuthDTO auth = new InternalAuthDTO(faker.options().option(employeeRoles));
        auth.setUsername(generateUniqueUsername());
        auth.setPassword(generatePassword());
        return auth;
    }

    /**
     * Generate management-level InternalAuth
     */
    public InternalAuthDTO createManagementAuth() {
        String[] managementRoles = {"MANAGER", "DIRECTOR", "SUPERVISOR"};
        InternalAuthDTO auth = new InternalAuthDTO(faker.options().option(managementRoles));
        auth.setUsername("mgr." + faker.name().lastName().toLowerCase());
        auth.setPassword(generateStrongPassword());
        return auth;
    }

    /**
     * Generate strong password for high-privilege accounts
     */
    private String generateStrongPassword() {
        return faker.internet().password(12, 16, true, true, true);
    }

    // ========== Utility Methods ==========

    /**
     * Clear unique username constraints (useful for testing)
     */
    public void clearUniqueUsernames() {
        usedUsernames.clear();
    }

    /**
     * Get count of unique usernames generated
     */
    public int getUniqueUsernameCount() {
        return usedUsernames.size();
    }

    /**
     * Get all available roles
     */
    public List<String> getAvailableRoles() {
        return List.of(
                "ADMIN", "USER", "MANAGER", "EMPLOYEE", "HR", "FINANCE",
                "IT_SUPPORT", "DEVELOPER", "ANALYST", "SUPERVISOR",
                "DIRECTOR", "COORDINATOR", "SPECIALIST", "INTERN", "CONTRACTOR"
        );
    }

    /**
     * Check if role is valid
     */
    public boolean isValidRole(String role) {
        return getAvailableRoles().contains(role);
    }
}