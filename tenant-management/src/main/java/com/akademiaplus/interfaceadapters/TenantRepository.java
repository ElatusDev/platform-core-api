package com.akademiaplus.interfaceadapters;

import com.akademiaplus.tenancy.TenantDataModel;
import lombok.NonNull;
import com.akademiaplus.utilities.persistence.repository.TenantScopedRepository;

public interface TenantRepository extends TenantScopedRepository<@NonNull TenantDataModel, @NonNull Long> {
}
