/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.makani.employee.interfaceadapters;

import com.makani.people.employee.EmployeeDataModel;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EmployeeRepository extends JpaRepository<EmployeeDataModel, Integer> {
    Optional<EmployeeDataModel> findByEmployeeId(Integer employeeId);

    @Modifying
    @Transactional
    @Query(value = "ALTER TABLE employee AUTO_INCREMENT = 1", nativeQuery = true)
    void restIdCounter();
}
