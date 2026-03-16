/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.config;

import lombok.Getter;

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;

/**
 * Declares every entity in the mock data dependency graph.
 *
 * <p>Each value carries three properties:</p>
 * <ul>
 *   <li>{@code loadable} — {@code true} if a {@code DataLoader} and
 *       {@code AbstractMockDataUseCase} exist for this entity.</li>
 *   <li>{@code cleanable} — {@code true} if a {@code DataCleanUp} exists
 *       (currently all values are cleanable).</li>
 *   <li>{@code dependencies} — the direct FK parents in the graph;
 *       cleanup must delete a child <em>before</em> its parents.</li>
 * </ul>
 *
 * <p>People entities list auth/PII as parents (not TENANT directly)
 * so cleanup deletes people → auth/PII → TENANT in the correct FK order.
 * TENANT is reached transitively through auth/PII.</p>
 */
@Getter
public enum MockEntityType {

    // ── Level 0: root entity ──
    TENANT(true, true),

    // ── Level 0: cleanup-only shared tables (FK → TENANT) ──
    TENANT_SEQUENCE(false, true, TENANT),
    PERSON_PII(false, true, TENANT),
    INTERNAL_AUTH(false, true, TENANT),
    CUSTOMER_AUTH(false, true, TENANT),

    // ── Level 1: tenant-scoped standalone entities ──
    TENANT_SUBSCRIPTION(true, true, TENANT),
    TENANT_BILLING_CYCLE(true, true, TENANT),
    COMPENSATION(true, true, TENANT),
    MEMBERSHIP(true, true, TENANT),
    STORE_PRODUCT(true, true, TENANT),
    NOTIFICATION(true, true, TENANT),
    COURSE(true, true, TENANT),

    // ── Level 1: people with internal auth (FK → INTERNAL_AUTH, PERSON_PII) ──
    EMPLOYEE(true, true, INTERNAL_AUTH, PERSON_PII),
    COLLABORATOR(true, true, INTERNAL_AUTH, PERSON_PII),

    // ── Level 1: people with customer auth (FK → CUSTOMER_AUTH, PERSON_PII) ──
    ADULT_STUDENT(true, true, CUSTOMER_AUTH, PERSON_PII),
    TUTOR(true, true, CUSTOMER_AUTH, PERSON_PII),

    // ── Level 2: depends on TUTOR ──
    MINOR_STUDENT(true, true, TUTOR),

    // ── Level 2: course-dependent entities ──
    SCHEDULE(true, true, COURSE),
    STORE_TRANSACTION(true, true, TENANT),

    // ── Level 3: membership associations (FK → MEMBERSHIP, COURSE, user) ──
    MEMBERSHIP_ADULT_STUDENT(true, true, MEMBERSHIP, COURSE, ADULT_STUDENT),
    MEMBERSHIP_TUTOR(true, true, MEMBERSHIP, COURSE, TUTOR),

    // ── Level 3: course event (FK → SCHEDULE, COURSE, COLLABORATOR) ──
    COURSE_EVENT(true, true, SCHEDULE, COURSE, COLLABORATOR),

    // ── Level 4: payments (FK → membership association) ──
    PAYMENT_ADULT_STUDENT(true, true, MEMBERSHIP_ADULT_STUDENT),
    PAYMENT_TUTOR(true, true, MEMBERSHIP_TUTOR),

    // ── Level 5: card payment info (FK → payment) ──
    CARD_PAYMENT_INFO(true, true, PAYMENT_ADULT_STUDENT),

    // ── Level 3: store sale items (FK → transaction + product) ──
    STORE_SALE_ITEM(true, true, STORE_TRANSACTION, STORE_PRODUCT),

    // ── Level 2: notification delivery (FK → notification) ──
    NOTIFICATION_DELIVERY(true, true, NOTIFICATION),

    // ── Level 1: email (FK → TENANT) ──
    EMAIL(true, true, TENANT),

    // ── Level 2: email children (FK → EMAIL) ──
    EMAIL_RECIPIENT(true, true, EMAIL),
    EMAIL_ATTACHMENT(true, true, EMAIL),

    // ── Level 0: platform-level entities (no tenant scoping) ──
    DEMO_REQUEST(true, true),
    PUSH_DEVICE(true, true, ADULT_STUDENT),

    // ── Level 4: attendance (FK → COURSE_EVENT) ──
    ATTENDANCE_SESSION(true, true, COURSE_EVENT),

    // ── Level 4: notification read status (FK → NOTIFICATION, ADULT_STUDENT for userIds) ──
    NOTIFICATION_READ_STATUS(true, true, NOTIFICATION, ADULT_STUDENT),

    // ── Level 5: attendance record (FK → ATTENDANCE_SESSION) ──
    ATTENDANCE_RECORD(true, true, ATTENDANCE_SESSION),

    // ── Bridge tables: course-people associations ──
    COURSE_AVAILABLE_COLLABORATOR(true, true, COURSE, COLLABORATOR),
    ADULT_STUDENT_COURSE(true, true, ADULT_STUDENT, COURSE),
    MINOR_STUDENT_COURSE(true, true, MINOR_STUDENT, COURSE),
    COURSE_EVENT_ADULT_STUDENT_ATTENDEE(true, true, COURSE_EVENT, ADULT_STUDENT),
    COURSE_EVENT_MINOR_STUDENT_ATTENDEE(true, true, COURSE_EVENT, MINOR_STUDENT),

    // ── Bridge tables: billing-people associations ──
    MEMBERSHIP_COURSE(true, true, MEMBERSHIP, COURSE),
    COMPENSATION_COLLABORATOR(true, true, COMPENSATION, COLLABORATOR),

    // ── Entity tables: tenant config + content ──
    TENANT_BRANDING(true, true, TENANT),
    NEWS_FEED_ITEM(true, true, EMPLOYEE),
    TASK(true, true, EMPLOYEE),

    // ── Config tables: email templates ──
    EMAIL_TEMPLATE(true, true, TENANT),
    EMAIL_TEMPLATE_VARIABLE(true, true, EMAIL_TEMPLATE);

    private final boolean loadable;
    private final boolean cleanable;
    /**
     * -- GETTER --
     *  Returns the direct FK parents of this entity in the dependency graph.
     */
    private final Set<MockEntityType> dependencies;

    MockEntityType(boolean loadable, boolean cleanable, MockEntityType... dependencies) {
        this.loadable = loadable;
        this.cleanable = cleanable;
        this.dependencies = dependencies.length == 0
                ? Collections.emptySet()
                : Collections.unmodifiableSet(new java.util.LinkedHashSet<>(Arrays.asList(dependencies)));
    }

}
