/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.leadmanagement;

import com.akademiaplus.infra.persistence.model.SoftDeletable;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * Platform-level entity representing a product demo request from a prospective customer.
 * <p>
 * This entity is NOT tenant-scoped — it captures leads before any tenant context exists.
 * Uses a single {@code Long} auto-increment primary key with soft delete support.
 *
 * @author ElatusDev
 * @since 1.0
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Scope("prototype")
@Component
@Entity
@Table(name = "demo_requests")
@SQLDelete(sql = "UPDATE demo_requests SET deleted_at = CURRENT_TIMESTAMP WHERE demo_request_id = ?")
@SQLRestriction("deleted_at IS NULL")
public class DemoRequestDataModel extends SoftDeletable {

    /** Unique identifier for the demo request. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "demo_request_id")
    private Long demoRequestId;

    /** First name of the person requesting a demo. */
    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    /** Last name of the person requesting a demo. */
    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    /** Email address of the person requesting a demo. Must be unique. */
    @Column(name = "email", nullable = false, unique = true)
    private String email;

    /** Name of the company or organization the person represents. */
    @Column(name = "company_name", nullable = false, length = 200)
    private String companyName;

    /** Optional message or additional context from the requester. */
    @Column(name = "message", columnDefinition = "TEXT")
    private String message;

    /** Current status of the demo request (e.g., PENDING, CONTACTED, SCHEDULED). */
    @Column(name = "status", nullable = false, length = 20)
    private String status;
}
