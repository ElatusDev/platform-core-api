package com.akademiaplus.infra.persistence.config;

import com.akademiaplus.utilities.exceptions.InvalidTenantException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Holds the current tenant context using a thread-local variable.
 * <p>
 * The tenant ID is set by the {@code TenantContextLoader} filter from the
 * {@code X-Tenant-Id} HTTP header. All tenant-scoped operations use this
 * holder to access the current tenant.
 *
 * <p>Uses {@code ThreadLocal} so the same value is visible to all components
 * on the current thread, regardless of which bean instance they hold.
 *
 * @author ElatusDev
 * @since 1.0
 */
@Slf4j
@Component
public class TenantContextHolder {

    private static final ThreadLocal<Long> TENANT_ID = new ThreadLocal<>();

    /**
     * Sets the current tenant ID for this thread.
     *
     * @param tenantId the tenant ID
     */
    public void setTenantId(Long tenantId) {
        TENANT_ID.set(tenantId);
    }

    /**
     * Returns the current tenant ID, if set.
     *
     * @return optional containing the tenant ID, or empty if not set
     */
    public Optional<Long> getTenantId() {
        return Optional.ofNullable(TENANT_ID.get());
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

    /**
     * Clears the tenant context for this thread.
     */
    public void clear() {
        TENANT_ID.remove();
    }
}