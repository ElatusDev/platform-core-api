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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link TenantBrandingFactory}.
 */
@DisplayName("TenantBrandingFactory")
@ExtendWith(MockitoExtension.class)
class TenantBrandingFactoryTest {

    private static final long TENANT_ID = 42L;

    @Mock
    private TenantContextHolder tenantContextHolder;

    private TenantBrandingFactory factory;

    @BeforeEach
    void setUp() {
        TenantBrandingDataGenerator generator = new TenantBrandingDataGenerator();
        factory = new TenantBrandingFactory(generator, tenantContextHolder);
    }

    @Nested
    @DisplayName("List generation")
    class ListGeneration {

        @Test
        @DisplayName("Should always generate exactly one branding record regardless of count")
        void shouldAlwaysGenerateExactlyOne_regardlessOfCount() {
            // Given
            when(tenantContextHolder.requireTenantId()).thenReturn(TENANT_ID);

            // When
            List<TenantBrandingDataModel> result = factory.generate(5);

            // Then
            assertThat(result).hasSize(1);
        }

        @Test
        @DisplayName("Should generate one record even when count is zero")
        void shouldGenerateOneRecord_evenWhenCountIsZero() {
            // Given
            when(tenantContextHolder.requireTenantId()).thenReturn(TENANT_ID);

            // When
            List<TenantBrandingDataModel> result = factory.generate(0);

            // Then
            assertThat(result).hasSize(1);
        }
    }

    @Nested
    @DisplayName("Data model population")
    class DataModelPopulation {

        @Test
        @DisplayName("Should populate all required fields including tenant ID from context holder")
        void shouldPopulateAllRequiredFields_includingTenantIdFromContextHolder() {
            // Given
            when(tenantContextHolder.requireTenantId()).thenReturn(TENANT_ID);

            // When
            List<TenantBrandingDataModel> result = factory.generate(1);
            TenantBrandingDataModel model = result.get(0);

            // Then
            assertThat(model.getTenantId()).isEqualTo(TENANT_ID);
            assertThat(model.getSchoolName()).isNotBlank();
            assertThat(model.getLogoUrl()).isNotBlank();
            assertThat(model.getPrimaryColor()).isNotBlank();
            assertThat(model.getSecondaryColor()).isNotBlank();
            assertThat(model.getFontFamily()).isNotBlank();
        }
    }
}
