package com.fxexposure.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
 * Entity representing an FX Derivative contract in the PostgreSQL database.
 */
@Entity
@Table(
    name = "derivatives",
    indexes = {
        @Index(name = "idx_derivative_ref", columnList = "derivative_reference", unique = true),
        @Index(name = "idx_derivative_type", columnList = "derivative_type"),
        @Index(name = "idx_derivative_status", columnList = "status"),
        @Index(name = "idx_derivative_currency_pair", columnList = "base_currency, quote_currency"),
        @Index(name = "idx_derivative_maturity", columnList = "maturity_date"),
        @Index(name = "idx_derivative_exposure", columnList = "exposure_id")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Derivative {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "derivative_reference", nullable = false, unique = true, length = 50)
    private String derivativeReference;

    @Enumerated(EnumType.STRING)
    @Column(name = "derivative_type", nullable = false, length = 30)
    private DerivativeType derivativeType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exposure_id", nullable = false)
    private Exposure exposure;

    @Column(name = "base_currency", nullable = false, length = 3)
    private String baseCurrency;

    @Column(name = "quote_currency", nullable = false, length = 3)
    private String quoteCurrency;

    @Column(name = "notional_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal notionalAmount;

    @Column(name = "trade_date", nullable = false)
    private LocalDate tradeDate;

    @Column(name = "maturity_date", nullable = false)
    private LocalDate maturityDate;

    @Column(name = "spot_rate", precision = 19, scale = 6)
    private BigDecimal spotRate;

    @Column(name = "forward_rate", precision = 19, scale = 6)
    private BigDecimal forwardRate;

    @Column(name = "strike_rate", precision = 19, scale = 6)
    private BigDecimal strikeRate;

    @Enumerated(EnumType.STRING)
    @Column(name = "option_type", length = 10)
    private OptionType optionType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private DerivativeStatus status = DerivativeStatus.ACTIVE;

    @Column(name = "settlement_date")
    private LocalDate settlementDate;

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
        if (this.status == null) {
            this.status = DerivativeStatus.ACTIVE;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}

