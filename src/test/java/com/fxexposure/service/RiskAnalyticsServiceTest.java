package com.fxexposure.service;

import com.fxexposure.dto.CurrencyRiskResponseDto;
import com.fxexposure.dto.ExposureRiskResponseDto;
import com.fxexposure.dto.RiskSummaryResponseDto;
import com.fxexposure.entity.Exposure;
import com.fxexposure.entity.ExposureStatus;
import com.fxexposure.entity.ExposureType;
import com.fxexposure.exception.ResourceNotFoundException;
import com.fxexposure.repository.ExposureRepository;
import com.fxexposure.repository.HedgeAllocationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RiskAnalyticsServiceTest {

    @Mock
    private ExposureRepository exposureRepository;

    @Mock
    private HedgeAllocationRepository hedgeAllocationRepository;

    @InjectMocks
    private RiskAnalyticsService riskAnalyticsService;

    private Exposure exposureUSD;
    private Exposure exposureEUR;
    private Exposure exposureJPY;

    @BeforeEach
    void setUp() {
        exposureUSD = Exposure.builder()
                .id(1L)
                .exposureReference("EXP-USD-001")
                .exposureType(ExposureType.RECEIVABLE)
                .currency("USD")
                .baseCurrency("INR")
                .amount(new BigDecimal("1000000.00"))
                .exposureDate(LocalDate.of(2026, 10, 4))
                .maturityDate(LocalDate.of(2027, 1, 4))
                .status(ExposureStatus.PARTIALLY_HEDGED)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        exposureEUR = Exposure.builder()
                .id(2L)
                .exposureReference("EXP-EUR-001")
                .exposureType(ExposureType.PAYABLE)
                .currency("EUR")
                .baseCurrency("INR")
                .amount(new BigDecimal("500000.00"))
                .exposureDate(LocalDate.of(2026, 10, 4))
                .maturityDate(LocalDate.of(2027, 1, 4))
                .status(ExposureStatus.FULLY_HEDGED)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        exposureJPY = Exposure.builder()
                .id(3L)
                .exposureReference("EXP-JPY-001")
                .exposureType(ExposureType.FORECAST)
                .currency("JPY")
                .baseCurrency("INR")
                .amount(new BigDecimal("200000.00"))
                .exposureDate(LocalDate.of(2026, 10, 4))
                .maturityDate(LocalDate.of(2027, 1, 4))
                .status(ExposureStatus.OPEN)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("1. Overall summary with multiple exposures")
    void getOverallRiskSummary_MultipleExposures() {
        when(exposureRepository.findAll()).thenReturn(List.of(exposureUSD, exposureEUR, exposureJPY));
        List<Object[]> allocationsGrouped = new ArrayList<>();
        allocationsGrouped.add(new Object[]{1L, new BigDecimal("700000.00")});
        allocationsGrouped.add(new Object[]{2L, new BigDecimal("500000.00")});
        when(hedgeAllocationRepository.sumAllocationsGroupedByExposure()).thenReturn(allocationsGrouped);

        RiskSummaryResponseDto summary = riskAnalyticsService.getOverallRiskSummary();

        assertThat(summary).isNotNull();
        // Total exposure: 1,000,000 + 500,000 + 200,000 = 1,700,000
        assertThat(summary.getTotalExposureAmount()).isEqualByComparingTo(new BigDecimal("1700000.00"));
        // Total hedged: 700,000 + 500,000 = 1,200,000
        assertThat(summary.getTotalHedgedAmount()).isEqualByComparingTo(new BigDecimal("1200000.00"));
        // Total unhedged: 1,700,000 - 1,200,000 = 500,000
        assertThat(summary.getTotalUnhedgedAmount()).isEqualByComparingTo(new BigDecimal("500000.00"));
        // Overall hedge ratio: 1,200,000 / 1,700,000 * 100 = 70.59%
        assertThat(summary.getOverallHedgeRatio()).isEqualByComparingTo(new BigDecimal("70.59"));
        assertThat(summary.getTotalExposureCount()).isEqualTo(3);
        assertThat(summary.getTotalHedgedExposureCount()).isEqualTo(2);
        assertThat(summary.getTotalPartiallyHedgedExposureCount()).isEqualTo(1);
        assertThat(summary.getTotalFullyHedgedExposureCount()).isEqualTo(1);
        assertThat(summary.getTotalOpenExposureCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("2. Overall summary with no exposures")
    void getOverallRiskSummary_NoExposures() {
        when(exposureRepository.findAll()).thenReturn(Collections.emptyList());

        RiskSummaryResponseDto summary = riskAnalyticsService.getOverallRiskSummary();

        assertThat(summary).isNotNull();
        assertThat(summary.getTotalExposureAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(summary.getTotalHedgedAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(summary.getTotalUnhedgedAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(summary.getOverallHedgeRatio()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(summary.getTotalExposureCount()).isEqualTo(0);
        assertThat(summary.getTotalHedgedExposureCount()).isEqualTo(0);
    }

    @Test
    @DisplayName("3. Hedge ratio calculation is accurate to 2 decimal places")
    void getOverallRiskSummary_HedgeRatioCalculation() {
        when(exposureRepository.findAll()).thenReturn(List.of(exposureUSD));
        List<Object[]> allocationsGrouped = Collections.singletonList(new Object[]{1L, new BigDecimal("333333.33")});
        when(hedgeAllocationRepository.sumAllocationsGroupedByExposure()).thenReturn(allocationsGrouped);

        RiskSummaryResponseDto summary = riskAnalyticsService.getOverallRiskSummary();

        // 333,333.33 / 1,000,000.00 * 100 = 33.33%
        assertThat(summary.getOverallHedgeRatio()).isEqualByComparingTo(new BigDecimal("33.33"));
    }

    @Test
    @DisplayName("4. Unhedged amount calculation is exact difference")
    void getOverallRiskSummary_UnhedgedAmountCalculation() {
        when(exposureRepository.findAll()).thenReturn(List.of(exposureUSD));
        List<Object[]> allocationsGrouped = Collections.singletonList(new Object[]{1L, new BigDecimal("750000.00")});
        when(hedgeAllocationRepository.sumAllocationsGroupedByExposure()).thenReturn(allocationsGrouped);

        RiskSummaryResponseDto summary = riskAnalyticsService.getOverallRiskSummary();

        // 1,000,000 - 750,000 = 250,000
        assertThat(summary.getTotalUnhedgedAmount()).isEqualByComparingTo(new BigDecimal("250000.00"));
    }

    @Test
    @DisplayName("5. Zero exposure handling does not cause division by zero")
    void getOverallRiskSummary_ZeroExposureAmount() {
        Exposure zeroExp = Exposure.builder()
                .id(99L)
                .currency("USD")
                .amount(BigDecimal.ZERO)
                .status(ExposureStatus.OPEN)
                .build();

        when(exposureRepository.findAll()).thenReturn(List.of(zeroExp));
        when(hedgeAllocationRepository.sumAllocationsGroupedByExposure()).thenReturn(Collections.emptyList());

        RiskSummaryResponseDto summary = riskAnalyticsService.getOverallRiskSummary();

        assertThat(summary.getTotalExposureAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(summary.getOverallHedgeRatio()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("6. Currency-level aggregation returns correct figures for USD")
    void getRiskByCurrency_Aggregation() {
        when(exposureRepository.findAll()).thenReturn(List.of(exposureUSD));
        List<Object[]> allocationsGrouped = Collections.singletonList(new Object[]{1L, new BigDecimal("700000.00")});
        when(hedgeAllocationRepository.sumAllocationsGroupedByExposure()).thenReturn(allocationsGrouped);

        List<CurrencyRiskResponseDto> result = riskAnalyticsService.getRiskByCurrency();

        assertThat(result).hasSize(1);
        CurrencyRiskResponseDto usdRisk = result.get(0);
        assertThat(usdRisk.getCurrency()).isEqualTo("USD");
        assertThat(usdRisk.getTotalExposureAmount()).isEqualByComparingTo(new BigDecimal("1000000.00"));
        assertThat(usdRisk.getTotalHedgedAmount()).isEqualByComparingTo(new BigDecimal("700000.00"));
        assertThat(usdRisk.getTotalUnhedgedAmount()).isEqualByComparingTo(new BigDecimal("300000.00"));
        assertThat(usdRisk.getHedgeRatio()).isEqualByComparingTo(new BigDecimal("70.00"));
        assertThat(usdRisk.getExposureCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("7. Multiple currencies are properly segregated")
    void getRiskByCurrency_MultipleCurrencies() {
        when(exposureRepository.findAll()).thenReturn(List.of(exposureUSD, exposureEUR));
        List<Object[]> allocationsGrouped = List.of(
                new Object[]{1L, new BigDecimal("600000.00")},
                new Object[]{2L, new BigDecimal("200000.00")}
        );
        when(hedgeAllocationRepository.sumAllocationsGroupedByExposure()).thenReturn(allocationsGrouped);

        List<CurrencyRiskResponseDto> result = riskAnalyticsService.getRiskByCurrency();

        assertThat(result).hasSize(2);
        CurrencyRiskResponseDto usd = result.stream().filter(c -> c.getCurrency().equals("USD")).findFirst().orElseThrow();
        CurrencyRiskResponseDto eur = result.stream().filter(c -> c.getCurrency().equals("EUR")).findFirst().orElseThrow();

        assertThat(usd.getTotalExposureAmount()).isEqualByComparingTo(new BigDecimal("1000000.00"));
        assertThat(usd.getHedgeRatio()).isEqualByComparingTo(new BigDecimal("60.00"));

        assertThat(eur.getTotalExposureAmount()).isEqualByComparingTo(new BigDecimal("500000.00"));
        assertThat(eur.getHedgeRatio()).isEqualByComparingTo(new BigDecimal("40.00"));
    }

    @Test
    @DisplayName("8. Exposure-level analytics returns list for all exposures")
    void getExposureRiskAnalytics_ListAll() {
        when(exposureRepository.findAll()).thenReturn(List.of(exposureUSD, exposureEUR));
        when(hedgeAllocationRepository.sumAllocationsGroupedByExposure()).thenReturn(List.of(
                new Object[]{1L, new BigDecimal("700000.00")},
                new Object[]{2L, new BigDecimal("500000.00")}
        ));

        List<ExposureRiskResponseDto> result = riskAnalyticsService.getExposureRiskAnalytics();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getExposureReference()).isEqualTo("EXP-USD-001");
        assertThat(result.get(0).getHedgedAmount()).isEqualByComparingTo(new BigDecimal("700000.00"));
        assertThat(result.get(1).getExposureReference()).isEqualTo("EXP-EUR-001");
        assertThat(result.get(1).getHedgedAmount()).isEqualByComparingTo(new BigDecimal("500000.00"));
    }

    @Test
    @DisplayName("9. Exposure with no hedge returns zero hedged and 0% ratio")
    void getExposureRiskAnalyticsById_NoHedge() {
        when(exposureRepository.findById(3L)).thenReturn(Optional.of(exposureJPY));
        when(hedgeAllocationRepository.sumAllocatedAmountByExposureId(3L)).thenReturn(BigDecimal.ZERO);

        ExposureRiskResponseDto result = riskAnalyticsService.getExposureRiskAnalyticsById(3L);

        assertThat(result).isNotNull();
        assertThat(result.getHedgedAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(result.getUnhedgedAmount()).isEqualByComparingTo(new BigDecimal("200000.00"));
        assertThat(result.getHedgeRatio()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(result.getStatus()).isEqualTo(ExposureStatus.OPEN);
    }

    @Test
    @DisplayName("10. Fully hedged exposure returns 100% hedge ratio and 0 unhedged")
    void getExposureRiskAnalyticsById_FullyHedged() {
        when(exposureRepository.findById(2L)).thenReturn(Optional.of(exposureEUR));
        when(hedgeAllocationRepository.sumAllocatedAmountByExposureId(2L)).thenReturn(new BigDecimal("500000.00"));

        ExposureRiskResponseDto result = riskAnalyticsService.getExposureRiskAnalyticsById(2L);

        assertThat(result).isNotNull();
        assertThat(result.getHedgedAmount()).isEqualByComparingTo(new BigDecimal("500000.00"));
        assertThat(result.getUnhedgedAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(result.getHedgeRatio()).isEqualByComparingTo(new BigDecimal("100.00"));
        assertThat(result.getStatus()).isEqualTo(ExposureStatus.FULLY_HEDGED);
    }

    @Test
    @DisplayName("11. Partially hedged exposure returns expected intermediate ratio")
    void getExposureRiskAnalyticsById_PartiallyHedged() {
        when(exposureRepository.findById(1L)).thenReturn(Optional.of(exposureUSD));
        when(hedgeAllocationRepository.sumAllocatedAmountByExposureId(1L)).thenReturn(new BigDecimal("500000.00"));

        ExposureRiskResponseDto result = riskAnalyticsService.getExposureRiskAnalyticsById(1L);

        assertThat(result).isNotNull();
        assertThat(result.getHedgedAmount()).isEqualByComparingTo(new BigDecimal("500000.00"));
        assertThat(result.getUnhedgedAmount()).isEqualByComparingTo(new BigDecimal("500000.00"));
        assertThat(result.getHedgeRatio()).isEqualByComparingTo(new BigDecimal("50.00"));
        assertThat(result.getStatus()).isEqualTo(ExposureStatus.PARTIALLY_HEDGED);
    }

    @Test
    @DisplayName("12. Single exposure lookup returns correct DTO")
    void getExposureRiskAnalyticsById_Success() {
        when(exposureRepository.findById(1L)).thenReturn(Optional.of(exposureUSD));
        when(hedgeAllocationRepository.sumAllocatedAmountByExposureId(1L)).thenReturn(new BigDecimal("700000.00"));

        ExposureRiskResponseDto result = riskAnalyticsService.getExposureRiskAnalyticsById(1L);

        assertThat(result).isNotNull();
        assertThat(result.getExposureId()).isEqualTo(1L);
        assertThat(result.getExposureReference()).isEqualTo("EXP-USD-001");
        assertThat(result.getCurrency()).isEqualTo("USD");
        assertThat(result.getExposureAmount()).isEqualByComparingTo(new BigDecimal("1000000.00"));
    }

    @Test
    @DisplayName("13. Missing exposure throws ResourceNotFoundException")
    void getExposureRiskAnalyticsById_NotFound() {
        when(exposureRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> riskAnalyticsService.getExposureRiskAnalyticsById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Exposure not found with id: 99");
    }
}
