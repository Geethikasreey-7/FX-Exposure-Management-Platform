package com.fxexposure.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Entity representing an FX Exposure in the PostgreSQL database.
 */
@Entity
@Table(
    name = "exposures",
    indexes = {
        @Index(name = "idx_exposure_ref", columnList = "exposure_reference", unique = true),
        @Index(name = "idx_exposure_currency", columnList = "currency"),
        @Index(name = "idx_exposure_status", columnList = "status"),
        @Index(name = "idx_exposure_type", columnList = "exposure_type"),
        @Index(name = "idx_exposure_dates", columnList = "exposure_date, maturity_date")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Exposure {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "exposure_reference", nullable = false, unique = true, length = 50)
    private String exposureReference;

    @Enumerated(EnumType.STRING)
    @Column(name = "exposure_type", nullable = false, length = 30)
    private ExposureType exposureType;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(name = "base_currency", nullable = false, length = 3)
    private String baseCurrency;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(name = "exposure_date", nullable = false)
    private LocalDate exposureDate;

    @Column(name = "maturity_date", nullable = false)
    private LocalDate maturityDate;

    @Column(length = 255)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private ExposureStatus status = ExposureStatus.OPEN;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.status == null) {
            this.status = ExposureStatus.OPEN;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
