package com.akademiaplus.utilities.idgeneration.interfaceadapters;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Sequence tracking entity for multi-tenant ID generation.
 * <p>
 * IMPORTANT: This entity does NOT extend TenantScoped as it manages
 * sequences for ALL tenants and must not be filtered by tenant context.
 */
@Entity
@Table(name = "tenant_sequences")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TenantSequence {

    @EmbeddedId
    private TenantSequenceId id;

    @Column(name = "next_value", nullable = false)
    @Builder.Default
    private Long nextValue = 1L;

    @Version
    @Column(name = "version", nullable = false)
    @Builder.Default
    private Integer version = 0;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * Composite primary key for tenant_sequences table.
     */
    @Embeddable
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @EqualsAndHashCode
    public static class TenantSequenceId implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        @Column(name = "tenant_id", nullable = false)
        private Integer tenantId;

        @Column(name = "entity_name", nullable = false, length = 50)
        private String entityName;
    }
}