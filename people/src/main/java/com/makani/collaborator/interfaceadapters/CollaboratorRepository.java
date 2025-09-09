/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.makani.collaborator.interfaceadapters;

import com.makani.people.collaborator.CollaboratorDataModel;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface CollaboratorRepository extends JpaRepository<CollaboratorDataModel, Integer> {
    @Modifying
    @Transactional
    @Query(value = "ALTER TABLE collaborator AUTO_INCREMENT = 1", nativeQuery = true)
    void restIdCounter();
}
