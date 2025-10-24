/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.interfaceadapters;

import com.akademiaplus.users.base.PersonPIIDataModel;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PersonPIIRepository extends JpaRepository<PersonPIIDataModel, Long> {
}
