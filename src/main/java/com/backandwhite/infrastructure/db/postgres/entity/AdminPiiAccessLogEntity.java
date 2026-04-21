package com.backandwhite.infrastructure.db.postgres.entity;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.*;

/**
 * Compliance audit — whenever an admin views or exports customer PII (address,
 * phone, email, order details), a row is written here. Kept 3 years (Fase 15).
 */
@Getter
@Setter
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "admin_pii_access_log")
public class AdminPiiAccessLogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "at", nullable = false)
    private Instant at;

    @Column(name = "admin_user_id", nullable = false, length = 120)
    private String adminUserId;

    @Column(name = "customer_id", length = 120)
    private String customerId;

    @Column(name = "order_id", length = 64)
    private String orderId;

    /** VIEW_ADDRESS | VIEW_PHONE | VIEW_EMAIL | EXPORT | DELETE_REQUEST | … */
    @Column(name = "action", nullable = false, length = 60)
    private String action;

    @Column(name = "reason", columnDefinition = "text")
    private String reason;

    @Column(name = "source_ip", length = 45)
    private String sourceIp;
}
