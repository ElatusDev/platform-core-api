package com.akademiaplus.infra.persistence.config;

import jakarta.annotation.Nonnull;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Filter;
import org.hibernate.Session;
import org.springframework.core.annotation.Order;
import org.springframework.orm.jpa.EntityManagerHolder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Objects;

/**
 * Servlet filter that enables Hibernate tenant filtering for multi-tenant data isolation.
 * <p>
 * This filter is executed for each HTTP request and enables Hibernate's tenant filter
 * with the tenant ID from the current security context. The filter ensures that all
 * database queries within the request are automatically scoped to the current tenant.
 * <p>
 * Order is set to 2 to ensure it runs after tenant context establishment but before
 * the main application logic.
 */
@Slf4j
@Component
@Order(HibernateTenantFilter.FILTER_ORDER)
public class HibernateTenantFilter extends OncePerRequestFilter {

    // Filter configuration
    private static final String TENANT_FILTER_NAME = "tenantFilter";
    private static final String TENANT_ID_PARAMETER = "tenantId";
    static final int FILTER_ORDER = 2;

    // Error messages (public for testing)
    public static final String ERROR_INVALID_TENANT_ID_FORMAT = "Invalid X-Tenant-Id format";

    // Log messages (private - internal only)
    private static final String DEBUG_FILTER_ENABLED = "✅ Enabled Hibernate filter for tenant {}";
    private static final String DEBUG_NO_ACTIVE_SESSION = "No active Hibernate session yet (will be created when needed)";
    private static final String ERROR_INVALID_TENANT_FORMAT = "Invalid tenant ID format in header";
    private static final String DEBUG_COULD_NOT_GET_SESSION = "Could not get current session: {}";

    private final EntityManagerFactory entityManagerFactory;
    private final TenantContextHolder tenantContextHolder;

    /**
     * Constructor for dependency injection.
     *
     * @param entityManagerFactory The JPA entity manager factory
     * @param tenantContextHolder The holder for current tenant context
     */
    public HibernateTenantFilter(EntityManagerFactory entityManagerFactory,
                                 TenantContextHolder tenantContextHolder) {
        this.entityManagerFactory = entityManagerFactory;
        this.tenantContextHolder = tenantContextHolder;
    }

    /**
     * Filters each HTTP request to enable Hibernate tenant filtering.
     * <p>
     * The method performs the following operations:
     * 1. Retrieves the current Hibernate session (if available)
     * 2. Enables the tenant filter with the current tenant ID
     * 3. Proceeds with the filter chain
     * 4. Handles invalid tenant ID format errors
     *
     * @param request The HTTP request
     * @param response The HTTP response
     * @param filterChain The filter chain
     * @throws ServletException if a servlet error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doFilterInternal(@Nonnull HttpServletRequest request,
                                    @Nonnull HttpServletResponse response,
                                    @Nonnull FilterChain filterChain)
            throws ServletException, IOException {
        try {
            Session hibernateSession = getCurrentSession();
            if (hibernateSession != null) {
                tenantContextHolder.getTenantId().ifPresent(tenantId -> {
                    Filter filter = hibernateSession.enableFilter(TENANT_FILTER_NAME);
                    filter.setParameter(TENANT_ID_PARAMETER, tenantId);
                    log.debug(DEBUG_FILTER_ENABLED, tenantId);
                });
            } else {
                log.debug(DEBUG_NO_ACTIVE_SESSION);
            }

            filterChain.doFilter(request, response);
        } catch (NumberFormatException e) {
            log.error(ERROR_INVALID_TENANT_FORMAT, e);
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, ERROR_INVALID_TENANT_ID_FORMAT);
        }
    }

    /**
     * Retrieves the current Hibernate session from the transaction synchronization manager.
     * <p>
     * This method safely attempts to retrieve the session and handles cases where:
     * - No transaction is active yet
     * - Entity manager is closed
     * - Any other session retrieval errors
     *
     * @return The current Hibernate session, or null if not available
     */
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
            log.debug(DEBUG_COULD_NOT_GET_SESSION, e.getMessage());
        }
        return null;
    }
}