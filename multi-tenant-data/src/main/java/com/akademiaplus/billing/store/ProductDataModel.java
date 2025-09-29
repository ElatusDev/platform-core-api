/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.billing.store;

import com.akademiaplus.infra.TenantScoped;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * Entity representing products available in the store within the multi-tenant platform.
 * Each product is managed independently per tenant, allowing different organizations
 * to maintain their own product catalogs with unique pricing, inventory, and details.
 * <p>
 * Products can include physical items, digital resources, course materials,
 * or any sellable items relevant to the educational institution's operations.
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Scope("prototype")
@Component
@Entity
@Table(name = "store_product")
@IdClass(ProductDataModel.ProductCompositeId.class)
public class ProductDataModel extends TenantScoped {

    /**
     * Unique identifier for the product within the tenant.
     * Auto-incremented per tenant for better performance and serves as part of the composite key.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "product_id")
    private Integer productId;

    /**
     * Display name of the product.
     * Must be unique within the tenant to avoid confusion in the catalog.
     */
    @Column(name = "product_name", nullable = false, length = 100)
    private String name;

    /**
     * Detailed description of the product including features, specifications,
     * or any relevant information for customers making purchase decisions.
     */
    @Column(name = "description", length = 500)
    private String description;

    /**
     * Current selling price of the product.
     * Stored as Double for flexibility, but should be handled with proper
     * currency precision in business logic and display layers.
     */
    @Column(name = "price", nullable = false)
    private Double price;

    /**
     * Current available quantity in stock for this product.
     * Used for inventory management and preventing overselling.
     * Zero or negative values may indicate out-of-stock status.
     */
    @Column(name = "stock_quantity", nullable = false)
    private Integer stockQuantity;

    /**
     * Composite primary key class for Product entity.
     * Combines tenant ID and product ID for uniqueness across tenants.
     */
    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ProductCompositeId {

        private Integer tenantId;
        private Integer productId;

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof ProductCompositeId that)) return false;
            return tenantId.equals(that.tenantId) &&
                    productId.equals(that.productId);
        }

        @Override
        public int hashCode() {
            return java.util.Objects.hash(tenantId, productId);
        }

        @Override
        public String toString() {
            return getClass().getSimpleName() +
                    "{tenantId=" + tenantId +
                    ", productId=" + productId + "}";
        }
    }
}