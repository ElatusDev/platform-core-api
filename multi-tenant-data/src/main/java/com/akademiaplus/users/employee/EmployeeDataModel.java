/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.users.employee;

import com.akademiaplus.security.InternalAuthDataModel;
import com.akademiaplus.users.base.AbstractUser;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.io.Serial;
import java.io.Serializable;

/**
 * Entity representing an employee in the multi-tenant platform.
 * Extends AbstractUser to inherit common user functionality and adds
 * employee-specific attributes such as employee type and internal authentication.
 * <p>
 * Employees are internal users who have access to the platform's administrative
 * and operational functions. They require internal authentication credentials
 * and are categorized by employee type for role-based access control.
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Scope("prototype")
@Component
@Entity
@Table(name = "employees")
@SQLDelete(sql = "UPDATE employees SET deleted_at = CURRENT_TIMESTAMP WHERE tenant_id = ? AND employee_id = ?")
@IdClass(EmployeeDataModel.EmployeeCompositeId.class)
public class EmployeeDataModel extends AbstractUser {

    /**
     * Unique identifier for the employee within the tenant.
     * Auto-incremented per tenant for better performance.
     */
    @Id
    @Column(name = "employee_id")
    private Long employeeId;

    /**
     * Type/category of the employee (e.g., MANAGER, INSTRUCTOR, ADMIN, CLERK).
     * Used for role-based access control, business logic, and reporting.
     * <p>
     * Determines what functions and data the employee can access.
     */
    @Column(name = "employee_type", nullable = false, length = 50)
    private String employeeType;

    /**
     * Foreign key to the employee's internal authentication record.
     * Writable column used to persist the FK value during INSERT.
     */
    @Column(name = "internal_auth_id")
    private Long internalAuthId;

    /**
     * Reference to the employee's internal authentication credentials.
     * Uses composite foreign key to maintain tenant isolation.
     * <p>
     * Employees require internal auth for system access and security.
     */
    @OneToOne(optional = false, cascade = CascadeType.PERSIST, orphanRemoval = true)
    @JoinColumn(name = "tenant_id", referencedColumnName = "tenant_id", insertable=false, updatable=false)
    @JoinColumn(name = "internal_auth_id", referencedColumnName = "internal_auth_id", insertable=false, updatable=false)
    private InternalAuthDataModel internalAuth;


    /**
     * Composite primary key class for Employee entity.
     */
    @Data
    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class EmployeeCompositeId implements Serializable {
        private Long tenantId;
        private Long employeeId;
    }
}