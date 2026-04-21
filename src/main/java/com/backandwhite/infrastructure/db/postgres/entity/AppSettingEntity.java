package com.backandwhite.infrastructure.db.postgres.entity;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.*;

/**
 * Generic key-value storage used by cross-cutting concerns (webhook URL hash,
 * kill switches, discovery flags). One row per setting.
 */
@Getter
@Setter
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "app_settings")
public class AppSettingEntity {

    @Id
    @Column(name = "setting_key", length = 120)
    private String key;

    @Column(name = "setting_value", columnDefinition = "text")
    private String value;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
