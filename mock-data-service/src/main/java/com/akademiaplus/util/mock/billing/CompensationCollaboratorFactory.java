/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.util.mock.billing;

import com.akademiaplus.util.base.DataFactory;
import lombok.Setter;
import net.datafaker.Faker;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Factory for creating {@link CompensationCollaboratorRecord} instances with unique FK pairs.
 *
 * <p>Requires compensation and collaborator IDs to be injected via setters
 * before {@link #generate(int)} is called. Uses deterministic modulo cycling
 * to maximise unique pair coverage and generates a random {@code assignedDate}
 * within the last 90 days.</p>
 */
@Component
@SuppressWarnings("java:S2245")
public class CompensationCollaboratorFactory implements DataFactory<CompensationCollaboratorRecord> {

    static final String ERROR_COMPENSATION_IDS_NOT_SET =
            "availableCompensationIds must be set before generating";
    static final String ERROR_COLLABORATOR_IDS_NOT_SET =
            "availableCollaboratorIds must be set before generating";

    private static final String PAIR_SEPARATOR = ":";
    private static final int MAX_DAYS_BACK = 90;

    private final Faker faker;

    @Setter
    private List<Long> availableCompensationIds = List.of();

    @Setter
    private List<Long> availableCollaboratorIds = List.of();

    /**
     * Creates a new factory with a Mexican-Spanish locale Faker instance.
     */
    public CompensationCollaboratorFactory() {
        this.faker = new Faker(Locale.of("es", "MX"));
    }

    /**
     * Generates up to {@code count} unique compensation-collaborator pairs with assigned dates.
     *
     * @param count the desired number of records
     * @return an unmodifiable list of unique {@link CompensationCollaboratorRecord} instances
     * @throws IllegalStateException if required IDs have not been set
     */
    @Override
    public List<CompensationCollaboratorRecord> generate(int count) {
        if (availableCompensationIds.isEmpty()) {
            throw new IllegalStateException(ERROR_COMPENSATION_IDS_NOT_SET);
        }
        if (availableCollaboratorIds.isEmpty()) {
            throw new IllegalStateException(ERROR_COLLABORATOR_IDS_NOT_SET);
        }

        Set<String> seen = new HashSet<>();
        List<CompensationCollaboratorRecord> result = new ArrayList<>();
        int attempts = 0;
        int maxAttempts = count * 3;

        while (result.size() < count && attempts < maxAttempts) {
            Long compensationId = availableCompensationIds.get(
                    attempts % availableCompensationIds.size());
            Long collaboratorId = availableCollaboratorIds.get(
                    (attempts / availableCompensationIds.size()) % availableCollaboratorIds.size());
            String key = compensationId + PAIR_SEPARATOR + collaboratorId;
            if (seen.add(key)) {
                LocalDate assignedDate = LocalDate.now()
                        .minusDays(faker.number().numberBetween(0, MAX_DAYS_BACK));
                result.add(new CompensationCollaboratorRecord(
                        compensationId, collaboratorId, assignedDate));
            }
            attempts++;
        }
        return result;
    }
}
