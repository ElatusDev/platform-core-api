package com.akademiaplus.internal.interfaceadapters;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

import java.util.Optional;

@Slf4j
@RequestScope
@Component
public class TenantContextHolder {

    @Setter
    private  Integer tenantId;

    public Optional<Integer> getTenantId() {
        return Optional.of(tenantId);
    }
}