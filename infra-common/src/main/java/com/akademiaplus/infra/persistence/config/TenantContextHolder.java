package com.akademiaplus.infra.persistence.config;

import com.akademiaplus.utilities.exceptions.InvalidTenantException;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

import java.util.Optional;

/**
 * Holds the current tenant context for the request scope.
 * <p>
 * The tenant ID is set by the {@code TenantContextLoader} filter from the
 * {@code X-Tenant-Id} HTTP header. All tenant-scoped operations use this
 * holder to access the current tenant.
 *
 * @author ElatusDev
 * @since 1.0
 */
@Slf4j
@RequestScope
@Component
public class TenantContextHolder {

    @Setter
    private Long tenantId;

    /**
     * Returns the current tenant ID, if set.
     *
     * @return optional containing the tenant ID, or empty if not set
     */
    public Optional<Long> getTenantId() {
        return Optional.of(tenantId);
    }

    /**
     * Returns the current tenant ID or throws {@link InvalidTenantException}
     * if no tenant context is set.
     *
     * @return the current tenant ID, never null
     * @throws InvalidTenantException if tenant context is missing
     */
    public Long requireTenantId() {
        return getTenantId()
                .orElseThrow(InvalidTenantException::new);
    }
}