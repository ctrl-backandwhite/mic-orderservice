package com.backandwhite.infrastructure.db.postgres.entity;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * One row per outbound HTTP call to CJ Dropshipping. Used to reconstruct
 * exactly what was sent and what CJ answered weeks after the fact (Fase 8.1).
 */
@Getter
@Setter
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "cj_api_audit_log")
public class CjApiAuditLogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "order_id", length = 64)
    private String orderId;

    @Column(name = "endpoint", nullable = false, length = 200)
    private String endpoint;

    @Column(name = "http_status")
    private Integer httpStatus;

    @Column(name = "cj_code", length = 20)
    private String cjCode;

    @Column(name = "cj_request_id", length = 120)
    private String cjRequestId;

    @Column(name = "latency_ms")
    private Integer latencyMs;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "request_body", columnDefinition = "jsonb")
    private String requestBody;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "response_body", columnDefinition = "jsonb")
    private String responseBody;

    @Column(name = "error_message", columnDefinition = "text")
    private String errorMessage;

    @Column(name = "retry_attempt")
    private Short retryAttempt;
}
