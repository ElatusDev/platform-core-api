package com.akademiaplus.infra.persistence.config;

import com.akademiaplus.utilities.exceptions.InvalidTenantException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Servlet filter that extracts and establishes tenant context from HTTP request headers.
 * <p>
 * This filter is executed first in the filter chain (HIGHEST_PRECEDENCE) to ensure
 * tenant context is established before any other filters or application logic runs.
 * It extracts the tenant ID from the X-Tenant-Id header and stores it in the
 * TenantContextHolder for use throughout the request lifecycle.
 * <p>
 * If no valid tenant ID is found, the request is rejected with a 400 Bad Request error.
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TenantContextLoader extends OncePerRequestFilter {

    // HTTP headers
    private static final String TENANT_HEADER = "X-Tenant-Id";

    // Error messages (public for testing)
    public static final String ERROR_TENANT_HEADER_REQUIRED = "X-Tenant-Id header is required";
    public static final String ERROR_NO_TENANT_ID_FOUND = "No tenant ID found";

    // Log messages (private - internal only)
    private static final String ERROR_NO_TENANT_FOR_REQUEST = "No tenant ID found for request: {} {}";
    private static final String DEBUG_TENANT_SET_IN_CONTEXT = "✅ Set tenant {} in context for: {} {}";
    private static final String ERROR_INVALID_TENANT_FORMAT = "Invalid tenant ID format: {}";

    private final TenantContextHolder tenantContextHolder;

    /**
     * Constructor for dependency injection.
     *
     * @param tenantContextHolder The holder for current tenant context
     */
    public TenantContextLoader(TenantContextHolder tenantContextHolder) {
        this.tenantContextHolder = tenantContextHolder;
    }

    /**
     * Skips tenant context extraction for infrastructure endpoints that are not
     * tenant-scoped (healthchecks, login, OpenAPI docs).
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        // Strip context path (e.g. /api) for matching
        String contextPath = request.getContextPath();
        if (contextPath != null && !contextPath.isEmpty()) {
            path = path.substring(contextPath.length());
        }
        return path.startsWith("/actuator")
                || path.startsWith("/v1/security/login");
    }

    /**
     * Filters each HTTP request to extract and establish tenant context.
     * <p>
     * The method performs the following operations:
     * 1. Extracts tenant ID from X-Tenant-Id header
     * 2. Validates that tenant ID exists and is valid
     * 3. Stores tenant ID in TenantContextHolder
     * 4. Rejects request if tenant ID is missing or invalid
     *
     * @param request The HTTP request
     * @param response The HTTP response
     * @param filterChain The filter chain
     * @throws ServletException if a servlet error occurs
     * @throws IOException if an I/O error occurs
     * @throws InvalidTenantException if no valid tenant ID is found
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {
        Long tenantId = extractTenantId(request);

        if (tenantId == null) {
            log.error(ERROR_NO_TENANT_FOR_REQUEST,
                    request.getMethod(), request.getRequestURI());
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, ERROR_TENANT_HEADER_REQUIRED);
            throw new InvalidTenantException(ERROR_NO_TENANT_ID_FOUND);
        }

        tenantContextHolder.setTenantId(tenantId);
        log.debug(DEBUG_TENANT_SET_IN_CONTEXT,
                tenantId, request.getMethod(), request.getRequestURI());

        filterChain.doFilter(request, response);
    }

    /**
     * Extracts tenant ID from the X-Tenant-Id header.
     * <p>
     * The method safely parses the header value as an integer and handles:
     * - Missing header
     * - Empty or whitespace-only header
     * - Invalid number format
     *
     * @param request The HTTP request
     * @return The tenant ID, or null if not found or invalid
     */
    private Long extractTenantId(HttpServletRequest request) {
        String tenantHeader = request.getHeader(TENANT_HEADER);
        if (tenantHeader != null && !tenantHeader.trim().isEmpty()) {
            try {
                return Long.valueOf(tenantHeader.trim());
            } catch (NumberFormatException e) {
                log.error(ERROR_INVALID_TENANT_FORMAT, tenantHeader, e);
                return null;
            }
        }
        return null;
    }
}