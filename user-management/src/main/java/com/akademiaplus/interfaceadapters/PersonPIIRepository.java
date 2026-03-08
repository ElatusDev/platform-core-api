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
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;

import java.util.Optional;

public interface PersonPIIRepository extends TenantScopedRepository<PersonPIIDataModel, PersonPIIDataModel.PersonPIICompositeId> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    boolean existsByEmailHash(String emailHash);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    boolean existsByPhoneHash(String phoneHash);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    boolean existsByEmailHashAndPersonPiiIdNot(String emailHash, Long personPiiId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    boolean existsByPhoneHashAndPersonPiiIdNot(String phoneHash, Long personPiiId);

    /**
     * Finds a person's PII record by email hash.
     *
     * @param emailHash the SHA-256 hash of the normalized email
     * @return the PersonPII if found
     */
    Optional<PersonPIIDataModel> findByEmailHash(String emailHash);
}
