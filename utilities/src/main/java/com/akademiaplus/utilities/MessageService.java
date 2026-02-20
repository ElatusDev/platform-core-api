/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.utilities;

import com.akademiaplus.utilities.config.BeanConfig;
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

    // ── Domain-specific message keys ────────────────────────────────────
    private static final String INVALID_DATA_EMAIL_CREATION_REQUEST = "invalid.data.email.creation.request";
    private static final String INVALID_DATA_PHONE_CREATION_REQUEST = "invalid.data.phone.creation.request";
    private static final String INVALID_UNKNOWN_DATA_REQUEST = "invalid.unknown.data.request";
    private static final String INVALID_LOGIN = "invalid.login";
    private static final String SCHEDULE_NOT_AVAILABLE = "schedule.not.available";
    private static final String INTERNAL_ERROR_HIGH_SEVERITY = "internal.error.high.severity";

    private final MessageSource messageSource;
    private final Locale locale;

    /**
     * Creates a new MessageService.
     *
     * @param messageSource Spring message source for i18n resolution
     */
    public MessageService(MessageSource messageSource) {
        this.messageSource = messageSource;
        this.locale = Locale.forLanguageTag(BeanConfig.LOCALE_LANGUAGE);
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
    // Domain-specific methods
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
     * Resolves the high severity internal error message.
     *
     * @return localized error message for internal server errors
     */
    public String getInternalErrorHighSeverity() {
        return messageSource.getMessage(INTERNAL_ERROR_HIGH_SEVERITY, null, locale);
    }
}
