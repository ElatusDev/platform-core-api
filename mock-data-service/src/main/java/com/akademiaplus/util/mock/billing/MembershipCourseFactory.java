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
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Factory for creating {@link MembershipCourseRecord} instances with unique FK pairs.
 *
 * <p>Requires membership and course IDs to be injected via setters
 * before {@link #generate(int)} is called. Uses deterministic modulo cycling
 * to maximise unique pair coverage.</p>
 */
@Component
public class MembershipCourseFactory implements DataFactory<MembershipCourseRecord> {

    static final String ERROR_MEMBERSHIP_IDS_NOT_SET =
            "availableMembershipIds must be set before generating";
    static final String ERROR_COURSE_IDS_NOT_SET =
            "availableCourseIds must be set before generating";

    private static final String PAIR_SEPARATOR = ":";

    @Setter
    private List<Long> availableMembershipIds = List.of();

    @Setter
    private List<Long> availableCourseIds = List.of();

    /**
     * Generates up to {@code count} unique membership-to-course pairs.
     *
     * @param count the desired number of records
     * @return an unmodifiable list of unique {@link MembershipCourseRecord} instances
     * @throws IllegalStateException if required IDs have not been set
     */
    @Override
    public List<MembershipCourseRecord> generate(int count) {
        if (availableMembershipIds.isEmpty()) {
            throw new IllegalStateException(ERROR_MEMBERSHIP_IDS_NOT_SET);
        }
        if (availableCourseIds.isEmpty()) {
            throw new IllegalStateException(ERROR_COURSE_IDS_NOT_SET);
        }

        Set<String> seen = new HashSet<>();
        List<MembershipCourseRecord> result = new ArrayList<>();
        int attempts = 0;
        int maxAttempts = count * 3;

        while (result.size() < count && attempts < maxAttempts) {
            Long membershipId = availableMembershipIds.get(
                    attempts % availableMembershipIds.size());
            Long courseId = availableCourseIds.get(
                    (attempts / availableMembershipIds.size()) % availableCourseIds.size());
            String key = membershipId + PAIR_SEPARATOR + courseId;
            if (seen.add(key)) {
                result.add(new MembershipCourseRecord(membershipId, courseId));
            }
            attempts++;
        }
        return result;
    }
}
