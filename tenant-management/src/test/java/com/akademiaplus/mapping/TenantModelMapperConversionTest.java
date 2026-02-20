/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.mapping;

import com.akademiaplus.tenancy.TenantDataModel;
import com.akademiaplus.tenancy.TenantSubscriptionDataModel;
import com.akademiaplus.usecases.TenantCreationUseCase;
import com.akademiaplus.usecases.TenantSubscriptionCreationUseCase;
import com.akademiaplus.utilities.config.ModelMapperConfig;
import openapi.akademiaplus.domain.tenant.management.dto.SubscriptionCreateRequestDTO;
import openapi.akademiaplus.domain.tenant.management.dto.TenantCreateRequestDTO;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.modelmapper.ModelMapper;

import java.math.BigDecimal;
import java.net.URI;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Validates that {@link ModelMapper} (configured via {@link ModelMapperConfig})
 * correctly converts tenant-management DTOs into their corresponding data models.
 * <p>
 * Uses a <strong>real</strong> {@code ModelMapper} to surface field-name mismatches,
 * type mismatches (e.g. {@code URI → String}), and unwanted deep-matching side effects
 * that unit tests with mocked mappers cannot detect.
 */
@DisplayName("Tenant ModelMapper DTO → DataModel Conversion")
class TenantModelMapperConversionTest {

    private static ModelMapper modelMapper;

    // ── Tenant test constants ────────────────────────────────────────────
    public static final String ORG_NAME = "AkademiaPlus Inc.";
    public static final String LEGAL_NAME = "AkademiaPlus S.A. de C.V.";
    public static final URI WEBSITE_URL = URI.create("https://akademiaplus.com");
    public static final String EMAIL = "admin@akademiaplus.com";
    public static final String ADDRESS = "Av. Insurgentes Sur 1234";
    public static final String PHONE = "+525512345678";
    public static final String LANDLINE = "+525598765432";
    public static final String DESCRIPTION = "Educational platform";
    public static final String TAX_ID = "RFC123456ABC";

    // ── Subscription test constants ───────────────────────────────────────
    public static final LocalDate BILLING_DATE = LocalDate.of(2026, 6, 15);
    public static final Double RATE_PER_STUDENT = 250.0;

    @BeforeAll
    static void setUpMapper() {
        modelMapper = new ModelMapperConfig().modelMapper();

        modelMapper.getConfiguration().setImplicitMappingEnabled(false);

        modelMapper.createTypeMap(
                TenantCreateRequestDTO.class,
                TenantDataModel.class,
                TenantCreationUseCase.MAP_NAME
        ).addMappings(mapper ->
                mapper.skip(TenantDataModel::setTenantId)
        ).implicitMappings();

        modelMapper.createTypeMap(
                SubscriptionCreateRequestDTO.class,
                TenantSubscriptionDataModel.class,
                TenantSubscriptionCreationUseCase.MAP_NAME
        ).addMappings(mapper -> {
                mapper.skip(TenantSubscriptionDataModel::setTenantSubscriptionId);
                mapper.skip(TenantSubscriptionDataModel::setMaxUsers);
        }).implicitMappings();

        modelMapper.getConfiguration().setImplicitMappingEnabled(true);
    }

    @Nested
    @DisplayName("TenantCreateRequestDTO → TenantDataModel")
    class TenantDtoToModel {

        @Test
        @DisplayName("Should map organizationName when names match exactly")
        void shouldMapOrganizationName_whenNamesMatchExactly() {
            // Given
            TenantCreateRequestDTO dto = buildTenantDto();

            // When
            TenantDataModel result = modelMapper.map(dto, TenantDataModel.class, TenantCreationUseCase.MAP_NAME);

            // Then
            assertThat(result.getOrganizationName()).isEqualTo(ORG_NAME);
        }

        @Test
        @DisplayName("Should convert URI websiteUrl to String when types differ")
        void shouldConvertUriWebsiteUrl_whenTypesDiffer() {
            // Given — DTO has URI, model has String
            TenantCreateRequestDTO dto = buildTenantDto();

            // When
            TenantDataModel result = modelMapper.map(dto, TenantDataModel.class, TenantCreationUseCase.MAP_NAME);

            // Then — URI→String converter from ModelMapperConfig
            assertThat(result.getWebsiteUrl()).isEqualTo(WEBSITE_URL.toString());
        }

        @Test
        @DisplayName("Should map all scalar fields when names match exactly")
        void shouldMapAllScalarFields_whenNamesMatchExactly() {
            // Given
            TenantCreateRequestDTO dto = buildTenantDto();

            // When
            TenantDataModel result = modelMapper.map(dto, TenantDataModel.class, TenantCreationUseCase.MAP_NAME);

            // Then
            assertThat(result.getLegalName()).isEqualTo(LEGAL_NAME);
            assertThat(result.getEmail()).isEqualTo(EMAIL);
            assertThat(result.getAddress()).isEqualTo(ADDRESS);
            assertThat(result.getPhone()).isEqualTo(PHONE);
            assertThat(result.getLandline()).isEqualTo(LANDLINE);
            assertThat(result.getDescription()).isEqualTo(DESCRIPTION);
            assertThat(result.getTaxId()).isEqualTo(TAX_ID);
        }

        @Test
        @DisplayName("Should leave tenantId null when creation DTO has no ID")
        void shouldLeaveTenantIdNull_whenCreationDtoHasNoId() {
            // Given
            TenantCreateRequestDTO dto = buildTenantDto();

            // When
            TenantDataModel result = modelMapper.map(dto, TenantDataModel.class, TenantCreationUseCase.MAP_NAME);

            // Then — DB generates via AUTO_INCREMENT
            assertThat(result.getTenantId()).isNull();
        }
    }

    @Nested
    @DisplayName("SubscriptionCreateRequestDTO → TenantSubscriptionDataModel")
    class SubscriptionDtoToModel {

        @Test
        @DisplayName("Should map all scalar fields when names match exactly")
        void shouldMapAllScalarFields_whenNamesMatchExactly() {
            // Given
            SubscriptionCreateRequestDTO dto = buildSubscriptionDto();

            // When
            TenantSubscriptionDataModel result = new TenantSubscriptionDataModel();
            modelMapper.map(dto, result, TenantSubscriptionCreationUseCase.MAP_NAME);

            // Then
            assertThat(result.getType()).isEqualTo("standard");
            assertThat(result.getBillingDate()).isEqualTo(BILLING_DATE);
            assertThat(result.getRatePerStudent()).isEqualByComparingTo(BigDecimal.valueOf(RATE_PER_STUDENT));
        }

        @Test
        @DisplayName("Should leave tenantSubscriptionId null when creation DTO has no ID")
        void shouldLeaveTenantSubscriptionIdNull_whenCreationDtoHasNoId() {
            // Given
            SubscriptionCreateRequestDTO dto = buildSubscriptionDto();

            // When
            TenantSubscriptionDataModel result = new TenantSubscriptionDataModel();
            modelMapper.map(dto, result, TenantSubscriptionCreationUseCase.MAP_NAME);

            // Then — ID assigned by EntityIdAssigner via Hibernate listener
            assertThat(result.getTenantSubscriptionId()).isNull();
        }

        @Test
        @DisplayName("Should skip maxUsers because it is JsonNullable and requires manual unwrapping")
        void shouldSkipMaxUsers_becauseItIsJsonNullable() {
            // Given — maxUsers is JsonNullable<Integer> in DTO, plain Integer in entity
            SubscriptionCreateRequestDTO dto = buildSubscriptionDto();

            // When
            TenantSubscriptionDataModel result = new TenantSubscriptionDataModel();
            modelMapper.map(dto, result, TenantSubscriptionCreationUseCase.MAP_NAME);

            // Then — maxUsers is skipped by TypeMap; use case handles unwrapping
            assertThat(result.getMaxUsers()).isNull();
        }
    }

    // ── Builder helpers ──────────────────────────────────────────────────

    private static TenantCreateRequestDTO buildTenantDto() {
        TenantCreateRequestDTO dto = new TenantCreateRequestDTO(
                ORG_NAME, EMAIL, ADDRESS
        );
        dto.setLegalName(LEGAL_NAME);
        dto.setWebsiteUrl(WEBSITE_URL);
        dto.setPhone(PHONE);
        dto.setLandline(LANDLINE);
        dto.setDescription(DESCRIPTION);
        dto.setTaxId(TAX_ID);
        return dto;
    }

    private static SubscriptionCreateRequestDTO buildSubscriptionDto() {
        SubscriptionCreateRequestDTO dto = new SubscriptionCreateRequestDTO();
        dto.setType(SubscriptionCreateRequestDTO.TypeEnum.STANDARD);
        dto.setBillingDate(BILLING_DATE);
        dto.setRatePerStudent(RATE_PER_STUDENT);
        return dto;
    }
}
