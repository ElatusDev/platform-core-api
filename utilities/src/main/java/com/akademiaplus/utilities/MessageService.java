/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.utilities;

import com.akademiaplus.utilities.config.BeanConfig;
import jakarta.annotation.PostConstruct;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;

import java.util.Locale;

/**
 * Centralized i18n message resolution service.
 * <p>
 * Resolves localized messages from {@code .properties} files using Spring's
 * {@link MessageSource}. Generic methods accept entity type keys from
 * {@link EntityType} and resolve them to display names before formatting.
 *
 * @author ElatusDev
 * @since 1.0
 */
@Service
public class MessageService {

    // ── Generic message keys (used by BaseControllerAdvice) ──────────────
    /** Message key for entity not found errors. */
    public static final String KEY_ENTITY_NOT_FOUND = "entity.not.found";

    /** Message key for entity deletion not allowed (DB constraint). */
    public static final String KEY_ENTITY_DELETE_NOT_ALLOWED = "entity.delete.not.allowed";

    /** Message key for entity deletion not allowed with business reason. */
    public static final String KEY_ENTITY_DELETE_NOT_ALLOWED_REASON = "entity.delete.not.allowed.reason";

    /** Message key for duplicate entity field violation. */
    public static final String KEY_ENTITY_DUPLICATE_FIELD = "entity.duplicate.field";

    /** Message key for generic constraint violation. */
    public static final String KEY_ENTITY_CONSTRAINT_VIOLATION = "entity.constraint.violation";

    /** Message key for invalid tenant context. */
    public static final String KEY_INVALID_TENANT = "invalid.tenant";

    // ── Legacy per-entity message keys ───────────────────────────────────
    private static final String ADULT_STUDENT_ENTITY = "entity.adult.student";
    private static final String EMPLOYEE_ENTITY = "entity.employee";
    private static final String COLLABORATOR_ENTITY = "entity.collaborator";
    private static final String TUTOR_ENTITY = "entity.tutor";
    private static final String MINOR_STUDENT_ENTITY = "entity.minor.student";
    private static final String COURSE_ENTITY = "entity.course";
    private static final String COURSE_EVENT_ENTITY = "entity.course.event";
    private static final String MEMBERSHIP_ENTITY = "entity.membership";
    private static final String MEMBERSHIP_ADULT_STUDENT_ENTITY = "entity.membership.adult.student";
    private static final String MEMBERSHIP_TUTOR_ENTITY = "entity.membership.tutor";
    private static final String PAYMENT_ADULT_STUDENT_ENTITY = "entity.payment.adult.student";
    private static final String PAYMENT_TUTOR_ENTITY = "entity.payment.tutor";
    private static final String COMPENSATION_ENTITY = "entity.compensation";
    private static final String NOTIFICATION_ENTITY = "entity.notification";
    private static final String STORE_PRODUCT_ENTITY = "entity.store.product";
    private static final String STORE_TRANSACTION_ENTITY = "entity.store.transaction";

    private static final String INVALID_DATA_EMAIL_CREATION_REQUEST = "invalid.data.email.creation.request";
    private static final String INVALID_DATA_PHONE_CREATION_REQUEST = "invalid.data.phone.creation.request";
    private static final String INVALID_UNKNOWN_DATA_REQUEST = "invalid.unknown.data.request";

    //      security
    private static final String INVALID_LOGIN = "invalid.login";

    //      coordination
    private static final String SCHEDULE_NOT_AVAILABLE = "schedule.not.available";
    private static final String SCHEDULE_NOT_FOUND = "schedule.not.found";
    private static final String COURSE_COLLABORATOR_NOT_FOUND = "course.collaborator.not.assignable";

    // Internal server error
    private static final String INTERNAL_ERROR_HIGH_SEVERITY = "internal.error.high.severity";

    private final MessageSource messageSource;
    private final Locale locale;

    // Cached entity display names (initialized in @PostConstruct)
    private String adultStudent;
    private String collaborator;
    private String employee;
    private String tutor;
    private String minorStudent;
    private String course;
    private String courseEvent;
    private String membership;
    private String membershipAdultStudent;
    private String membershipTutor;
    private String paymentAdultStudent;
    private String paymentTutor;
    private String compensation;
    private String notification;
    private String storeProduct;
    private String storeTransaction;

    /**
     * Creates a new MessageService.
     *
     * @param messageSource Spring message source for i18n resolution
     */
    public MessageService(MessageSource messageSource) {
        this.messageSource = messageSource;
        this.locale = Locale.forLanguageTag(BeanConfig.LOCALE_LANGUAGE);
    }

    /** Initializes cached entity display name strings. */
    @PostConstruct
    public void init() {
        adultStudent = messageSource.getMessage(ADULT_STUDENT_ENTITY, null, locale);
        collaborator = messageSource.getMessage(COLLABORATOR_ENTITY, null, locale);
        employee = messageSource.getMessage(EMPLOYEE_ENTITY, null, locale);
        tutor = messageSource.getMessage(TUTOR_ENTITY, null, locale);
        minorStudent = messageSource.getMessage(MINOR_STUDENT_ENTITY, null, locale);
        course = messageSource.getMessage(COURSE_ENTITY, null, locale);
        courseEvent = messageSource.getMessage(COURSE_EVENT_ENTITY, null, locale);
        membership = messageSource.getMessage(MEMBERSHIP_ENTITY, null, locale);
        membershipAdultStudent = messageSource.getMessage(MEMBERSHIP_ADULT_STUDENT_ENTITY, null, locale);
        membershipTutor = messageSource.getMessage(MEMBERSHIP_TUTOR_ENTITY, null, locale);
        paymentAdultStudent = messageSource.getMessage(PAYMENT_ADULT_STUDENT_ENTITY, null, locale);
        paymentTutor = messageSource.getMessage(PAYMENT_TUTOR_ENTITY, null, locale);
        compensation = messageSource.getMessage(COMPENSATION_ENTITY, null, locale);
        notification = messageSource.getMessage(NOTIFICATION_ENTITY, null, locale);
        storeProduct = messageSource.getMessage(STORE_PRODUCT_ENTITY, null, locale);
        storeTransaction = messageSource.getMessage(STORE_TRANSACTION_ENTITY, null, locale);
    }

    // ══════════════════════════════════════════════════════════════════════
    // Generic methods — used by BaseControllerAdvice
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Resolves a localized "entity not found" message.
     * <p>
     * First resolves the entity type key (e.g., "entity.employee" → "Empleado"),
     * then formats into the "entity.not.found" template.
     *
     * @param entityTypeKey message key from {@link EntityType}
     * @param entityId      the ID that was not found
     * @return formatted message, e.g., "Empleado con ID: 42 no existe!"
     */
    public String getEntityNotFound(String entityTypeKey, String entityId) {
        String entityName = messageSource.getMessage(entityTypeKey, null, locale);
        return messageSource.getMessage(KEY_ENTITY_NOT_FOUND,
                new Object[]{entityName, entityId}, locale);
    }

    /**
     * Resolves a localized "deletion not allowed" message for DB constraint violations.
     *
     * @param entityTypeKey message key from {@link EntityType}
     * @return formatted message, e.g., "Eliminacion de Empleado no esta permitida!..."
     */
    public String getEntityDeleteNotAllowed(String entityTypeKey) {
        String entityName = messageSource.getMessage(entityTypeKey, null, locale);
        return messageSource.getMessage(KEY_ENTITY_DELETE_NOT_ALLOWED,
                new Object[]{entityName}, locale);
    }

    /**
     * Resolves a localized "deletion not allowed" message for business rule violations.
     *
     * @param entityTypeKey message key from {@link EntityType}
     * @param entityId      the entity ID whose deletion was attempted
     * @param reason        human-readable business rule description
     * @return formatted message
     */
    public String getEntityDeleteNotAllowedWithReason(
            String entityTypeKey, String entityId, String reason) {
        String entityName = messageSource.getMessage(entityTypeKey, null, locale);
        return messageSource.getMessage(KEY_ENTITY_DELETE_NOT_ALLOWED_REASON,
                new Object[]{entityName, entityId, reason}, locale);
    }

    /**
     * Resolves a localized "duplicate field" message.
     *
     * @param entityTypeKey message key from {@link EntityType}
     * @param field         the field name that has a duplicate value
     * @return formatted message
     */
    public String getEntityDuplicateField(String entityTypeKey, String field) {
        String entityName = messageSource.getMessage(entityTypeKey, null, locale);
        return messageSource.getMessage(KEY_ENTITY_DUPLICATE_FIELD,
                new Object[]{entityName, field}, locale);
    }

    /**
     * Resolves the generic constraint violation message.
     *
     * @return "Operacion no permitida: conflicto de integridad de datos"
     */
    public String getConstraintViolation() {
        return messageSource.getMessage(KEY_ENTITY_CONSTRAINT_VIOLATION, null, locale);
    }

    /**
     * Resolves the invalid tenant context message.
     *
     * @return "Contexto de organizacion es requerido"
     */
    public String getInvalidTenant() {
        return messageSource.getMessage(KEY_INVALID_TENANT, null, locale);
    }

    // ══════════════════════════════════════════════════════════════════════
    // Deprecated per-entity NotFound methods
    // ══════════════════════════════════════════════════════════════════════

    /** @deprecated Use {@link #getEntityNotFound(String, String)} with {@link EntityType#ADULT_STUDENT} */
    @Deprecated(since = "1.1", forRemoval = true)
    public String getAdultStudentNotFound(String id) {
        return resolveNotFoundByName(adultStudent, id);
    }

    /** @deprecated Use {@link #getEntityNotFound(String, String)} with {@link EntityType#COLLABORATOR} */
    @Deprecated(since = "1.1", forRemoval = true)
    public String getCollaboratorNotFound(String id) {
        return resolveNotFoundByName(collaborator, id);
    }

    /** @deprecated Use {@link #getEntityNotFound(String, String)} with {@link EntityType#EMPLOYEE} */
    @Deprecated(since = "1.1", forRemoval = true)
    public String getEmployeeNotFound(String id) {
        return resolveNotFoundByName(employee, id);
    }

    /** @deprecated Use {@link #getEntityNotFound(String, String)} with {@link EntityType#TUTOR} */
    @Deprecated(since = "1.1", forRemoval = true)
    public String getTutorNotFound(String id) {
        return resolveNotFoundByName(tutor, id);
    }

    /** @deprecated Use {@link #getEntityNotFound(String, String)} with {@link EntityType#MINOR_STUDENT} */
    @Deprecated(since = "1.1", forRemoval = true)
    public String getMinorStudentNotFound(String id) {
        return resolveNotFoundByName(minorStudent, id);
    }

    /** @deprecated Use {@link #getEntityNotFound(String, String)} with {@link EntityType#COURSE} */
    @Deprecated(since = "1.1", forRemoval = true)
    public String getCourseNotFound(String id) {
        return resolveNotFoundByName(course, id);
    }

    /** @deprecated Use {@link #getEntityNotFound(String, String)} with {@link EntityType#COURSE_EVENT} */
    @Deprecated(since = "1.1", forRemoval = true)
    public String getCourseEventNotFound(String id) {
        return resolveNotFoundByName(courseEvent, id);
    }

    /** @deprecated Use {@link #getEntityNotFound(String, String)} with {@link EntityType#MEMBERSHIP} */
    @Deprecated(since = "1.1", forRemoval = true)
    public String getMembershipNotFound(String id) {
        return resolveNotFoundByName(membership, id);
    }

    /** @deprecated Use {@link #getEntityNotFound(String, String)} with {@link EntityType#MEMBERSHIP_ADULT_STUDENT} */
    @Deprecated(since = "1.1", forRemoval = true)
    public String getMembershipAdultStudentNotFound(String id) {
        return resolveNotFoundByName(membershipAdultStudent, id);
    }

    /** @deprecated Use {@link #getEntityNotFound(String, String)} with {@link EntityType#MEMBERSHIP_TUTOR} */
    @Deprecated(since = "1.1", forRemoval = true)
    public String getMembershipTutorNotFound(String id) {
        return resolveNotFoundByName(membershipTutor, id);
    }

    /** @deprecated Use {@link #getEntityNotFound(String, String)} with {@link EntityType#PAYMENT_ADULT_STUDENT} */
    @Deprecated(since = "1.1", forRemoval = true)
    public String getPaymentAdultStudentNotFound(String id) {
        return resolveNotFoundByName(paymentAdultStudent, id);
    }

    /** @deprecated Use {@link #getEntityNotFound(String, String)} with {@link EntityType#PAYMENT_TUTOR} */
    @Deprecated(since = "1.1", forRemoval = true)
    public String getPaymentTutorNotFound(String id) {
        return resolveNotFoundByName(paymentTutor, id);
    }

    /** @deprecated Use {@link #getEntityNotFound(String, String)} with {@link EntityType#COMPENSATION} */
    @Deprecated(since = "1.1", forRemoval = true)
    public String getCompensationNotFound(String id) {
        return resolveNotFoundByName(compensation, id);
    }

    /** @deprecated Use {@link #getEntityNotFound(String, String)} with {@link EntityType#NOTIFICATION} */
    @Deprecated(since = "1.1", forRemoval = true)
    public String getNotificationNotFound(String id) {
        return resolveNotFoundByName(notification, id);
    }

    /** @deprecated Use {@link #getEntityNotFound(String, String)} with {@link EntityType#STORE_PRODUCT} */
    @Deprecated(since = "1.1", forRemoval = true)
    public String getStoreProductNotFound(String id) {
        return resolveNotFoundByName(storeProduct, id);
    }

    /** @deprecated Use {@link #getEntityNotFound(String, String)} with {@link EntityType#STORE_TRANSACTION} */
    @Deprecated(since = "1.1", forRemoval = true)
    public String getStoreTransactionNotFound(String id) {
        return resolveNotFoundByName(storeTransaction, id);
    }

    // ══════════════════════════════════════════════════════════════════════
    // Deprecated per-entity DeleteNotAllowed methods
    // ══════════════════════════════════════════════════════════════════════

    /** @deprecated Use {@link #getEntityDeleteNotAllowed(String)} with {@link EntityType#ADULT_STUDENT} */
    @Deprecated(since = "1.1", forRemoval = true)
    public String getAdultStudentDeleteNotAllowed() {
        return resolveDeleteNotAllowedByName(adultStudent);
    }

    /** @deprecated Use {@link #getEntityDeleteNotAllowed(String)} with {@link EntityType#COLLABORATOR} */
    @Deprecated(since = "1.1", forRemoval = true)
    public String getCollaboratorDeleteNotAllowed() {
        return resolveDeleteNotAllowedByName(collaborator);
    }

    /** @deprecated Use {@link #getEntityDeleteNotAllowed(String)} with {@link EntityType#EMPLOYEE} */
    @Deprecated(since = "1.1", forRemoval = true)
    public String getEmployeeDeleteNotAllowed() {
        return resolveDeleteNotAllowedByName(employee);
    }

    // ══════════════════════════════════════════════════════════════════════
    // Non-deprecated domain-specific methods
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Resolves the invalid email creation request message.
     *
     * @return localized error message for duplicate email
     */
    public String getInvalidDataEmailCreationRequest() {
        return messageSource.getMessage(INVALID_DATA_EMAIL_CREATION_REQUEST, null, locale);
    }

    /**
     * Resolves the invalid phone creation request message.
     *
     * @return localized error message for duplicate phone
     */
    public String getInvalidDataPhoneCreationRequest() {
        return messageSource.getMessage(INVALID_DATA_PHONE_CREATION_REQUEST, null, locale);
    }

    /**
     * Resolves the unknown data request message.
     *
     * @return localized error message for unrecognized property
     */
    public String getInvalidUnknownDataRequest() {
        return messageSource.getMessage(INVALID_UNKNOWN_DATA_REQUEST, null, locale);
    }

    /**
     * Resolves the invalid login message.
     *
     * @return localized error message for invalid credentials
     */
    public String getInvalidLogin() {
        return messageSource.getMessage(INVALID_LOGIN, null, locale);
    }

    /**
     * Resolves the schedule not available message.
     *
     * @param conflicting description of the conflicting schedule
     * @return localized error message
     */
    public String getScheduleNotAvailable(String conflicting) {
        return messageSource.getMessage(SCHEDULE_NOT_AVAILABLE, new Object[]{conflicting}, locale);
    }

    /**
     * Resolves the course collaborator not found message.
     *
     * @param notFounded identifier of the collaborator not found
     * @return localized error message
     */
    public String getCourseCollaboratorNotFound(String notFounded) {
        return messageSource.getMessage(COURSE_COLLABORATOR_NOT_FOUND, new Object[]{notFounded}, locale);
    }

    /**
     * Resolves the schedule not found message.
     *
     * @param notFounded identifier of the schedule not found
     * @return localized error message
     */
    public String getScheduleNotFound(String notFounded) {
        return messageSource.getMessage(SCHEDULE_NOT_FOUND, new Object[]{notFounded}, locale);
    }

    /**
     * Resolves the high severity internal error message.
     *
     * @return localized error message for internal server errors
     */
    public String getInternalErrorHighSeverity() {
        return messageSource.getMessage(INTERNAL_ERROR_HIGH_SEVERITY, null, locale);
    }

    // ══════════════════════════════════════════════════════════════════════
    // Private helpers
    // ══════════════════════════════════════════════════════════════════════

    private String resolveNotFoundByName(String entityName, String id) {
        return messageSource.getMessage(KEY_ENTITY_NOT_FOUND,
                new Object[]{entityName, id}, locale);
    }

    private String resolveDeleteNotAllowedByName(String entityName) {
        return messageSource.getMessage(KEY_ENTITY_DELETE_NOT_ALLOWED,
                new Object[]{entityName}, locale);
    }
}
