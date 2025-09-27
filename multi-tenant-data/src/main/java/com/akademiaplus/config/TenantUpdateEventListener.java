package com.akademiaplus.config;

import com.akademiaplus.TenantAndSoftDeleteAwareEntity;
import com.akademiaplus.TenantContextHolder;
import org.hibernate.event.spi.PreUpdateEvent;
import org.hibernate.event.spi.PreUpdateEventListener;

import java.util.Objects;

public class TenantUpdateEventListener implements PreUpdateEventListener {

    private final TenantContextHolder tenantContextHolder;

    public TenantUpdateEventListener(TenantContextHolder tenantContextHolder) {
        this.tenantContextHolder = tenantContextHolder;
    }

    @Override
    public boolean onPreUpdate(PreUpdateEvent event) {
        if (event.getEntity() instanceof TenantAndSoftDeleteAwareEntity) {
            TenantAndSoftDeleteAwareEntity tenantAwareEntity = (TenantAndSoftDeleteAwareEntity) event.getEntity();
            Integer contextTenantId = tenantContextHolder.getTenantId();

            if (!Objects.equals(contextTenantId, tenantAwareEntity.getTenantId())) {
                throw new SecurityException("Tenant mismatch. Cannot modify entity belonging to a different tenant.");
            }
        }
        return false;
    }
}
