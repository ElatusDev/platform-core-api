/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.users.customer;

import com.akademiaplus.security.CustomerAuthDataModel;
import com.akademiaplus.users.base.AbstractUser;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.io.Serializable;

/**
 * Entity representing an adult student in the multi-tenant platform.
 * Extends AbstractUser to inherit common user functionality and adds
 * adult student-specific attributes such as customer authentication.
 * <p>
 * Adult students are external users who enroll in courses and services.
 * They use customer authentication (OAuth, social login, etc.) rather than
 * internal authentication. They can manage their own enrollments and payments.
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Scope("prototype")
@Component
@Entity
@Table(name = "adult_students")
@SQLDelete(sql = "UPDATE adult_students SET deleted_at = CURRENT_TIMESTAMP WHERE tenant_id = ?")
@IdClass(AdultStudentDataModel.AdultStudentCompositeId.class)
public class AdultStudentDataModel extends AbstractUser {

    /**
     * Unique identifier for the adult student within the tenant.
     * Auto-incremented per tenant for better performance.
     */
    @Id
    @Column(name = "adult_student_id")
    private Long adultStudentId;

    /**
     * Reference to the student's customer authentication credentials.
     * Uses composite foreign key to maintain tenant isolation.
     * <p>
     * Adult students authenticate through external providers (OAuth, social login, etc.)
     * rather than internal system credentials.
     */
    @OneToOne(optional = false, cascade = CascadeType.PERSIST, orphanRemoval = true)
    @JoinColumn(name = "tenant_id", referencedColumnName = "tenant_id", insertable=false, updatable=false)
    @JoinColumn(name = "customer_auth_id", referencedColumnName = "customer_auth_id", insertable=false, updatable=false)
    private CustomerAuthDataModel customerAuth;

    /**
     * Composite primary key class for AdultStudent entity.
     */
    @Data
    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class AdultStudentCompositeId implements Serializable {
        protected Integer tenantId;
        protected Integer adultStudentId;
    }
}