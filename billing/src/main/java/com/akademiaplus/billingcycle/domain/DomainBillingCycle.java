/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.billingcycle.domain;

import com.akademiaplus.billingcycle.domain.exception.InvalidBillingTransitionException;
import com.akademiaplus.tenancy.BillingStatus;
import com.akademiaplus.tenancy.TenantBillingCycleDataModel;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;

/**
 * Encapsulates business rules for the BillingCycle entity.
 *
 * <p>Enforces the billing cycle state machine: PENDING → BILLED → PAID/FAILED,
 * FAILED → BILLED/CANCELLED. PAID and CANCELLED are terminal states.</p>
 *
 * <p>Has zero I/O — all data is received via {@link #get(TenantBillingCycleDataModel)}.</p>
 *
 * @see TenantBillingCycleDataModel
 * @see BillingStatus
 * @author ElatusDev
 * @since 1.0
 */
@Component
public class DomainBillingCycle {

    /** Valid transitions: from → allowed targets. */
    public static final Map<BillingStatus, Set<BillingStatus>> TRANSITIONS = Map.of(
            BillingStatus.PENDING, Set.of(BillingStatus.BILLED, BillingStatus.CANCELLED),
            BillingStatus.BILLED, Set.of(BillingStatus.PAID, BillingStatus.FAILED),
            BillingStatus.FAILED, Set.of(BillingStatus.BILLED, BillingStatus.CANCELLED)
    );

    /** Terminal states — no further transitions allowed. */
    public static final Set<BillingStatus> TERMINAL_STATES = Set.of(
            BillingStatus.PAID, BillingStatus.CANCELLED
    );

    private TenantBillingCycleDataModel dataModel;

    /**
     * Entry point — sets the data model for subsequent operations.
     *
     * @param cycle the billing cycle data model to operate on
     * @return this instance for fluent chaining
     */
    public DomainBillingCycle get(TenantBillingCycleDataModel cycle) {
        this.dataModel = cycle;
        return this;
    }

    /**
     * Validates that the given transition is legal per the state machine.
     *
     * @param target the desired next status
     * @return this instance for fluent chaining
     * @throws InvalidBillingTransitionException if transition is not allowed
     */
    public DomainBillingCycle validateTransition(BillingStatus target) {
        BillingStatus current = dataModel.getBillingStatus();
        Set<BillingStatus> allowed = TRANSITIONS.getOrDefault(current, Set.of());
        if (!allowed.contains(target)) {
            throw new InvalidBillingTransitionException(current, target);
        }
        return this;
    }
}
