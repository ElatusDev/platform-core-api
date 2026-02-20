/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.billing.store;

 import com.akademiaplus.infra.persistence.model.TenantScoped;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

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
@Table(name = "store_sale_items")
@SQLDelete(sql = "UPDATE store_sale_items SET deleted_at = CURRENT_TIMESTAMP WHERE tenant_id = ?")
@IdClass(StoreSaleItemDataModel.SaleItemCompositeId.class)
public class StoreSaleItemDataModel extends TenantScoped {

    /**
     * Unique identifier for the sale item within the tenant.
     * Auto-incremented per tenant for better performance and serves as part of the composite key.
     */
    @Id
    @Column(name = "store_sale_item_id")
    private Long storeSaleItemId;

    /**
     * Foreign key to the parent store transaction.
     * Insertable column used by the mock-data pipeline to set the FK value.
     */
    @Column(name = "store_transaction_id", nullable = false)
    private Long storeTransactionId;

    /**
     * Foreign key to the product being sold.
     * Insertable column used by the mock-data pipeline to set the FK value.
     */
    @Column(name = "store_product_id", nullable = false)
    private Long storeProductId;

    /**
     * Reference to the parent store transaction containing this sale item.
     * Uses tenant-aware join to maintain data isolation across tenants.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", referencedColumnName = "tenant_id", insertable=false, updatable=false)
    @JoinColumn(name = "store_transaction_id", referencedColumnName = "store_transaction_id", insertable=false, updatable=false)
    private StoreTransactionDataModel transaction;

    /**
     * Reference to the product being sold in this line item.
     * Uses tenant-aware join to ensure products belong to the same tenant.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", referencedColumnName = "tenant_id", insertable=false, updatable=false)
    @JoinColumn(name = "store_product_id", referencedColumnName = "store_product_id", insertable=false, updatable=false)
    private StoreProductDataModel product;

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
    @Column(name = "unit_price_at_sale", nullable = false, precision = 10, scale = 2)
    private BigDecimal unitPriceAtSale;

    /**
     * Total amount for this line item (quantity × unit_price_at_sale).
     * Calculated and stored for performance and audit purposes.
     */
    @Column(name = "item_total", nullable = false, precision = 10, scale = 2)
    private BigDecimal itemTotal;

    /**
     * Composite primary key class for SaleItem entity.
     * Combines tenant ID and sale item ID for uniqueness across tenants.
     */
    @Data
    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class SaleItemCompositeId {
        private Long tenantId;
        private Long storeSaleItemId;
    }
}