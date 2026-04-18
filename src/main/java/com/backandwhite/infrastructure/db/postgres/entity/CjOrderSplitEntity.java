package com.backandwhite.infrastructure.db.postgres.entity;

import com.backandwhite.common.infrastructure.entity.AuditableEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Records a CJ order-split event: when CJ splits one parent order into multiple
 * child orders (different warehouses / fulfilment centres).
 */
@Getter
@Setter
@Entity
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "cj_order_splits")
public class CjOrderSplitEntity extends AuditableEntity {

    @Id
    @Column(length = 64)
    private String id;

    /** Internal order UUID from our system. */
    @Column(name = "order_id", nullable = false, length = 64)
    private String orderId;

    /** The original CJ order ID that was split. */
    @Column(name = "original_cj_order_id", nullable = false, length = 100)
    private String originalCjOrderId;

    /** The new child CJ order ID created by the split. */
    @Column(name = "split_cj_order_id", length = 100)
    private String splitCjOrderId;

    /** Status of the split child order. */
    @Column(name = "order_status", length = 50)
    private String orderStatus;

    /** Product list for this sub-order, stored as JSON text from CJ. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "product_list", columnDefinition = "jsonb")
    private Object productList;
}
