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
 * Entity representing individual line items within a store transaction in the multi-tenant platform.
 * Each sale item captures the details of a specific product sold as part of a transaction,
 * including quantity, pricing at the time of sale, and calculated totals.
 * <p>
 * This entity maintains historical pricing data by storing the unit price at the time
 * of sale, which may differ from the current product price due to discounts, promotions,
 * or price changes over time.
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Scope("prototype")
@Component
@Entity
@Table(name = "store_sale_item")
@IdClass(SaleItemDataModel.SaleItemCompositeId.class)
public class SaleItemDataModel extends TenantScoped {

    /**
     * Unique identifier for the sale item within the tenant.
     * Auto-incremented per tenant for better performance and serves as part of the composite key.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "sale_item_id")
    private Integer saleItemId;

    /**
     * Reference to the parent store transaction containing this sale item.
     * Uses tenant-aware join to maintain data isolation across tenants.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", referencedColumnName = "tenant_id")
    @JoinColumn(name = "transaction_id", referencedColumnName = "transaction_id")
    private StoreTransactionDataModel transaction;

    /**
     * Reference to the product being sold in this line item.
     * Uses tenant-aware join to ensure products belong to the same tenant.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", referencedColumnName = "tenant_id")
    @JoinColumn(name = "product_id", referencedColumnName = "product_id")
    private ProductDataModel product;

    /**
     * Number of units of the product being purchased.
     * Must be positive for sales transactions.
     */
    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    /**
     * Unit price of the product at the time of this sale.
     * Preserved for historical accuracy and may differ from current product price
     * due to discounts, promotions, or price changes.
     */
    @Column(name = "unit_price_at_sale", nullable = false)
    private Double unitPriceAtSale;

    /**
     * Total amount for this line item (quantity × unit_price_at_sale).
     * Calculated and stored for performance and audit purposes.
     */
    @Column(name = "item_total", nullable = false)
    private Double itemTotal;

    /**
     * Composite primary key class for SaleItem entity.
     * Combines tenant ID and sale item ID for uniqueness across tenants.
     */
    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class SaleItemCompositeId {

        private Integer tenantId;
        private Integer saleItemId;

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof SaleItemCompositeId that)) return false;
            return tenantId.equals(that.tenantId) &&
                    saleItemId.equals(that.saleItemId);
        }

        @Override
        public int hashCode() {
            return java.util.Objects.hash(tenantId, saleItemId);
        }

        @Override
        public String toString() {
            return getClass().getSimpleName() +
                    "{tenantId=" + tenantId +
                    ", saleItemId=" + saleItemId + "}";
        }
    }
}