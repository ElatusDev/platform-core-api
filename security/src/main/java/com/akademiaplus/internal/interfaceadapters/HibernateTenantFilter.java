package com.akademiaplus.internal.interfaceadapters;

import jakarta.annotation.Nonnull;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Filter;
import org.hibernate.Session;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.orm.jpa.EntityManagerHolder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Objects;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@RequiredArgsConstructor
@Slf4j
public class HibernateTenantFilter extends OncePerRequestFilter {

    private static final String TENANT_HEADER = "X-Tenant-Id";
    private static final String TENANT_FILTER_NAME = "tenantFilter";

    private final EntityManagerFactory entityManagerFactory;
    private final TenantContextHolder tenantContextHolder;

    @Override
    protected void doFilterInternal(@Nonnull HttpServletRequest request,
                                    @Nonnull HttpServletResponse response,
                                    @Nonnull FilterChain filterChain)
            throws ServletException, IOException {

        Integer tenantId = null;
        Session hibernateSession = null;

        try {
            tenantId = extractTenantId(request);

            if (tenantId == null) {
                log.error("No tenant ID found for request: {} {}",
                        request.getMethod(), request.getRequestURI());
                response.sendError(HttpServletResponse.SC_BAD_REQUEST,
                        "X-Tenant-Id header is required");
                return;
            }

            tenantContextHolder.setTenantId(tenantId);
            log.debug("✅ Set tenant {} in context for: {} {}",
                    tenantId, request.getMethod(), request.getRequestURI());

            hibernateSession = getCurrentSession();
            if (hibernateSession != null) {
                Filter filter = hibernateSession.enableFilter(TENANT_FILTER_NAME);
                filter.setParameter("tenantId", tenantId);
                log.debug("✅ Enabled Hibernate filter for tenant {}", tenantId);
            } else {
                log.debug("No active Hibernate session yet (will be created when needed)");
            }

            filterChain.doFilter(request, response);
        } catch (NumberFormatException e) {
            log.error("Invalid tenant ID format in header", e);
            response.sendError(HttpServletResponse.SC_BAD_REQUEST,
                    "Invalid X-Tenant-Id format");
        }
    }

    private Integer extractTenantId(HttpServletRequest request) {
        String tenantHeader = request.getHeader(TENANT_HEADER);
        if (tenantHeader != null && !tenantHeader.trim().isEmpty()) {
            try {
                return Integer.valueOf(tenantHeader.trim());
            } catch (NumberFormatException e) {
                log.error("Invalid tenant ID format: {}", tenantHeader);
                return null;
            }
        }
        return null;
    }

    private Session getCurrentSession() {
        try {
            if (TransactionSynchronizationManager.hasResource(entityManagerFactory)) {
                EntityManagerHolder holder = (EntityManagerHolder)
                        TransactionSynchronizationManager.getResource(entityManagerFactory);
                EntityManager em = Objects.requireNonNull(holder).getEntityManager();
                if (em.isOpen()) {
                    return em.unwrap(Session.class);
                }
            }
        } catch (Exception e) {
            log.debug("Could not get current session: {}", e.getMessage());
        }
        return null;
    }
}