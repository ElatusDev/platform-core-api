package com.akademiaplus.infra.listeners;

import com.akademiaplus.infra.TenantContextHolder;
import com.akademiaplus.infra.TenantScoped;
import com.akademiaplus.infra.exception.InvalidTenantException;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.event.spi.*;
import org.hibernate.persister.entity.EntityPersister;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
@Slf4j
@RequiredArgsConstructor
public class TenantPreInsertEventListener implements PreInsertEventListener {

    @Getter
    private final ObjectProvider<@NonNull TenantContextHolder> tenantContextHolderProvider;

    @Override
    public boolean onPreInsert(PreInsertEvent event) {
        log.debug("PRE_INSERT event for: {}", event.getEntity().getClass().getSimpleName());

        if (event.getEntity() instanceof TenantScoped tenantEntity) {
            TenantContextHolder holder = tenantContextHolderProvider.getIfAvailable();
            Integer tenantId = Objects.requireNonNull(holder).getTenantId()
                                        .orElseThrow(()-> new InvalidTenantException("Missing tenant!"));
            tenantEntity.setTenantId(tenantId);
            this.updateState(event.getPersister(), event.getState(), tenantId);
            log.debug("Set tenant {} for new entity", tenantId);
        }
        return false;
    }

    private void updateState(EntityPersister persister, Object[] state, Integer tenantId) {
        String[] propertyNames = persister.getPropertyNames();
        for (int i = 0; i < propertyNames.length; i++) {
            if ("tenantId".equals(propertyNames[i])) {
                state[i] = tenantId;
                break;
            }
        }
    }
}