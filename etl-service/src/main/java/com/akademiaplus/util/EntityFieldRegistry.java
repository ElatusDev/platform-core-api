/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.util;

import com.akademiaplus.domain.MigrationEntityType;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Static registry mapping each {@link MigrationEntityType} to its target field definitions.
 *
 * <p>Field definitions are derived from the actual JPA {@code @Column} annotations
 * in the multi-tenant-data module. This registry serves as the source of truth for
 * Claude API schema context and row validation.</p>
 */
@Component
public class EntityFieldRegistry {

    private static final Map<MigrationEntityType, List<EntityFieldDefinition>> REGISTRY;

    static {
        Map<MigrationEntityType, List<EntityFieldDefinition>> map = new EnumMap<>(MigrationEntityType.class);

        List<EntityFieldDefinition> personPiiFields = List.of(
                new EntityFieldDefinition("firstName", "String", true, "Person's first name (encrypted)"),
                new EntityFieldDefinition("lastName", "String", true, "Person's last name (encrypted)"),
                new EntityFieldDefinition("email", "String", true, "Email address (encrypted)"),
                new EntityFieldDefinition("phoneNumber", "String", true, "Phone number (encrypted)"),
                new EntityFieldDefinition("address", "String", true, "Physical address (encrypted)"),
                new EntityFieldDefinition("zipCode", "String", true, "ZIP/postal code (encrypted)")
        );

        List<EntityFieldDefinition> commonUserFields = List.of(
                new EntityFieldDefinition("birthDate", "LocalDate", true, "Date of birth (YYYY-MM-DD)"),
                new EntityFieldDefinition("entryDate", "LocalDate", true, "Date of enrollment/entry (YYYY-MM-DD)")
        );

        map.put(MigrationEntityType.EMPLOYEE, combine(personPiiFields, commonUserFields, List.of(
                new EntityFieldDefinition("employeeType", "String", true,
                        "Role: MANAGER, INSTRUCTOR, ADMIN, CLERK, etc.")
        )));

        map.put(MigrationEntityType.COLLABORATOR, combine(personPiiFields, commonUserFields, List.of(
                new EntityFieldDefinition("skills", "String", true, "Comma-separated skills list")
        )));

        map.put(MigrationEntityType.ADULT_STUDENT, combine(personPiiFields, commonUserFields, List.of()));

        map.put(MigrationEntityType.TUTOR, combine(personPiiFields, commonUserFields, List.of()));

        map.put(MigrationEntityType.MINOR_STUDENT, combine(personPiiFields, commonUserFields, List.of(
                new EntityFieldDefinition("tutorId", "Long", true,
                        "ID of the tutor (parent/guardian) — must exist in tenant")
        )));

        map.put(MigrationEntityType.COURSE, List.of(
                new EntityFieldDefinition("courseName", "String", true, "Course display name"),
                new EntityFieldDefinition("courseDescription", "String", true, "Course description"),
                new EntityFieldDefinition("maxCapacity", "Integer", true, "Maximum enrollment capacity")
        ));

        map.put(MigrationEntityType.SCHEDULE, List.of(
                new EntityFieldDefinition("scheduleDay", "String", true,
                        "Day of week: MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY"),
                new EntityFieldDefinition("startTime", "LocalTime", true, "Session start time (HH:mm)"),
                new EntityFieldDefinition("endTime", "LocalTime", true, "Session end time (HH:mm)"),
                new EntityFieldDefinition("courseId", "Long", false, "FK to course — must exist in tenant")
        ));

        map.put(MigrationEntityType.MEMBERSHIP, List.of(
                new EntityFieldDefinition("membershipType", "String", true, "Membership plan type/level"),
                new EntityFieldDefinition("fee", "BigDecimal", true, "Membership cost (decimal with 2 places)"),
                new EntityFieldDefinition("description", "String", true, "Membership benefits description")
        ));

        map.put(MigrationEntityType.ENROLLMENT, List.of(
                new EntityFieldDefinition("startDate", "LocalDate", true, "Enrollment start date (YYYY-MM-DD)"),
                new EntityFieldDefinition("dueDate", "LocalDate", true, "Enrollment expiration date (YYYY-MM-DD)"),
                new EntityFieldDefinition("membershipId", "Long", false, "FK to membership — must exist in tenant"),
                new EntityFieldDefinition("courseId", "Long", false, "FK to course — must exist in tenant"),
                new EntityFieldDefinition("studentId", "Long", true,
                        "FK to adult student or tutor — must exist in tenant")
        ));

        map.put(MigrationEntityType.STORE_PRODUCT, List.of(
                new EntityFieldDefinition("name", "String", true, "Product display name"),
                new EntityFieldDefinition("description", "String", false, "Product description"),
                new EntityFieldDefinition("price", "BigDecimal", true, "Selling price (decimal with 2 places)"),
                new EntityFieldDefinition("stockQuantity", "Integer", true, "Available inventory quantity")
        ));

        REGISTRY = Collections.unmodifiableMap(map);
    }

    /**
     * Returns field definitions for the given entity type.
     *
     * @param entityType the migration entity type
     * @return list of field definitions, never null
     */
    public List<EntityFieldDefinition> getFields(MigrationEntityType entityType) {
        return REGISTRY.getOrDefault(entityType, List.of());
    }

    /**
     * Returns field definitions for all supported entity types.
     *
     * @return unmodifiable map of entity type to field definitions
     */
    public Map<MigrationEntityType, List<EntityFieldDefinition>> getAllFields() {
        return REGISTRY;
    }

    /**
     * Returns the required field names for a given entity type.
     *
     * @param entityType the migration entity type
     * @return list of required field names
     */
    public List<String> getRequiredFieldNames(MigrationEntityType entityType) {
        return getFields(entityType).stream()
                .filter(EntityFieldDefinition::required)
                .map(EntityFieldDefinition::name)
                .toList();
    }

    @SafeVarargs
    private static List<EntityFieldDefinition> combine(List<EntityFieldDefinition>... lists) {
        return java.util.stream.Stream.of(lists)
                .flatMap(List::stream)
                .toList();
    }
}
