/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.interfaceadapters;

import com.akademiaplus.usecases.DeleteTenantUseCase;
import com.akademiaplus.usecases.DeleteTenantSubscriptionUseCase;
import com.akademiaplus.usecases.GetAllTenantBillingCyclesUseCase;
import com.akademiaplus.usecases.GetAllTenantSubscriptionsUseCase;
import com.akademiaplus.usecases.GetAllTenantsUseCase;
import com.akademiaplus.usecases.GetTenantBillingCycleByIdUseCase;
import com.akademiaplus.usecases.GetTenantByIdUseCase;
import com.akademiaplus.usecases.GetTenantSubscriptionByIdUseCase;
import com.akademiaplus.usecases.TenantCreationUseCase;
import com.akademiaplus.usecases.TenantSubscriptionCreationUseCase;
import com.akademiaplus.usecases.TenantBillingCycleCreationUseCase;
import openapi.akademiaplus.domain.tenant.management.api.TenantsApi;
import openapi.akademiaplus.domain.tenant.management.dto.BillingCycleCreateRequestDTO;
import openapi.akademiaplus.domain.tenant.management.dto.BillingCycleDTO;
import openapi.akademiaplus.domain.tenant.management.dto.BillingCycleDetailsDTO;
import openapi.akademiaplus.domain.tenant.management.dto.BillingStatusDTO;
import openapi.akademiaplus.domain.tenant.management.dto.ListBillingCycles200ResponseDTO;
import openapi.akademiaplus.domain.tenant.management.dto.ListTenantSubscriptions200ResponseDTO;
import openapi.akademiaplus.domain.tenant.management.dto.ListTenants200ResponseDTO;
import openapi.akademiaplus.domain.tenant.management.dto.SubscriptionCreateRequestDTO;
import openapi.akademiaplus.domain.tenant.management.dto.TenantCreateRequestDTO;
import openapi.akademiaplus.domain.tenant.management.dto.TenantDTO;
import openapi.akademiaplus.domain.tenant.management.dto.TenantDetailsDTO;
import openapi.akademiaplus.domain.tenant.management.dto.TenantSubscriptionDTO;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * REST controller for tenant lifecycle operations.
 *
 * <p>Implements the OpenAPI-generated {@link TenantsApi} interface,
 * delegating to domain use cases for CRUD operations on tenants,
 * subscriptions, and billing cycles.
 *
 * <p>Unlike domain-entity controllers, tenant endpoints are not
 * tenant-scoped — the tenant IS the root entity, so there is no
 * {@code X-Tenant-Id} header requirement for tenant-level operations.
 * Subscription and billing cycle endpoints do require tenant context.
 *
 * @author ElatusDev
 * @since 1.0
 */
@RestController
@RequestMapping("/v1")
public class TenantController implements TenantsApi {

    private final TenantCreationUseCase tenantCreationUseCase;
    private final DeleteTenantUseCase deleteTenantUseCase;
    private final GetTenantByIdUseCase getTenantByIdUseCase;
    private final GetAllTenantsUseCase getAllTenantsUseCase;
    private final TenantSubscriptionCreationUseCase tenantSubscriptionCreationUseCase;
    private final DeleteTenantSubscriptionUseCase deleteTenantSubscriptionUseCase;
    private final GetTenantSubscriptionByIdUseCase getTenantSubscriptionByIdUseCase;
    private final GetAllTenantSubscriptionsUseCase getAllTenantSubscriptionsUseCase;
    private final TenantBillingCycleCreationUseCase tenantBillingCycleCreationUseCase;
    private final GetTenantBillingCycleByIdUseCase getTenantBillingCycleByIdUseCase;
    private final GetAllTenantBillingCyclesUseCase getAllTenantBillingCyclesUseCase;

    /**
     * Constructs the controller with all required use cases.
     *
     * @param tenantCreationUseCase              the tenant creation use case
     * @param deleteTenantUseCase                the tenant deletion use case
     * @param getTenantByIdUseCase               the get tenant by ID use case
     * @param getAllTenantsUseCase                the get all tenants use case
     * @param tenantSubscriptionCreationUseCase   the subscription creation use case
     * @param deleteTenantSubscriptionUseCase     the subscription deletion use case
     * @param getTenantSubscriptionByIdUseCase    the get subscription by ID use case
     * @param getAllTenantSubscriptionsUseCase     the get all subscriptions use case
     * @param tenantBillingCycleCreationUseCase   the billing cycle creation use case
     * @param getTenantBillingCycleByIdUseCase    the get billing cycle by ID use case
     * @param getAllTenantBillingCyclesUseCase     the get all billing cycles use case
     */
    public TenantController(TenantCreationUseCase tenantCreationUseCase,
                            DeleteTenantUseCase deleteTenantUseCase,
                            GetTenantByIdUseCase getTenantByIdUseCase,
                            GetAllTenantsUseCase getAllTenantsUseCase,
                            TenantSubscriptionCreationUseCase tenantSubscriptionCreationUseCase,
                            DeleteTenantSubscriptionUseCase deleteTenantSubscriptionUseCase,
                            GetTenantSubscriptionByIdUseCase getTenantSubscriptionByIdUseCase,
                            GetAllTenantSubscriptionsUseCase getAllTenantSubscriptionsUseCase,
                            TenantBillingCycleCreationUseCase tenantBillingCycleCreationUseCase,
                            GetTenantBillingCycleByIdUseCase getTenantBillingCycleByIdUseCase,
                            GetAllTenantBillingCyclesUseCase getAllTenantBillingCyclesUseCase) {
        this.tenantCreationUseCase = tenantCreationUseCase;
        this.deleteTenantUseCase = deleteTenantUseCase;
        this.getTenantByIdUseCase = getTenantByIdUseCase;
        this.getAllTenantsUseCase = getAllTenantsUseCase;
        this.tenantSubscriptionCreationUseCase = tenantSubscriptionCreationUseCase;
        this.deleteTenantSubscriptionUseCase = deleteTenantSubscriptionUseCase;
        this.getTenantSubscriptionByIdUseCase = getTenantSubscriptionByIdUseCase;
        this.getAllTenantSubscriptionsUseCase = getAllTenantSubscriptionsUseCase;
        this.tenantBillingCycleCreationUseCase = tenantBillingCycleCreationUseCase;
        this.getTenantBillingCycleByIdUseCase = getTenantBillingCycleByIdUseCase;
        this.getAllTenantBillingCyclesUseCase = getAllTenantBillingCyclesUseCase;
    }

    // ── Tenant operations ──────────────────────────────────────────────

    /** {@inheritDoc} */
    @Override
    public ResponseEntity<TenantDTO> createTenant(
            TenantCreateRequestDTO tenantCreateRequestDTO) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(tenantCreationUseCase.create(tenantCreateRequestDTO));
    }

    /** {@inheritDoc} */
    @Override
    public ResponseEntity<Void> deleteTenant(Long tenantId) {
        deleteTenantUseCase.delete(tenantId);
        return ResponseEntity.noContent().build();
    }

    /** {@inheritDoc} */
    @Override
    public ResponseEntity<TenantDetailsDTO> getTenant(Long tenantId,
                                                       Boolean includeSubscriptions,
                                                       Boolean includeBilling) {
        return ResponseEntity.ok(getTenantByIdUseCase.get(tenantId));
    }

    /** {@inheritDoc} */
    @Override
    public ResponseEntity<ListTenants200ResponseDTO> listTenants(Integer page,
                                                                   Integer limit,
                                                                   @Nullable String search) {
        List<TenantDTO> tenants = getAllTenantsUseCase.getAll();
        ListTenants200ResponseDTO response = new ListTenants200ResponseDTO();
        response.setData(tenants);
        return ResponseEntity.ok(response);
    }

    // ── Subscription operations ────────────────────────────────────────

    /** {@inheritDoc} */
    @Override
    public ResponseEntity<TenantSubscriptionDTO> createTenantSubscription(
            Long tenantId,
            SubscriptionCreateRequestDTO subscriptionCreateRequestDTO) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(tenantSubscriptionCreationUseCase.create(subscriptionCreateRequestDTO));
    }

    /** {@inheritDoc} */
    @Override
    public ResponseEntity<Void> cancelTenantSubscription(Long tenantId, Long subscriptionId) {
        deleteTenantSubscriptionUseCase.delete(subscriptionId);
        return ResponseEntity.noContent().build();
    }

    /** {@inheritDoc} */
    @Override
    public ResponseEntity<TenantSubscriptionDTO> getTenantSubscription(Long tenantId,
                                                                        Long subscriptionId) {
        return ResponseEntity.ok(getTenantSubscriptionByIdUseCase.get(subscriptionId));
    }

    /** {@inheritDoc} */
    @Override
    public ResponseEntity<ListTenantSubscriptions200ResponseDTO> listTenantSubscriptions(
            Long tenantId) {
        List<TenantSubscriptionDTO> subscriptions = getAllTenantSubscriptionsUseCase.getAll();
        ListTenantSubscriptions200ResponseDTO response = new ListTenantSubscriptions200ResponseDTO();
        response.setData(subscriptions);
        return ResponseEntity.ok(response);
    }

    // ── Billing cycle operations ───────────────────────────────────────

    /** {@inheritDoc} */
    @Override
    public ResponseEntity<BillingCycleDTO> createBillingCycle(
            Long tenantId,
            BillingCycleCreateRequestDTO billingCycleCreateRequestDTO) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(tenantBillingCycleCreationUseCase.create(billingCycleCreateRequestDTO));
    }

    /** {@inheritDoc} */
    @Override
    public ResponseEntity<BillingCycleDetailsDTO> getBillingCycle(Long tenantId,
                                                                   Long billingCycleId) {
        return ResponseEntity.ok(getTenantBillingCycleByIdUseCase.get(billingCycleId));
    }

    /** {@inheritDoc} */
    @Override
    public ResponseEntity<ListBillingCycles200ResponseDTO> listBillingCycles(
            Long tenantId,
            @Nullable LocalDate fromDate,
            @Nullable LocalDate toDate,
            @Nullable BillingStatusDTO status) {
        List<BillingCycleDTO> billingCycles = getAllTenantBillingCyclesUseCase.getAll();
        ListBillingCycles200ResponseDTO response = new ListBillingCycles200ResponseDTO();
        response.setData(billingCycles);
        return ResponseEntity.ok(response);
    }
}
