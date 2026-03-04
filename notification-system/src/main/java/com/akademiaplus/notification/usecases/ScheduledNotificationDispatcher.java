/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.notification.usecases;

import com.akademiaplus.notification.interfaceadapters.NotificationRepository;
import com.akademiaplus.notifications.NotificationDataModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Periodically queries for scheduled notifications and dispatches them.
 * <p>
 * Runs outside HTTP request scope, so it creates a synthetic
 * {@link RequestAttributes} context per tenant to enable
 * {@code TenantContextHolder} and {@code EntityIdAssigner} to function
 * correctly when creating delivery records.
 *
 * @see NotificationDispatchService
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ScheduledNotificationDispatcher {

    /** Default scheduler interval in milliseconds (60 seconds). */
    public static final long DEFAULT_INTERVAL_MS = 60_000L;

    /** Prefix for synthetic session IDs on scheduler threads. */
    public static final String SESSION_ID_PREFIX = "scheduler-";

    private final NotificationRepository notificationRepository;
    private final NotificationDispatchService notificationDispatchService;
    private final ApplicationContext applicationContext;

    /**
     * Queries all pending scheduled notifications across tenants and
     * dispatches each one within the appropriate tenant context.
     * <p>
     * Notifications are grouped by tenant to minimize context switches.
     * Individual dispatch failures are caught and logged to prevent one
     * failed notification from blocking the rest.
     */
    @Scheduled(fixedDelayString = "${akademia.notification.scheduler.interval-ms:60000}")
    public void dispatchScheduledNotifications() {
        final LocalDateTime now = LocalDateTime.now();
        final List<NotificationDataModel> pending = notificationRepository.findScheduledBefore(now);

        if (pending.isEmpty()) {
            return;
        }

        log.info("Found {} scheduled notifications due for dispatch", pending.size());

        final Map<Long, List<NotificationDataModel>> byTenant = pending.stream()
                .collect(Collectors.groupingBy(NotificationDataModel::getTenantId));

        for (Map.Entry<Long, List<NotificationDataModel>> entry : byTenant.entrySet()) {
            executeInTenantContext(entry.getKey(), entry.getValue());
        }
    }

    /**
     * Executes notification dispatches within a synthetic request scope
     * for the given tenant.
     * <p>
     * Creates a minimal {@link RequestAttributes} implementation and
     * sets the tenant ID on the request-scoped {@code TenantContextHolder}
     * bean, enabling {@code EntityIdAssigner} to function correctly.
     *
     * @param tenantId      the tenant to set in context
     * @param notifications the notifications to dispatch
     */
    void executeInTenantContext(Long tenantId, List<NotificationDataModel> notifications) {
        RequestContextHolder.setRequestAttributes(new SchedulerRequestAttributes());
        try {
            applicationContext.getBean(
                    com.akademiaplus.infra.persistence.config.TenantContextHolder.class
            ).setTenantId(tenantId);

            for (NotificationDataModel notification : notifications) {
                dispatchSafely(notification);
            }
        } finally {
            RequestContextHolder.resetRequestAttributes();
        }
    }

    /**
     * Dispatches a single notification, catching and logging any exceptions.
     *
     * @param notification the notification to dispatch
     */
    void dispatchSafely(NotificationDataModel notification) {
        try {
            notificationDispatchService.dispatch(notification);
        } catch (IllegalArgumentException | IllegalStateException e) {
            log.error("Failed to dispatch notification {} for tenant {}: {}",
                    notification.getNotificationId(),
                    notification.getTenantId(),
                    e.getMessage(), e);
        }
    }

    /**
     * Minimal {@link RequestAttributes} implementation for scheduler threads.
     * <p>
     * Provides a simple attribute store so that request-scoped beans
     * (like {@code TenantContextHolder}) can be created and used outside
     * of an actual HTTP request context.
     */
    static class SchedulerRequestAttributes implements RequestAttributes {

        private final Map<String, Object> attributes = new ConcurrentHashMap<>();

        @Override
        public Object getAttribute(String name, int scope) {
            return attributes.get(name);
        }

        @Override
        public void setAttribute(String name, Object value, int scope) {
            attributes.put(name, value);
        }

        @Override
        public void removeAttribute(String name, int scope) {
            attributes.remove(name);
        }

        @Override
        public String[] getAttributeNames(int scope) {
            return attributes.keySet().toArray(new String[0]);
        }

        @Override
        public void registerDestructionCallback(String name, Runnable callback, int scope) {
            // No-op for scheduler context — no destruction lifecycle
        }

        @Override
        public Object resolveReference(String key) {
            return null;
        }

        @Override
        public String getSessionId() {
            return SESSION_ID_PREFIX + Thread.currentThread().getName();
        }

        @Override
        public Object getSessionMutex() {
            return this;
        }
    }
}
