package com.fxexposure.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Data Transfer Object representing hedge allocation details.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HedgeAllocationResponseDto {

    private Long id;
    private String allocationReference;
    private Long exposureId;
    private String exposureReference;
    private Long derivativeId;
    private String derivativeReference;
    private BigDecimal allocatedAmount;
    private LocalDate allocationDate;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

