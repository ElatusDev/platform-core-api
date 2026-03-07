/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.leadmanagement.interfaceadapters;

import com.akademiaplus.leadmanagement.DemoRequestDataModel;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data JPA repository for {@link DemoRequestDataModel}.
 * <p>
 * Platform-level repository — no tenant filtering is applied.
 *
 * @author ElatusDev
 * @since 1.0
 */
public interface DemoRequestRepository extends JpaRepository<DemoRequestDataModel, Long> {

    /**
     * Checks whether a demo request with the given email already exists.
     *
     * @param email the email to check
     * @return {@code true} if a demo request with the email exists
     */
    boolean existsByEmail(String email);
}
