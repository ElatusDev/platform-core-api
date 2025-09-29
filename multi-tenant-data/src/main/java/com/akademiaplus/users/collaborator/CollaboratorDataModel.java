/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.users.collaborator;

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

import java.io.Serializable;

/**
 * Entity representing a collaborator in the multi-tenant platform.
 * Extends AbstractUser to inherit common user functionality and adds
 * collaborator-specific attributes such as skills and internal authentication.
 * <p>
 * Collaborators are internal users who provide instructional or support services.
 * They have specific skills that can be matched to courses and events.
 * Like employees, they require internal authentication for system access.
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Scope("prototype")
@Component
@Entity
@Table(name = "collaborators")
@SQLDelete(sql = "UPDATE collaborators SET deleted_at = CURRENT_TIMESTAMP WHERE tenant_id = ?")
@IdClass(CollaboratorDataModel.CollaboratorCompositeId.class)
public class CollaboratorDataModel extends AbstractUser {

    /**
     * Unique identifier for the collaborator within the tenant.
     * Auto-incremented per tenant for better performance.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "collaborator_id")
    private Integer collaboratorId;

    /**
     * Skills and competencies of the collaborator.
     * Used for matching collaborators to appropriate courses and events.
     * <p>
     * Can include technical skills, languages, certifications, etc.
     * Example: "Java Programming, Database Design, Spanish, Project Management"
     */
    @Column(name = "skills", nullable = false, length = 100)
    private String skills;

    /**
     * Reference to the collaborator's internal authentication credentials.
     * Uses composite foreign key to maintain tenant isolation.
     * <p>
     * Collaborators require internal auth for system access and course management.
     */
    @OneToOne(optional = false, cascade = CascadeType.PERSIST, orphanRemoval = true)
    @JoinColumn(name = "tenant_id", referencedColumnName = "tenant_id")
    @JoinColumn(name = "internal_auth_id", referencedColumnName = "internal_auth_id")
    private InternalAuthDataModel internalAuth;

    /**
     * Composite primary key class for Collaborator entity.
     */
    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class CollaboratorCompositeId implements Serializable {
        protected Integer tenantId;
        protected Integer collaboratorId;

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof CollaboratorCompositeId that)) return false;
            return tenantId.equals(that.tenantId) && collaboratorId.equals(that.collaboratorId);
        }

        @Override
        public int hashCode() {
            return java.util.Objects.hash(tenantId, collaboratorId);
        }

        @Override
        public String toString() {
            return getClass().getSimpleName() + "{tenantId=" + tenantId + ", collaboratorId=" + collaboratorId + "}";
        }
    }
}