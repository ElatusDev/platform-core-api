/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.notifications;

import com.akademiaplus.infra.persistence.model.Auditable;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * Platform-level entity representing a registered push notification device.
 * <p>
 * This entity is NOT tenant-scoped — device tokens belong to users across tenants.
 * Uses a single {@code Long} auto-increment primary key.
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
@Table(name = "push_devices")
public class PushDeviceDataModel extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "push_device_id")
    private Long pushDeviceId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "device_token", nullable = false, unique = true)
    private String deviceToken;

    @Column(name = "platform", nullable = false, length = 10)
    private String platform;

    @Column(name = "app_version", length = 20)
    private String appVersion;
}
