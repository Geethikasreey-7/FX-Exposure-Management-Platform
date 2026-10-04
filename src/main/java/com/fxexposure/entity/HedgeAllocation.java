package com.fxexposure.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
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
 * Entity representing an allocation of an FX Derivative to an FX Exposure.
 */
@Entity
@Table(
    name = "hedge_allocations",
    indexes = {
        @Index(name = "idx_hedge_alloc_ref", columnList = "allocation_reference", unique = true),
        @Index(name = "idx_hedge_alloc_exposure", columnList = "exposure_id"),
        @Index(name = "idx_hedge_alloc_derivative", columnList = "derivative_id"),
        @Index(name = "idx_hedge_alloc_date", columnList = "allocation_date")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HedgeAllocation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "allocation_reference", nullable = false, unique = true, length = 50)
    private String allocationReference;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exposure_id", nullable = false)
    private Exposure exposure;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "derivative_id", nullable = false)
    private Derivative derivative;

    @Column(name = "allocated_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal allocatedAmount;

    @Column(name = "allocation_date", nullable = false)
    private LocalDate allocationDate;

    @Column(length = 255)
    private String description;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}

