package com.akademiaplus.internal.interfaceadapters;

/*
 * Copyright a (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */

import com.akademiaplus.infra.TenantContextHolder;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.hibernate.Filter;
import org.hibernate.Session;
import org.springframework.core.annotation.Order;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityManager;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.io.IOException;

@Component
@Order(2)
public class HibernateTenantFilter extends OncePerRequestFilter {

    private static final String TENANT_FILTER_NAME = "tenantFilter";
    private final TenantContextHolder tenantContextHolder;
    private final EntityManagerFactory entityManagerFactory;

    public HibernateTenantFilter(TenantContextHolder tenantContextHolder, EntityManagerFactory entityManagerFactory) {
        this.tenantContextHolder = tenantContextHolder;
        this.entityManagerFactory = entityManagerFactory;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        // Get the current EntityManager for the transaction
        EntityManager entityManager = getEntityManager();

        if (entityManager != null) {
            Session session = entityManager.unwrap(Session.class);
            if (session != null) {
                // Get the tenant ID from our request-scoped holder
                Integer tenantId = tenantContextHolder.getTenantId();

                try {
                    // Activate the Hibernate filter and set the parameter
                    Filter filter = session.enableFilter(TENANT_FILTER_NAME);
                    filter.setParameter("tenantId", tenantId);
                    filterChain.doFilter(request, response);
                } finally {
                    // Very important: always disable the filter when done
                    session.disableFilter(TENANT_FILTER_NAME);
                }
            } else {
                filterChain.doFilter(request, response);
            }
        } else {
            // No EntityManager available for the current request
            filterChain.doFilter(request, response);
        }
    }

    private EntityManager getEntityManager() {
        if (TransactionSynchronizationManager.hasResource(entityManagerFactory)) {
            return (EntityManager) TransactionSynchronizationManager.getResource(entityManagerFactory);
        }
        return null;
    }
}