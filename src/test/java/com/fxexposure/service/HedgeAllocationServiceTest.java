package com.fxexposure.service;

import com.fxexposure.dto.HedgeAllocationRequestDto;
import com.fxexposure.dto.HedgeAllocationResponseDto;
import com.fxexposure.dto.HedgeSummaryResponseDto;
import com.fxexposure.entity.Derivative;
import com.fxexposure.entity.DerivativeStatus;
import com.fxexposure.entity.DerivativeType;
import com.fxexposure.entity.Exposure;
import com.fxexposure.entity.ExposureStatus;
import com.fxexposure.entity.ExposureType;
import com.fxexposure.entity.HedgeAllocation;
import com.fxexposure.exception.DuplicateResourceException;
import com.fxexposure.exception.ResourceNotFoundException;
import com.fxexposure.repository.DerivativeRepository;
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
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HedgeAllocationServiceTest {

    @Mock
    private HedgeAllocationRepository hedgeAllocationRepository;

    @Mock
    private ExposureRepository exposureRepository;

    @Mock
    private DerivativeRepository derivativeRepository;

    @InjectMocks
    private HedgeAllocationService hedgeAllocationService;

    private Exposure sampleExposure;
    private Derivative sampleDerivative;
    private HedgeAllocation sampleAllocation;
    private HedgeAllocationRequestDto requestDto;

    @BeforeEach
    void setUp() {
        sampleExposure = Exposure.builder()
                .id(1L)
                .exposureReference("EXP-USD-0001")
                .exposureType(ExposureType.RECEIVABLE)
                .currency("USD")
                .baseCurrency("INR")
                .amount(new BigDecimal("100000.00"))
                .exposureDate(LocalDate.of(2026, 10, 4))
                .maturityDate(LocalDate.of(2027, 1, 4))
                .status(ExposureStatus.OPEN)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        sampleDerivative = Derivative.builder()
                .id(1L)
                .derivativeReference("DER-USD-0001")
                .derivativeType(DerivativeType.FORWARD)
                .exposure(sampleExposure)
                .baseCurrency("USD")
                .quoteCurrency("INR")
                .notionalAmount(new BigDecimal("70000.00"))
                .tradeDate(LocalDate.of(2026, 10, 4))
                .maturityDate(LocalDate.of(2027, 1, 4))
                .spotRate(new BigDecimal("83.500000"))
                .forwardRate(new BigDecimal("84.100000"))
                .status(DerivativeStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        sampleAllocation = HedgeAllocation.builder()
                .id(1L)
                .allocationReference("ALLOC-001")
                .exposure(sampleExposure)
                .derivative(sampleDerivative)
                .allocatedAmount(new BigDecimal("40000.00"))
                .allocationDate(LocalDate.of(2026, 10, 4))
                .description("Initial 40k forward hedge")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        requestDto = HedgeAllocationRequestDto.builder()
                .allocationReference("ALLOC-001")
                .exposureId(1L)
                .derivativeId(1L)
                .allocatedAmount(new BigDecimal("40000.00"))
                .allocationDate(LocalDate.of(2026, 10, 4))
                .description("Initial 40k forward hedge")
                .build();
    }

    @Test
    @DisplayName("1. Should successfully create a valid allocation")
    void createAllocation_Success() {
        when(hedgeAllocationRepository.existsByAllocationReference("ALLOC-001")).thenReturn(false);
        when(exposureRepository.findById(1L)).thenReturn(Optional.of(sampleExposure));
        when(derivativeRepository.findById(1L)).thenReturn(Optional.of(sampleDerivative));
        when(hedgeAllocationRepository.sumAllocatedAmountByExposureId(1L)).thenReturn(BigDecimal.ZERO);
        when(hedgeAllocationRepository.sumAllocatedAmountByDerivativeId(1L)).thenReturn(BigDecimal.ZERO);
        when(hedgeAllocationRepository.save(any(HedgeAllocation.class))).thenReturn(sampleAllocation);

        HedgeAllocationResponseDto response = hedgeAllocationService.createAllocation(requestDto);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getAllocationReference()).isEqualTo("ALLOC-001");
        assertThat(response.getAllocatedAmount()).isEqualByComparingTo(new BigDecimal("40000.00"));
        assertThat(response.getExposureReference()).isEqualTo("EXP-USD-0001");
        assertThat(response.getDerivativeReference()).isEqualTo("DER-USD-0001");

        verify(hedgeAllocationRepository).save(any(HedgeAllocation.class));
        verify(exposureRepository).save(sampleExposure);
    }

    @Test
    @DisplayName("2. Should throw ResourceNotFoundException when Exposure not found")
    void createAllocation_ExposureNotFound() {
        when(hedgeAllocationRepository.existsByAllocationReference("ALLOC-001")).thenReturn(false);
        when(exposureRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> hedgeAllocationService.createAllocation(requestDto))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Exposure not found with id: 1");

        verify(hedgeAllocationRepository, never()).save(any(HedgeAllocation.class));
    }

    @Test
    @DisplayName("3. Should throw ResourceNotFoundException when Derivative not found")
    void createAllocation_DerivativeNotFound() {
        when(hedgeAllocationRepository.existsByAllocationReference("ALLOC-001")).thenReturn(false);
        when(exposureRepository.findById(1L)).thenReturn(Optional.of(sampleExposure));
        when(derivativeRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> hedgeAllocationService.createAllocation(requestDto))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Derivative not found with id: 1");

        verify(hedgeAllocationRepository, never()).save(any(HedgeAllocation.class));
    }

    @Test
    @DisplayName("4. Should throw DuplicateResourceException on duplicate allocation reference")
    void createAllocation_DuplicateReference() {
        when(hedgeAllocationRepository.existsByAllocationReference("ALLOC-001")).thenReturn(true);

        assertThatThrownBy(() -> hedgeAllocationService.createAllocation(requestDto))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("Allocation reference already exists: ALLOC-001");

        verify(hedgeAllocationRepository, never()).save(any(HedgeAllocation.class));
    }

    @Test
    @DisplayName("5. Should throw IllegalArgumentException on zero or negative allocated amount")
    void createAllocation_InvalidAmount() {
        requestDto.setAllocatedAmount(BigDecimal.ZERO);

        assertThatThrownBy(() -> hedgeAllocationService.createAllocation(requestDto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Allocated amount must be greater than zero");

        verify(hedgeAllocationRepository, never()).save(any(HedgeAllocation.class));
    }

    @Test
    @DisplayName("6. Should throw IllegalArgumentException when allocation exceeds available exposure")
    void createAllocation_ExceedsExposure() {
        requestDto.setAllocatedAmount(new BigDecimal("80000.00"));

        when(hedgeAllocationRepository.existsByAllocationReference("ALLOC-001")).thenReturn(false);
        when(exposureRepository.findById(1L)).thenReturn(Optional.of(sampleExposure));
        when(derivativeRepository.findById(1L)).thenReturn(Optional.of(sampleDerivative));
        // Already 50k allocated, exposure is 100k -> 50k available, but requesting 80k
        when(hedgeAllocationRepository.sumAllocatedAmountByExposureId(1L)).thenReturn(new BigDecimal("50000.00"));

        assertThatThrownBy(() -> hedgeAllocationService.createAllocation(requestDto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("exceeds available exposure amount");

        verify(hedgeAllocationRepository, never()).save(any(HedgeAllocation.class));
    }

    @Test
    @DisplayName("7. Should throw IllegalArgumentException when allocation exceeds available derivative notional")
    void createAllocation_ExceedsDerivativeNotional() {
        requestDto.setAllocatedAmount(new BigDecimal("50000.00"));

        when(hedgeAllocationRepository.existsByAllocationReference("ALLOC-001")).thenReturn(false);
        when(exposureRepository.findById(1L)).thenReturn(Optional.of(sampleExposure));
        when(derivativeRepository.findById(1L)).thenReturn(Optional.of(sampleDerivative));
        // Exposure has 100k available
        when(hedgeAllocationRepository.sumAllocatedAmountByExposureId(1L)).thenReturn(BigDecimal.ZERO);
        // Derivative has 70k notional, 30k already allocated -> 40k available, but requesting 50k
        when(hedgeAllocationRepository.sumAllocatedAmountByDerivativeId(1L)).thenReturn(new BigDecimal("30000.00"));

        assertThatThrownBy(() -> hedgeAllocationService.createAllocation(requestDto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("exceeds available derivative notional amount");

        verify(hedgeAllocationRepository, never()).save(any(HedgeAllocation.class));
    }

    @Test
    @DisplayName("8. Should support multiple allocations for one exposure")
    void getAllocationsByExposure_Multiple() {
        HedgeAllocation alloc2 = HedgeAllocation.builder()
                .id(2L)
                .allocationReference("ALLOC-002")
                .exposure(sampleExposure)
                .derivative(sampleDerivative)
                .allocatedAmount(new BigDecimal("20000.00"))
                .allocationDate(LocalDate.of(2026, 10, 5))
                .build();

        when(exposureRepository.existsById(1L)).thenReturn(true);
        when(hedgeAllocationRepository.findByExposureId(1L)).thenReturn(List.of(sampleAllocation, alloc2));

        List<HedgeAllocationResponseDto> results = hedgeAllocationService.getAllocationsByExposure(1L);

        assertThat(results).hasSize(2);
        assertThat(results.get(0).getAllocationReference()).isEqualTo("ALLOC-001");
        assertThat(results.get(1).getAllocationReference()).isEqualTo("ALLOC-002");
    }

    @Test
    @DisplayName("9. Should calculate total hedged amount in summary")
    void getExposureHedgeSummary_TotalHedged() {
        when(exposureRepository.findById(1L)).thenReturn(Optional.of(sampleExposure));
        when(hedgeAllocationRepository.sumAllocatedAmountByExposureId(1L)).thenReturn(new BigDecimal("60000.00"));

        HedgeSummaryResponseDto summary = hedgeAllocationService.getExposureHedgeSummary(1L);

        assertThat(summary).isNotNull();
        assertThat(summary.getTotalHedgedAmount()).isEqualByComparingTo(new BigDecimal("60000.00"));
    }

    @Test
    @DisplayName("10. Should calculate unhedged amount correctly in summary")
    void getExposureHedgeSummary_UnhedgedAmount() {
        when(exposureRepository.findById(1L)).thenReturn(Optional.of(sampleExposure));
        when(hedgeAllocationRepository.sumAllocatedAmountByExposureId(1L)).thenReturn(new BigDecimal("60000.00"));

        HedgeSummaryResponseDto summary = hedgeAllocationService.getExposureHedgeSummary(1L);

        assertThat(summary).isNotNull();
        // 100,000 - 60,000 = 40,000
        assertThat(summary.getUnhedgedAmount()).isEqualByComparingTo(new BigDecimal("40000.00"));
    }

    @Test
    @DisplayName("11. Should calculate hedge ratio accurately in summary")
    void getExposureHedgeSummary_HedgeRatio() {
        when(exposureRepository.findById(1L)).thenReturn(Optional.of(sampleExposure));
        when(hedgeAllocationRepository.sumAllocatedAmountByExposureId(1L)).thenReturn(new BigDecimal("70000.00"));

        HedgeSummaryResponseDto summary = hedgeAllocationService.getExposureHedgeSummary(1L);

        assertThat(summary).isNotNull();
        // 70,000 / 100,000 * 100 = 70.00%
        assertThat(summary.getHedgeRatio()).isEqualByComparingTo(new BigDecimal("70.00"));
    }

    @Test
    @DisplayName("12. Exposure status should transition to PARTIALLY_HEDGED when partly allocated")
    void updateExposureStatus_PartiallyHedged() {
        when(hedgeAllocationRepository.existsByAllocationReference("ALLOC-001")).thenReturn(false);
        when(exposureRepository.findById(1L)).thenReturn(Optional.of(sampleExposure));
        when(derivativeRepository.findById(1L)).thenReturn(Optional.of(sampleDerivative));
        when(hedgeAllocationRepository.sumAllocatedAmountByExposureId(1L)).thenReturn(BigDecimal.ZERO).thenReturn(new BigDecimal("40000.00"));
        when(hedgeAllocationRepository.sumAllocatedAmountByDerivativeId(1L)).thenReturn(BigDecimal.ZERO);
        when(hedgeAllocationRepository.save(any(HedgeAllocation.class))).thenReturn(sampleAllocation);

        hedgeAllocationService.createAllocation(requestDto);

        assertThat(sampleExposure.getStatus()).isEqualTo(ExposureStatus.PARTIALLY_HEDGED);
        verify(exposureRepository).save(sampleExposure);
    }

    @Test
    @DisplayName("13. Exposure status should transition to FULLY_HEDGED when fully allocated")
    void updateExposureStatus_FullyHedged() {
        requestDto.setAllocatedAmount(new BigDecimal("100000.00"));
        sampleDerivative.setNotionalAmount(new BigDecimal("100000.00"));

        when(hedgeAllocationRepository.existsByAllocationReference("ALLOC-001")).thenReturn(false);
        when(exposureRepository.findById(1L)).thenReturn(Optional.of(sampleExposure));
        when(derivativeRepository.findById(1L)).thenReturn(Optional.of(sampleDerivative));
        when(hedgeAllocationRepository.sumAllocatedAmountByExposureId(1L)).thenReturn(BigDecimal.ZERO).thenReturn(new BigDecimal("100000.00"));
        when(hedgeAllocationRepository.sumAllocatedAmountByDerivativeId(1L)).thenReturn(BigDecimal.ZERO);
        when(hedgeAllocationRepository.save(any(HedgeAllocation.class))).thenReturn(sampleAllocation);

        hedgeAllocationService.createAllocation(requestDto);

        assertThat(sampleExposure.getStatus()).isEqualTo(ExposureStatus.FULLY_HEDGED);
        verify(exposureRepository).save(sampleExposure);
    }

    @Test
    @DisplayName("14. Deleting allocation should recalculate exposure status to OPEN if total hedged becomes zero")
    void deleteAllocation_RecalculatesStatus() {
        sampleExposure.setStatus(ExposureStatus.PARTIALLY_HEDGED);
        when(hedgeAllocationRepository.findById(1L)).thenReturn(Optional.of(sampleAllocation));
        doNothing().when(hedgeAllocationRepository).delete(sampleAllocation);
        when(hedgeAllocationRepository.sumAllocatedAmountByExposureId(1L)).thenReturn(BigDecimal.ZERO);

        hedgeAllocationService.deleteAllocation(1L);

        verify(hedgeAllocationRepository).delete(sampleAllocation);
        assertThat(sampleExposure.getStatus()).isEqualTo(ExposureStatus.OPEN);
        verify(exposureRepository).save(sampleExposure);
    }

    @Test
    @DisplayName("15. Updating allocation should validate available amounts excluding current allocation")
    void updateAllocation_ValidatesExcludingCurrent() {
        HedgeAllocationRequestDto updateDto = HedgeAllocationRequestDto.builder()
                .allocationReference("ALLOC-001")
                .exposureId(1L)
                .derivativeId(1L)
                .allocatedAmount(new BigDecimal("60000.00"))
                .allocationDate(LocalDate.of(2026, 10, 4))
                .description("Increased allocation")
                .build();

        when(hedgeAllocationRepository.findById(1L)).thenReturn(Optional.of(sampleAllocation));
        when(hedgeAllocationRepository.findByAllocationReference("ALLOC-001")).thenReturn(Optional.of(sampleAllocation));
        when(exposureRepository.findById(1L)).thenReturn(Optional.of(sampleExposure));
        when(derivativeRepository.findById(1L)).thenReturn(Optional.of(sampleDerivative));
        // Excluding current allocation, other allocations sum to 20k -> 80k available on exposure
        when(hedgeAllocationRepository.sumAllocatedAmountByExposureIdExcluding(1L, 1L)).thenReturn(new BigDecimal("20000.00"));
        // Derivative has 70k notional, 0 other allocations -> 70k available
        when(hedgeAllocationRepository.sumAllocatedAmountByDerivativeIdExcluding(1L, 1L)).thenReturn(BigDecimal.ZERO);
        when(hedgeAllocationRepository.save(any(HedgeAllocation.class))).thenReturn(sampleAllocation);
        when(hedgeAllocationRepository.sumAllocatedAmountByExposureId(1L)).thenReturn(new BigDecimal("60000.00"));

        HedgeAllocationResponseDto response = hedgeAllocationService.updateAllocation(1L, updateDto);

        assertThat(response).isNotNull();
        verify(hedgeAllocationRepository).save(sampleAllocation);
    }
}

