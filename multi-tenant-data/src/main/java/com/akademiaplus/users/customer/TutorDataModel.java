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
 * Entity representing a tutor in the multi-tenant platform.
 * Extends AbstractUser to inherit common user functionality and adds
 * tutor-specific attributes such as optional customer authentication.
 * <p>
 * Tutors are responsible guardians who manage minor student accounts and enrollments.
 * They may have customer authentication for self-service capabilities but can also
 * be managed directly by platform administrators.
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Scope("prototype")
@Component
@Entity
@Table(name = "tutors")
@SQLDelete(sql = "UPDATE tutors SET deleted_at = CURRENT_TIMESTAMP WHERE tenant_id = ?")
@IdClass(TutorDataModel.TutorCompositeId.class)
public class TutorDataModel extends AbstractUser {

    /**
     * Unique identifier for the tutor within the tenant.
     * Auto-incremented per tenant for better performance.
     */
    @Id
    @Column(name = "tutor_id")
    private Integer tutorId;

    /**
     * Reference to the tutor's customer authentication credentials.
     * Uses composite foreign key to maintain tenant isolation.
     * <p>
     * Optional field as tutors may be managed directly by administrators
     * without requiring self-service authentication capabilities.
     */
    @OneToOne(optional = true, cascade = CascadeType.PERSIST, orphanRemoval = true)
    @JoinColumn(name = "tenant_id", referencedColumnName = "tenant_id", insertable=false, updatable=false)
    @JoinColumn(name = "customer_auth_id", referencedColumnName = "customer_auth_id", insertable=false, updatable=false)
    private CustomerAuthDataModel customerAuth;

    /**
     * Composite primary key class for Tutor entity.
     */
    @Data
    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class TutorCompositeId implements Serializable {
        protected Integer tenantId;
        protected Integer tutorId;
    }
}