package com.fxexposure.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Data Transfer Object representing portfolio-level FX risk summary and hedging metrics.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RiskSummaryResponseDto {

    private BigDecimal totalExposureAmount;
    private BigDecimal totalHedgedAmount;
    private BigDecimal totalUnhedgedAmount;
    private BigDecimal overallHedgeRatio;
    private long totalExposureCount;
    private long totalHedgedExposureCount;
    private long totalPartiallyHedgedExposureCount;
    private long totalOpenExposureCount;
    private long totalFullyHedgedExposureCount;
}

