package com.backandwhite.infrastructure.db.postgres.entity;

import com.backandwhite.common.infrastructure.entity.AuditableEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.Instant;

@With
@Getter
@Setter
@Entity
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "tracking_events")
public class TrackingEventEntity extends AuditableEntity {

    @Id
    @Column(length = 64)
    private String id;

    @Column(name = "order_id", nullable = false, length = 64)
    private String orderId;

    @Column(nullable = false, length = 50)
    private String status;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(length = 200)
    private String location;

    @Column(length = 100)
    private String carrier;

    @Column(name = "event_at", nullable = false)
    private Instant eventAt;
}
