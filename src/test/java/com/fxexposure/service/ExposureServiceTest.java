package com.fxexposure.service;

import com.fxexposure.dto.ExposureRequestDto;
import com.fxexposure.dto.ExposureResponseDto;
import com.fxexposure.entity.Exposure;
import com.fxexposure.entity.ExposureStatus;
import com.fxexposure.entity.ExposureType;
import com.fxexposure.exception.DuplicateResourceException;
import com.fxexposure.exception.ResourceNotFoundException;
import com.fxexposure.repository.ExposureRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;

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
class ExposureServiceTest {

    @Mock
    private ExposureRepository exposureRepository;

    @InjectMocks
    private ExposureService exposureService;

    private Exposure sampleExposure;
    private ExposureRequestDto requestDto;

    @BeforeEach
    void setUp() {
        sampleExposure = Exposure.builder()
                .id(1L)
                .exposureReference("EXP-USD-0001")
                .exposureType(ExposureType.RECEIVABLE)
                .currency("USD")
                .baseCurrency("INR")
                .amount(new BigDecimal("5000000.00"))
                .exposureDate(LocalDate.of(2026, 10, 4))
                .maturityDate(LocalDate.of(2027, 1, 2))
                .description("USD receivable from overseas client")
                .status(ExposureStatus.OPEN)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        requestDto = ExposureRequestDto.builder()
                .exposureReference("EXP-USD-0001")
                .exposureType(ExposureType.RECEIVABLE)
                .currency("USD")
                .baseCurrency("INR")
                .amount(new BigDecimal("5000000.00"))
                .exposureDate(LocalDate.of(2026, 10, 4))
                .maturityDate(LocalDate.of(2027, 1, 2))
                .description("USD receivable from overseas client")
                .status(ExposureStatus.OPEN)
                .build();
    }

    @Test
    @DisplayName("Should successfully create a new exposure")
    void createExposure_Success() {
        when(exposureRepository.existsByExposureReference("EXP-USD-0001")).thenReturn(false);
        when(exposureRepository.save(any(Exposure.class))).thenReturn(sampleExposure);

        ExposureResponseDto response = exposureService.createExposure(requestDto);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getExposureReference()).isEqualTo("EXP-USD-0001");
        assertThat(response.getCurrency()).isEqualTo("USD");
        assertThat(response.getAmount()).isEqualByComparingTo(new BigDecimal("5000000.00"));
        assertThat(response.getStatus()).isEqualTo(ExposureStatus.OPEN);
        verify(exposureRepository).save(any(Exposure.class));
    }

    @Test
    @DisplayName("Should default status to OPEN when status is omitted")
    void createExposure_DefaultOpenStatus() {
        requestDto.setStatus(null);
        when(exposureRepository.existsByExposureReference("EXP-USD-0001")).thenReturn(false);
        when(exposureRepository.save(any(Exposure.class))).thenAnswer(invocation -> {
            Exposure saved = invocation.getArgument(0);
            saved.setId(2L);
            return saved;
        });

        ExposureResponseDto response = exposureService.createExposure(requestDto);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(ExposureStatus.OPEN);
    }

    @Test
    @DisplayName("Should throw DuplicateResourceException on duplicate exposureReference")
    void createExposure_DuplicateReference() {
        when(exposureRepository.existsByExposureReference("EXP-USD-0001")).thenReturn(true);

        assertThatThrownBy(() -> exposureService.createExposure(requestDto))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("Exposure reference already exists");

        verify(exposureRepository, never()).save(any(Exposure.class));
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when amount is zero or negative")
    void createExposure_InvalidAmount() {
        requestDto.setAmount(new BigDecimal("0.00"));

        assertThatThrownBy(() -> exposureService.createExposure(requestDto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Amount must be greater than zero");

        verify(exposureRepository, never()).save(any(Exposure.class));
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when maturityDate is before exposureDate")
    void createExposure_InvalidMaturityDate() {
        requestDto.setExposureDate(LocalDate.of(2027, 1, 2));
        requestDto.setMaturityDate(LocalDate.of(2026, 10, 4));

        assertThatThrownBy(() -> exposureService.createExposure(requestDto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Maturity date cannot be before exposure date");

        verify(exposureRepository, never()).save(any(Exposure.class));
    }

    @Test
    @DisplayName("Should retrieve exposure by ID")
    void getExposureById_Success() {
        when(exposureRepository.findById(1L)).thenReturn(Optional.of(sampleExposure));

        ExposureResponseDto response = exposureService.getExposureById(1L);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getExposureReference()).isEqualTo("EXP-USD-0001");
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when exposure ID not found")
    void getExposureById_NotFound() {
        when(exposureRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> exposureService.getExposureById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Exposure not found with id: 99");
    }

    @Test
    @DisplayName("Should retrieve all exposures")
    @SuppressWarnings("unchecked")
    void getAllExposures_Success() {
        when(exposureRepository.findAll(any(Specification.class))).thenReturn(List.of(sampleExposure));

        List<ExposureResponseDto> exposures = exposureService.getAllExposures("USD", ExposureStatus.OPEN, ExposureType.RECEIVABLE);

        assertThat(exposures).hasSize(1);
        assertThat(exposures.get(0).getExposureReference()).isEqualTo("EXP-USD-0001");
    }

    @Test
    @DisplayName("Should successfully update an existing exposure")
    void updateExposure_Success() {
        when(exposureRepository.findById(1L)).thenReturn(Optional.of(sampleExposure));
        when(exposureRepository.findByExposureReference("EXP-USD-0001")).thenReturn(Optional.of(sampleExposure));
        when(exposureRepository.save(any(Exposure.class))).thenReturn(sampleExposure);

        ExposureRequestDto updateDto = ExposureRequestDto.builder()
                .exposureReference("EXP-USD-0001")
                .exposureType(ExposureType.RECEIVABLE)
                .currency("USD")
                .baseCurrency("INR")
                .amount(new BigDecimal("6000000.00"))
                .exposureDate(LocalDate.of(2026, 10, 4))
                .maturityDate(LocalDate.of(2027, 2, 2))
                .description("Updated receivable amount")
                .status(ExposureStatus.PARTIALLY_HEDGED)
                .build();

        ExposureResponseDto response = exposureService.updateExposure(1L, updateDto);

        assertThat(response).isNotNull();
        verify(exposureRepository).save(sampleExposure);
    }

    @Test
    @DisplayName("Should throw DuplicateResourceException on update if reference belongs to another exposure")
    void updateExposure_DuplicateReference() {
        Exposure anotherExposure = Exposure.builder()
                .id(2L)
                .exposureReference("EXP-USD-0002")
                .build();

        when(exposureRepository.findById(1L)).thenReturn(Optional.of(sampleExposure));
        when(exposureRepository.findByExposureReference("EXP-USD-0002")).thenReturn(Optional.of(anotherExposure));

        ExposureRequestDto updateDto = ExposureRequestDto.builder()
                .exposureReference("EXP-USD-0002")
                .exposureType(ExposureType.RECEIVABLE)
                .currency("USD")
                .baseCurrency("INR")
                .amount(new BigDecimal("5000000.00"))
                .exposureDate(LocalDate.of(2026, 10, 4))
                .maturityDate(LocalDate.of(2027, 1, 2))
                .build();

        assertThatThrownBy(() -> exposureService.updateExposure(1L, updateDto))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("Exposure reference already in use");

        verify(exposureRepository, never()).save(sampleExposure);
    }

    @Test
    @DisplayName("Should delete exposure by ID")
    void deleteExposure_Success() {
        when(exposureRepository.existsById(1L)).thenReturn(true);
        doNothing().when(exposureRepository).deleteById(1L);

        exposureService.deleteExposure(1L);

        verify(exposureRepository).deleteById(1L);
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException on delete if ID not found")
    void deleteExposure_NotFound() {
        when(exposureRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> exposureService.deleteExposure(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Exposure not found with id: 99");

        verify(exposureRepository, never()).deleteById(anyLong());
    }
}
