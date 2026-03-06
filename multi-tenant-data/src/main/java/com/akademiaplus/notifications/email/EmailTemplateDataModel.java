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

import java.util.List;

/**
 * Entity representing email templates in the multi-tenant notification system.
 * Stores reusable email template content with variable placeholders,
 * enabling consistent and personalized email communication.
 * <p>
 * Each template is uniquely identified within a tenant and can define
 * multiple template variables for dynamic content substitution.
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Scope("prototype")
@Component
@Entity
@Table(name = "email_templates")
@SQLDelete(sql = "UPDATE email_templates SET deleted_at = CURRENT_TIMESTAMP WHERE tenant_id = ? AND template_id = ?")
@IdClass(EmailTemplateDataModel.EmailTemplateCompositeId.class)
public class EmailTemplateDataModel extends TenantScoped {

    /**
     * Unique identifier for the template within the tenant.
     * Assigned by {@code EntityIdAssigner} — never set manually.
     */
    @Id
    @Column(name = "template_id")
    private Long templateId;

    /**
     * Human-readable name for the template.
     * Used for identification and search in the template management UI.
     */
    @Column(name = "name", nullable = false, length = 100)
    private String name;

    /**
     * Optional description explaining the template's purpose and usage.
     */
    @Column(name = "description", length = 500)
    private String description;

    /**
     * Optional category for organizing templates (e.g., "welcome", "billing", "enrollment").
     */
    @Column(name = "category", length = 50)
    private String category;

    /**
     * Email subject line template with optional {@code {{variable}}} placeholders.
     */
    @Column(name = "subject_template", nullable = false, length = 255)
    private String subjectTemplate;

    /**
     * HTML body template content with {@code {{variable}}} placeholders.
     * Rendered by {@code EmailTemplateRenderingService} before sending.
     */
    @Column(name = "body_html", nullable = false, columnDefinition = "TEXT")
    private String bodyHtml;

    /**
     * Optional plain-text body template for email clients that do not support HTML.
     */
    @Column(name = "body_text", columnDefinition = "TEXT")
    private String bodyText;

    /**
     * Whether this template is currently active and available for use.
     * Inactive templates are retained but excluded from template listings.
     */
    @Column(name = "is_active", nullable = false)
    private boolean isActive;

    /**
     * List of variable definitions associated with this template.
     * Describes the expected placeholders and their metadata.
     */
    @OneToMany(mappedBy = "template", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private List<EmailTemplateVariableDataModel> variables;

    /**
     * Composite primary key class for EmailTemplate entity.
     * Combines tenant ID and template ID for uniqueness across tenants.
     */
    @Data
    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class EmailTemplateCompositeId {
        private Long tenantId;
        private Long templateId;
    }
}
