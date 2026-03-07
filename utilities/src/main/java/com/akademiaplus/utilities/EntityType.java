/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.utilities;

/**
 * Canonical entity type identifiers used for exception messages.
 * <p>
 * Values correspond to message property keys in
 * {@code utilities_messages_es_MX.properties} that resolve
 * to localized display names (e.g., "entity.employee" → "Empleado").
 *
 * @author ElatusDev
 * @since 1.0
 */
public final class EntityType {

    private EntityType() {}

    // user-management
    public static final String EMPLOYEE = "entity.employee";
    public static final String COLLABORATOR = "entity.collaborator";
    public static final String ADULT_STUDENT = "entity.adult.student";
    public static final String TUTOR = "entity.tutor";
    public static final String MINOR_STUDENT = "entity.minor.student";

    // billing
    public static final String MEMBERSHIP = "entity.membership";
    public static final String MEMBERSHIP_ADULT_STUDENT = "entity.membership.adult.student";
    public static final String MEMBERSHIP_TUTOR = "entity.membership.tutor";
    public static final String PAYMENT_ADULT_STUDENT = "entity.payment.adult.student";
    public static final String PAYMENT_TUTOR = "entity.payment.tutor";
    public static final String COMPENSATION = "entity.compensation";

    // course-management
    public static final String COURSE = "entity.course";
    public static final String SCHEDULE = "entity.schedule";
    public static final String COURSE_EVENT = "entity.course.event";

    // pos-system
    public static final String STORE_PRODUCT = "entity.store.product";
    public static final String STORE_TRANSACTION = "entity.store.transaction";
    public static final String STORE_SALE_ITEM = "entity.store.sale.item";

    // notification
    public static final String NOTIFICATION = "entity.notification";
    public static final String NOTIFICATION_DELIVERY = "entity.notification.delivery";
    public static final String EMAIL = "entity.email";
    public static final String EMAIL_TEMPLATE = "entity.email.template";

    // lead-management
    public static final String DEMO_REQUEST = "entity.demo.request";

    // tenant
    public static final String TENANT = "entity.tenant";
    public static final String TENANT_SUBSCRIPTION = "entity.tenant.subscription";
    public static final String TENANT_BILLING_CYCLE = "entity.tenant.billing.cycle";
}
