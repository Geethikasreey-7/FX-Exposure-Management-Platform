package com.fxexposure.service;

import com.fxexposure.dto.CurrencyRiskResponseDto;
import com.fxexposure.dto.ExposureRiskResponseDto;
import com.fxexposure.dto.RiskSummaryResponseDto;
import com.fxexposure.entity.Exposure;
import com.fxexposure.entity.ExposureStatus;
import com.fxexposure.exception.ResourceNotFoundException;
import com.fxexposure.repository.ExposureRepository;
import com.fxexposure.repository.HedgeAllocationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service calculating portfolio-level and exposure-level foreign exchange risk analytics.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RiskAnalyticsService {

    private final ExposureRepository exposureRepository;
    private final HedgeAllocationRepository hedgeAllocationRepository;

    /**
     * Compute overall portfolio risk summary including totals, counts, and aggregate hedge ratio.
     */
    public RiskSummaryResponseDto getOverallRiskSummary() {
        List<Exposure> exposures = exposureRepository.findAll();

        if (exposures.isEmpty()) {
            return RiskSummaryResponseDto.builder()
                    .totalExposureAmount(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP))
                    .totalHedgedAmount(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP))
                    .totalUnhedgedAmount(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP))
                    .overallHedgeRatio(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP))
                    .totalExposureCount(0)
                    .totalHedgedExposureCount(0)
                    .totalPartiallyHedgedExposureCount(0)
                    .totalOpenExposureCount(0)
                    .totalFullyHedgedExposureCount(0)
                    .build();
        }

        Map<Long, BigDecimal> hedgedMap = getHedgedAmountByExposureMap();

        BigDecimal totalExposure = BigDecimal.ZERO;
        BigDecimal totalHedged = BigDecimal.ZERO;
        long openCount = 0;
        long partialCount = 0;
        long fullyCount = 0;
        long hedgedCount = 0;

        for (Exposure exposure : exposures) {
            totalExposure = totalExposure.add(exposure.getAmount());
            BigDecimal hedged = hedgedMap.getOrDefault(exposure.getId(), BigDecimal.ZERO);
            totalHedged = totalHedged.add(hedged);

            if (exposure.getStatus() == ExposureStatus.OPEN) {
                openCount++;
            } else if (exposure.getStatus() == ExposureStatus.PARTIALLY_HEDGED) {
                partialCount++;
                hedgedCount++;
            } else if (exposure.getStatus() == ExposureStatus.FULLY_HEDGED) {
                fullyCount++;
                hedgedCount++;
            }
        }

        BigDecimal totalUnhedged = totalExposure.subtract(totalHedged);
        if (totalUnhedged.compareTo(BigDecimal.ZERO) < 0) {
            totalUnhedged = BigDecimal.ZERO;
        }

        BigDecimal overallHedgeRatio = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        if (totalExposure.compareTo(BigDecimal.ZERO) > 0) {
            overallHedgeRatio = totalHedged.multiply(BigDecimal.valueOf(100))
                    .divide(totalExposure, 2, RoundingMode.HALF_UP);
        }

        return RiskSummaryResponseDto.builder()
                .totalExposureAmount(totalExposure)
                .totalHedgedAmount(totalHedged)
                .totalUnhedgedAmount(totalUnhedged)
                .overallHedgeRatio(overallHedgeRatio)
                .totalExposureCount(exposures.size())
                .totalHedgedExposureCount(hedgedCount)
                .totalPartiallyHedgedExposureCount(partialCount)
                .totalOpenExposureCount(openCount)
                .totalFullyHedgedExposureCount(fullyCount)
                .build();
    }

    /**
     * Compute risk metrics aggregated by exposure currency.
     */
    public List<CurrencyRiskResponseDto> getRiskByCurrency() {
        List<Exposure> exposures = exposureRepository.findAll();
        if (exposures.isEmpty()) {
            return List.of();
        }

        Map<Long, BigDecimal> hedgedMap = getHedgedAmountByExposureMap();

        Map<String, List<Exposure>> groupedByCurrency = exposures.stream()
                .collect(Collectors.groupingBy(
                        e -> e.getCurrency().trim().toUpperCase(),
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        List<CurrencyRiskResponseDto> result = new ArrayList<>();
        for (Map.Entry<String, List<Exposure>> entry : groupedByCurrency.entrySet()) {
            String currency = entry.getKey();
            List<Exposure> currencyExposures = entry.getValue();

            BigDecimal totalExposure = BigDecimal.ZERO;
            BigDecimal totalHedged = BigDecimal.ZERO;

            for (Exposure exp : currencyExposures) {
                totalExposure = totalExposure.add(exp.getAmount());
                totalHedged = totalHedged.add(hedgedMap.getOrDefault(exp.getId(), BigDecimal.ZERO));
            }

            BigDecimal totalUnhedged = totalExposure.subtract(totalHedged);
            if (totalUnhedged.compareTo(BigDecimal.ZERO) < 0) {
                totalUnhedged = BigDecimal.ZERO;
            }

            BigDecimal hedgeRatio = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
            if (totalExposure.compareTo(BigDecimal.ZERO) > 0) {
                hedgeRatio = totalHedged.multiply(BigDecimal.valueOf(100))
                        .divide(totalExposure, 2, RoundingMode.HALF_UP);
            }

            result.add(CurrencyRiskResponseDto.builder()
                    .currency(currency)
                    .totalExposureAmount(totalExposure)
                    .totalHedgedAmount(totalHedged)
                    .totalUnhedgedAmount(totalUnhedged)
                    .hedgeRatio(hedgeRatio)
                    .exposureCount(currencyExposures.size())
                    .build());
        }

        return result;
    }

    /**
     * Compute risk analytics for all individual exposures.
     */
    public List<ExposureRiskResponseDto> getExposureRiskAnalytics() {
        List<Exposure> exposures = exposureRepository.findAll();
        if (exposures.isEmpty()) {
            return List.of();
        }

        Map<Long, BigDecimal> hedgedMap = getHedgedAmountByExposureMap();

        return exposures.stream()
                .map(exposure -> mapToExposureRiskDto(exposure, hedgedMap.getOrDefault(exposure.getId(), BigDecimal.ZERO)))
                .toList();
    }

    /**
     * Compute risk analytics for a specific exposure by ID.
     */
    public ExposureRiskResponseDto getExposureRiskAnalyticsById(Long exposureId) {
        Exposure exposure = exposureRepository.findById(exposureId)
                .orElseThrow(() -> new ResourceNotFoundException("Exposure not found with id: " + exposureId));

        BigDecimal hedgedAmount = hedgeAllocationRepository.sumAllocatedAmountByExposureId(exposureId);
        return mapToExposureRiskDto(exposure, hedgedAmount);
    }

    /**
     * Internal helper to build an ExposureRiskResponseDto from an Exposure and its hedged amount.
     */
    private ExposureRiskResponseDto mapToExposureRiskDto(Exposure exposure, BigDecimal hedgedAmount) {
        if (hedgedAmount == null) {
            hedgedAmount = BigDecimal.ZERO;
        }

        BigDecimal unhedgedAmount = exposure.getAmount().subtract(hedgedAmount);
        if (unhedgedAmount.compareTo(BigDecimal.ZERO) < 0) {
            unhedgedAmount = BigDecimal.ZERO;
        }

        BigDecimal hedgeRatio = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        if (exposure.getAmount() != null && exposure.getAmount().compareTo(BigDecimal.ZERO) > 0) {
            hedgeRatio = hedgedAmount.multiply(BigDecimal.valueOf(100))
                    .divide(exposure.getAmount(), 2, RoundingMode.HALF_UP);
        }

        return ExposureRiskResponseDto.builder()
                .exposureId(exposure.getId())
                .exposureReference(exposure.getExposureReference())
                .exposureType(exposure.getExposureType())
                .currency(exposure.getCurrency())
                .exposureAmount(exposure.getAmount())
                .hedgedAmount(hedgedAmount)
                .unhedgedAmount(unhedgedAmount)
                .hedgeRatio(hedgeRatio)
                .status(exposure.getStatus())
                .build();
    }

    /**
     * Aggregate all hedge allocations grouped by exposure ID into a lookup map.
     */
    private Map<Long, BigDecimal> getHedgedAmountByExposureMap() {
        List<Object[]> rows = hedgeAllocationRepository.sumAllocationsGroupedByExposure();
        Map<Long, BigDecimal> map = new HashMap<>();
        if (rows != null) {
            for (Object[] row : rows) {
                if (row != null && row.length >= 2 && row[0] instanceof Long expId) {
                    BigDecimal sum = row[1] instanceof BigDecimal val ? val : BigDecimal.ZERO;
                    map.put(expId, sum);
                }
            }
        }
        return map;
    }
}

