/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.collaborator.interfaceadapters;

import com.akademiaplus.users.collaborator.CollaboratorDataModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CollaboratorRepository extends JpaRepository<CollaboratorDataModel, Integer> {
}
