package com.backandwhite.infrastructure.db.postgres.entity;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.*;

/**
 * Whitelist of destination countries CJ Dropshipping is currently shipping to.
 * Checkout rejects orders whose country is not in this table (Fase 3).
 */
@Getter
@Setter
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "cj_allowed_countries")
public class CjAllowedCountryEntity {

    @Id
    @Column(name = "country_code", length = 2)
    private String countryCode;

    @Column(name = "country_name", length = 80, nullable = false)
    private String countryName;

    @Column(name = "active", nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
