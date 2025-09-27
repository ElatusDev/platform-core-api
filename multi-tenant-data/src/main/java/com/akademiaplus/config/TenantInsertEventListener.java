package com.akademiaplus.config;

import com.akademiaplus.TenantAndSoftDeleteAwareEntity;
import com.akademiaplus.TenantContextHolder;
import org.hibernate.event.spi.PreInsertEvent;
import org.hibernate.event.spi.PreInsertEventListener;
import org.springframework.stereotype.Component;

@Component
public class TenantInsertEventListener implements PreInsertEventListener {

    private final TenantContextHolder tenantContextHolder;

    public TenantInsertEventListener(TenantContextHolder tenantContextHolder) {
        this.tenantContextHolder = tenantContextHolder;
    }

    @Override
    public boolean onPreInsert(PreInsertEvent event) {
        if (event.getEntity() instanceof TenantAndSoftDeleteAwareEntity tenantAwareEntity) {
            if (tenantAwareEntity.getTenantId() == null) {
                Integer contextTenantId = tenantContextHolder.getTenantId();
                tenantAwareEntity.setTenantId(contextTenantId);
            }
        }

        return false;
    }
}