package com.fxexposure.dto;

import com.fxexposure.entity.ExposureStatus;
import com.fxexposure.entity.ExposureType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Data Transfer Object for returning FX exposure details.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExposureResponseDto {

    private Long id;
    private String exposureReference;
    private ExposureType exposureType;
    private String currency;
    private String baseCurrency;
    private BigDecimal amount;
    private LocalDate exposureDate;
    private LocalDate maturityDate;
    private String description;
    private ExposureStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
