package com.fxexposure.dto;

import com.fxexposure.entity.DerivativeStatus;
import com.fxexposure.entity.DerivativeType;
import com.fxexposure.entity.OptionType;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Data Transfer Object for creating or updating an FX derivative contract.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DerivativeRequestDto {

    @NotBlank(message = "Derivative reference cannot be blank")
    @Size(min = 3, max = 50, message = "Derivative reference must be between 3 and 50 characters")
    private String derivativeReference;

    @NotNull(message = "Derivative type is required (SPOT, FORWARD, SWAP, OPTION)")
    private DerivativeType derivativeType;

    @NotNull(message = "Exposure ID is required")
    private Long exposureId;

    @NotBlank(message = "Base currency cannot be blank")
    @Size(min = 3, max = 3, message = "Base currency must be a 3-letter ISO currency code")
    private String baseCurrency;

    @NotBlank(message = "Quote currency cannot be blank")
    @Size(min = 3, max = 3, message = "Quote currency must be a 3-letter ISO currency code")
    private String quoteCurrency;

    @NotNull(message = "Notional amount is required")
    @DecimalMin(value = "0.01", message = "Notional amount must be greater than zero")
    private BigDecimal notionalAmount;

    @NotNull(message = "Trade date is required")
    private LocalDate tradeDate;

    @NotNull(message = "Maturity date is required")
    private LocalDate maturityDate;

    private BigDecimal spotRate;

    private BigDecimal forwardRate;

    private BigDecimal strikeRate;

    private OptionType optionType;

    private DerivativeStatus status;

    private LocalDate settlementDate;

    @Size(max = 255, message = "Description must not exceed 255 characters")
    private String description;

    @AssertTrue(message = "Maturity date cannot be before trade date")
    public boolean isMaturityDateValid() {
        if (tradeDate == null || maturityDate == null) {
            return true;
        }
        return !maturityDate.isBefore(tradeDate);
    }
}

