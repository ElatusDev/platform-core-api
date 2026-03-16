/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.util.mock.tenant;

import com.akademiaplus.infra.persistence.config.TenantContextHolder;
import com.akademiaplus.tenancy.TenantBrandingDataModel;
import com.akademiaplus.util.base.DataFactory;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Factory for creating {@link TenantBrandingDataModel} instances with fake data.
 *
 * <p>Always generates exactly one branding record per tenant, since
 * tenant branding has a 1:1 relationship with the tenant entity.
 * The tenantId is obtained from {@link TenantContextHolder} because
 * {@code TenantBrandingDataModel} uses tenantId as its own primary key
 * and does not extend {@code TenantScoped}.</p>
 */
@Component
public class TenantBrandingFactory implements DataFactory<TenantBrandingDataModel> {

    private final TenantBrandingDataGenerator generator;
    private final TenantContextHolder tenantContextHolder;

    /**
     * Constructs the factory with the data generator and tenant context holder.
     *
     * @param generator            the branding data generator
     * @param tenantContextHolder  the tenant context holder for obtaining the current tenant ID
     */
    public TenantBrandingFactory(TenantBrandingDataGenerator generator,
                                 TenantContextHolder tenantContextHolder) {
        this.generator = generator;
        this.tenantContextHolder = tenantContextHolder;
    }

    /**
     * Generates exactly one {@link TenantBrandingDataModel} regardless of the
     * requested count, since each tenant has at most one branding configuration.
     *
     * @param count ignored; always produces a single-element list
     * @return a list containing one branding data model
     */
    @Override
    public List<TenantBrandingDataModel> generate(int count) {
        return List.of(createBranding());
    }

    private TenantBrandingDataModel createBranding() {
        TenantBrandingDataModel model = new TenantBrandingDataModel();
        model.setTenantId(tenantContextHolder.requireTenantId());
        model.setSchoolName(generator.schoolName());
        model.setLogoUrl(generator.logoUrl());
        model.setPrimaryColor(generator.primaryColor());
        model.setSecondaryColor(generator.secondaryColor());
        model.setFontFamily(generator.fontFamily());
        return model;
    }
}
