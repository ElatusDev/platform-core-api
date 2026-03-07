/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.notification.interfaceadapters;

import com.akademiaplus.notifications.PushDeviceDataModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Spring Data JPA repository for {@link PushDeviceDataModel}.
 * <p>
 * Platform-level repository — no tenant filtering is applied.
 *
 * @author ElatusDev
 * @since 1.0
 */
@Repository
public interface PushDeviceRepository extends JpaRepository<PushDeviceDataModel, Long> {

    Optional<PushDeviceDataModel> findByDeviceToken(String deviceToken);

    void deleteByDeviceToken(String deviceToken);

    boolean existsByDeviceToken(String deviceToken);
}
