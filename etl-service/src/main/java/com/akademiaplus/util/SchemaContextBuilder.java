/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.util;

import com.akademiaplus.domain.MigrationEntityType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Builds human-readable schema descriptions for Claude API prompt context.
 *
 * <p>Formats entity field definitions into concise text that helps Claude
 * understand the target schema for column mapping suggestions.</p>
 */
@Component
public class SchemaContextBuilder {

    private final EntityFieldRegistry fieldRegistry;

    /**
     * Creates a new SchemaContextBuilder.
     *
     * @param fieldRegistry the entity field registry
     */
    public SchemaContextBuilder(EntityFieldRegistry fieldRegistry) {
        this.fieldRegistry = fieldRegistry;
    }

    /**
     * Builds schema context for Claude prompt.
     *
     * <p>When entityType is provided, includes only that entity's schema.
     * When null, includes all entity schemas for auto-detection.</p>
     *
     * @param entityType the entity type hint (null for auto-detect)
     * @return formatted schema text for Claude context
     */
    public String buildContext(MigrationEntityType entityType) {
        if (entityType != null) {
            return formatEntitySchema(entityType, fieldRegistry.getFields(entityType));
        }
        return buildFullContext();
    }

    private String buildFullContext() {
        StringBuilder sb = new StringBuilder();
        Map<MigrationEntityType, List<EntityFieldDefinition>> allFields = fieldRegistry.getAllFields();

        for (Map.Entry<MigrationEntityType, List<EntityFieldDefinition>> entry : allFields.entrySet()) {
            if (!sb.isEmpty()) {
                sb.append("\n\n");
            }
            sb.append(formatEntitySchema(entry.getKey(), entry.getValue()));
        }
        return sb.toString();
    }

    private String formatEntitySchema(MigrationEntityType entityType, List<EntityFieldDefinition> fields) {
        StringBuilder sb = new StringBuilder();
        sb.append("Entity: ").append(entityType.name()).append("\nFields:\n");

        for (EntityFieldDefinition field : fields) {
            sb.append("  - ").append(field.name())
                    .append(" (").append(field.type())
                    .append(", ").append(field.required() ? "required" : "optional")
                    .append("): ").append(field.description())
                    .append("\n");
        }
        return sb.toString();
    }
}
