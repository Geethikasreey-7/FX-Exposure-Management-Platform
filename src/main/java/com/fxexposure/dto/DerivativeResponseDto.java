package com.fxexposure.dto;

import com.fxexposure.entity.DerivativeStatus;
import com.fxexposure.entity.DerivativeType;
import com.fxexposure.entity.OptionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Data Transfer Object for returning FX derivative contract details.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DerivativeResponseDto {

    private Long id;
    private String derivativeReference;
    private DerivativeType derivativeType;
    private Long exposureId;
    private String exposureReference;
    private String baseCurrency;
    private String quoteCurrency;
    private BigDecimal notionalAmount;
    private LocalDate tradeDate;
    private LocalDate maturityDate;
    private BigDecimal spotRate;
    private BigDecimal forwardRate;
    private BigDecimal strikeRate;
    private OptionType optionType;
    private DerivativeStatus status;
    private LocalDate settlementDate;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

