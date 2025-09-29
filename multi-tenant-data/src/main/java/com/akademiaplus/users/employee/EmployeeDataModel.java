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
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
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
@SQLDelete(sql = "UPDATE employees SET deleted_at = CURRENT_TIMESTAMP WHERE tenant_id = ?")
@IdClass(EmployeeDataModel.EmployeeCompositeId.class)
public class EmployeeDataModel extends AbstractUser implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * Unique identifier for the employee within the tenant.
     * Auto-incremented per tenant for better performance.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "employee_id")
    private Integer employeeId;

    /**
     * Type/category of the employee (e.g., MANAGER, INSTRUCTOR, ADMIN, CLERK).
     * Used for role-based access control, business logic, and reporting.
     * <p>
     * Determines what functions and data the employee can access.
     */
    @Column(name = "employee_type", nullable = false, length = 50)
    private String employeeType;

    /**
     * Reference to the employee's internal authentication credentials.
     * Uses composite foreign key to maintain tenant isolation.
     * <p>
     * Employees require internal auth for system access and security.
     */
    @OneToOne(optional = false, cascade = CascadeType.PERSIST, orphanRemoval = true)
    @JoinColumn(name = "tenant_id", referencedColumnName = "tenant_id")
    @JoinColumn(name = "internal_auth_id", referencedColumnName = "internal_auth_id")
    private InternalAuthDataModel internalAuth;


    /**
     * Composite primary key class for Employee entity.
     */
    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class EmployeeCompositeId implements Serializable {
        private Integer tenantId;
        private Integer employeeId;

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof EmployeeCompositeId that)) return false;
            return tenantId.equals(that.tenantId) && employeeId.equals(that.employeeId);
        }

        @Override
        public int hashCode() {
            return java.util.Objects.hash(tenantId, employeeId);
        }

        @Override
        public String toString() {
            return getClass().getSimpleName() + "{tenantId=" + tenantId + ", employeeId=" + employeeId + "}";
        }
    }
}