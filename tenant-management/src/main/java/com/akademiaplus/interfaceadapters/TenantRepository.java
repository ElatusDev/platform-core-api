package com.akademiaplus.interfaceadapters;

import com.akademiaplus.tenancy.TenantDataModel;
import lombok.NonNull;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TenantRepository extends JpaRepository<@NonNull TenantDataModel, @NonNull Long> {
}
