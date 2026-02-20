/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.membership.interfaceadapters;

import com.akademiaplus.billing.customerpayment.PaymentTutorDataModel;
import com.akademiaplus.utilities.persistence.repository.TenantScopedRepository;

public interface PaymentTutorRepository extends TenantScopedRepository<PaymentTutorDataModel, PaymentTutorDataModel.PaymentTutorCompositeId> {
}
