package com.fxexposure.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Data Transfer Object representing currency-level FX exposure and hedging metrics.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CurrencyRiskResponseDto {

    private String currency;
    private BigDecimal totalExposureAmount;
    private BigDecimal totalHedgedAmount;
    private BigDecimal totalUnhedgedAmount;
    private BigDecimal hedgeRatio;
    private long exposureCount;
}

