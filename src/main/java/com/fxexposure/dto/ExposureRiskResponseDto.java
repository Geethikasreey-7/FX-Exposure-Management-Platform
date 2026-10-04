package com.fxexposure.dto;

import com.fxexposure.entity.ExposureStatus;
import com.fxexposure.entity.ExposureType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Data Transfer Object representing individual exposure-level risk metrics and hedging state.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExposureRiskResponseDto {

    private Long exposureId;
    private String exposureReference;
    private ExposureType exposureType;
    private String currency;
    private BigDecimal exposureAmount;
    private BigDecimal hedgedAmount;
    private BigDecimal unhedgedAmount;
    private BigDecimal hedgeRatio;
    private ExposureStatus status;
}

