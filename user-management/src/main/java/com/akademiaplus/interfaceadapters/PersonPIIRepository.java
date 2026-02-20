/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.interfaceadapters;

import com.akademiaplus.utilities.persistence.repository.TenantScopedRepository;
import com.akademiaplus.users.base.PersonPIIDataModel;

public interface PersonPIIRepository extends TenantScopedRepository<PersonPIIDataModel, PersonPIIDataModel.PersonPIICompositeId> {
}
