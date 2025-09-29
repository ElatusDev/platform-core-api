/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.billing.store;

import com.akademiaplus.infra.TenantScoped;
import com.akademiaplus.users.employee.EmployeeDataModel;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

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
@Table(name = "store_transaction")
@IdClass(StoreTransactionDataModel.StoreTransactionCompositeId.class)
public class StoreTransactionDataModel extends TenantScoped {

    /**
     * Unique identifier for the transaction within the tenant.
     * Auto-incremented per tenant for better performance and serves as part of the composite key.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "transaction_id")
    private Integer transactionId;

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
    @Column(name = "total_amount", nullable = false)
    private Double totalAmount;

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
    @JoinColumn(name = "tenant_id", referencedColumnName = "tenant_id")
    @JoinColumn(name = "employee_id", referencedColumnName = "employee_id")
    private EmployeeDataModel employee;

    /**
     * List of individual sale items that comprise this transaction.
     * Uses tenant-aware mapping to maintain data isolation.
     */
    @OneToMany(mappedBy = "transaction", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private List<SaleItemDataModel> saleItems;

    /**
     * Composite primary key class for StoreTransaction entity.
     * Combines tenant ID and transaction ID for uniqueness across tenants.
     */
    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class StoreTransactionCompositeId {

        private Integer tenantId;
        private Integer transactionId;

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof StoreTransactionCompositeId that)) return false;
            return tenantId.equals(that.tenantId) &&
                    transactionId.equals(that.transactionId);
        }

        @Override
        public int hashCode() {
            return java.util.Objects.hash(tenantId, transactionId);
        }

        @Override
        public String toString() {
            return getClass().getSimpleName() +
                    "{tenantId=" + tenantId +
                    ", transactionId=" + transactionId + "}";
        }
    }
}