package com.akademiaplus.config;

import com.akademiaplus.TenantAndSoftDeleteAwareEntity;
import com.akademiaplus.TenantContextHolder;
import org.hibernate.event.spi.PreDeleteEvent;
import org.hibernate.event.spi.PreDeleteEventListener;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
public class TenantDeleteEventListener implements PreDeleteEventListener {

    private final TenantContextHolder tenantContextHolder;

    public TenantDeleteEventListener(TenantContextHolder tenantContextHolder) {
        this.tenantContextHolder = tenantContextHolder;
    }

    @Override
    public boolean onPreDelete(PreDeleteEvent event) {
        if (event.getEntity() instanceof TenantAndSoftDeleteAwareEntity tenantAwareEntity) {
            Integer contextTenantId = tenantContextHolder.getTenantId();
            if (!Objects.equals(contextTenantId, tenantAwareEntity.getTenantId())) {
                throw new SecurityException("Tenant mismatch. Cannot delete entity belonging to a different tenant.");
            }
        }
        return false;
    }
}