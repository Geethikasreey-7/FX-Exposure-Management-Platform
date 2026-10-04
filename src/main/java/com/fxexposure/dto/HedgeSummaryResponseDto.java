package com.fxexposure.dto;

import com.fxexposure.entity.ExposureStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Data Transfer Object summarizing hedging metrics and status for an FX exposure.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HedgeSummaryResponseDto {

    private Long exposureId;
    private String exposureReference;
    private BigDecimal exposureAmount;
    private BigDecimal totalHedgedAmount;
    private BigDecimal unhedgedAmount;
    private BigDecimal hedgeRatio;
    private ExposureStatus exposureStatus;
}

