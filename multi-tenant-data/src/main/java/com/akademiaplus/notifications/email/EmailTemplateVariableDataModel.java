/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.notifications.email;

import com.akademiaplus.infra.persistence.model.TenantScoped;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * Entity representing template variable definitions in the multi-tenant notification system.
 * Describes the expected placeholders within an {@link EmailTemplateDataModel},
 * including their data type, whether they are required, and optional default values.
 * <p>
 * Each variable record is uniquely identified by the combination of
 * tenant ID and template variable ID.
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Scope("prototype")
@Component
@Entity
@Table(name = "email_template_variables")
@SQLDelete(sql = "UPDATE email_template_variables SET deleted_at = CURRENT_TIMESTAMP WHERE tenant_id = ? AND template_variable_id = ?")
@IdClass(EmailTemplateVariableDataModel.EmailTemplateVariableCompositeId.class)
public class EmailTemplateVariableDataModel extends TenantScoped {

    /**
     * Unique identifier for the template variable within the tenant.
     * Assigned by {@code EntityIdAssigner} — never set manually.
     */
    @Id
    @Column(name = "template_variable_id")
    private Long templateVariableId;

    /**
     * Reference to the parent template's ID.
     * Forms part of the foreign key relationship to {@link EmailTemplateDataModel}.
     */
    @Column(name = "template_id", nullable = false)
    private Long templateId;

    /**
     * Variable name as used in template placeholders (e.g., "firstName" for {@code {{firstName}}}).
     */
    @Column(name = "name", nullable = false, length = 50)
    private String name;

    /**
     * Data type of the variable: STRING, NUMBER, BOOLEAN, DATE, or CURRENCY.
     * Used for validation and type coercion during template rendering.
     */
    @Column(name = "variable_type", nullable = false, length = 20)
    private String variableType;

    /**
     * Optional description explaining the variable's purpose and expected values.
     */
    @Column(name = "description", length = 200)
    private String description;

    /**
     * Whether this variable must be provided when rendering the template.
     * If {@code true} and the variable is missing, rendering may fail or use a default.
     */
    @Column(name = "is_required", nullable = false)
    private boolean required;

    /**
     * Optional default value used when the variable is not provided during rendering.
     */
    @Column(name = "default_value", length = 255)
    private String defaultValue;

    /**
     * Reference to the parent email template entity with tenant-aware join.
     * Uses composite foreign key to maintain tenant isolation.
     */
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", referencedColumnName = "tenant_id", insertable = false, updatable = false)
    @JoinColumn(name = "template_id", referencedColumnName = "template_id", insertable = false, updatable = false)
    private EmailTemplateDataModel template;

    /**
     * Composite primary key class for EmailTemplateVariable entity.
     * Combines tenant ID and template variable ID for uniqueness across tenants.
     */
    @Data
    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class EmailTemplateVariableCompositeId {
        private Long tenantId;
        private Long templateVariableId;
    }
}
