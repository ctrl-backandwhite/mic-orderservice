package com.backandwhite.infrastructure.db.postgres.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * One row per financial event tied to an order: customer payment in (INBOUND),
 * CJ payout (OUTBOUND), refund to customer (REFUND). Lets us reconcile every
 * dollar months later without re-reading Stripe/PayPal/CJ state (Fase 5 + 8).
 */
@Getter
@Setter
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "order_financial_ledger")
public class OrderFinancialLedgerEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "order_id", nullable = false, length = 64)
    private String orderId;

    /** INBOUND | OUTBOUND | REFUND */
    @Column(name = "entry_type", nullable = false, length = 20)
    private String entryType;

    @Column(name = "amount", nullable = false, precision = 18, scale = 4)
    private BigDecimal amount;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Column(name = "fx_rate", precision = 18, scale = 6)
    private BigDecimal fxRate;

    @Column(name = "provider", length = 40)
    private String provider;

    @Column(name = "external_tx_id", length = 120)
    private String externalTxId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", columnDefinition = "jsonb")
    private String metadata;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
