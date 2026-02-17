/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.billing.store;

 import com.akademiaplus.infra.persistence.model.TenantScoped;
import com.akademiaplus.users.employee.EmployeeDataModel;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.SQLDelete;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Entity representing store transactions in the multi-tenant platform.
 * Captures the complete details of a sales transaction including payment information,
 * transaction type, and the employee who processed the transaction.
 * <p>
 * Each transaction serves as the parent record for multiple sale items and maintains
 * audit information about when and how the transaction was processed.
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Scope("prototype")
@Component
@Entity
@Table(name = "store_transactions")
@SQLDelete(sql = "UPDATE store_transactions SET deleted_at = CURRENT_TIMESTAMP WHERE tenant_id = ?")
@IdClass(StoreTransactionDataModel.StoreTransactionCompositeId.class)
public class StoreTransactionDataModel extends TenantScoped {

    /**
     * Unique identifier for the transaction within the tenant.
     * Auto-incremented per tenant for better performance and serves as part of the composite key.
     */
    @Id
    @Column(name = "store_transaction_id")
    private Long storeTransactionId;

    /**
     * Timestamp when the transaction was created.
     * Automatically set on creation and cannot be updated for audit integrity.
     */
    @CreationTimestamp
    @Column(name = "transaction_datetime", nullable = false, updatable = false)
    private LocalDateTime transactionDatetime;

    /**
     * Type of transaction (e.g., SALE, REFUND, EXCHANGE, VOID).
     * Determines business logic for processing and affects inventory management.
     */
    @Column(name = "transaction_type", nullable = false, length = 30)
    private String transactionType;

    /**
     * Total monetary amount for the entire transaction.
     * Sum of all sale item totals, including taxes and discounts if applicable.
     */
    @Column(name = "total_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount;

    /**
     * Method used for payment (e.g., CASH, CREDIT_CARD, DEBIT_CARD, CHECK, DIGITAL_WALLET).
     * Important for reconciliation and financial reporting.
     */
    @Column(name = "payment_method", nullable = false, length = 50)
    private String paymentMethod;

    /**
     * Reference to the employee who processed this transaction.
     * Uses tenant-aware join to ensure employee belongs to the same tenant.
     * Optional field for transactions that may be processed automatically or by customers.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "tenant_id", referencedColumnName = "tenant_id", insertable=false, updatable=false)
    @JoinColumn(name = "employee_id", referencedColumnName = "employee_id", insertable=false, updatable=false)
    private EmployeeDataModel employee;

    /**
     * List of individual sale items that comprise this transaction.
     * Uses tenant-aware mapping to maintain data isolation.
     */
    @OneToMany(mappedBy = "transaction", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private List<StoreSaleItemDataModel> saleItems;

    /**
     * Composite primary key class for StoreTransaction entity.
     * Combines tenant ID and transaction ID for uniqueness across tenants.
     */
    @Data
    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class StoreTransactionCompositeId {
        private Long tenantId;
        private Long storeTransactionId;
    }
}