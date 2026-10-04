package com.fxexposure.service;

import com.fxexposure.dto.DerivativeRequestDto;
import com.fxexposure.dto.DerivativeResponseDto;
import com.fxexposure.entity.Derivative;
import com.fxexposure.entity.DerivativeStatus;
import com.fxexposure.entity.DerivativeType;
import com.fxexposure.entity.Exposure;
import com.fxexposure.entity.ExposureStatus;
import com.fxexposure.entity.ExposureType;
import com.fxexposure.entity.OptionType;
import com.fxexposure.exception.DuplicateResourceException;
import com.fxexposure.exception.ResourceNotFoundException;
import com.fxexposure.repository.DerivativeRepository;
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
class DerivativeServiceTest {

    @Mock
    private DerivativeRepository derivativeRepository;

    @Mock
    private ExposureRepository exposureRepository;

    @InjectMocks
    private DerivativeService derivativeService;

    private Exposure sampleExposure;
    private Derivative sampleDerivative;
    private DerivativeRequestDto requestDto;

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
                .status(ExposureStatus.OPEN)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        sampleDerivative = Derivative.builder()
                .id(1L)
                .derivativeReference("FWD-USD-0001")
                .derivativeType(DerivativeType.FORWARD)
                .exposure(sampleExposure)
                .baseCurrency("USD")
                .quoteCurrency("INR")
                .notionalAmount(new BigDecimal("1000000.00"))
                .tradeDate(LocalDate.of(2026, 10, 4))
                .maturityDate(LocalDate.of(2026, 12, 31))
                .spotRate(new BigDecimal("83.500000"))
                .forwardRate(new BigDecimal("84.100000"))
                .status(DerivativeStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        requestDto = DerivativeRequestDto.builder()
                .derivativeReference("FWD-USD-0001")
                .derivativeType(DerivativeType.FORWARD)
                .exposureId(1L)
                .baseCurrency("USD")
                .quoteCurrency("INR")
                .notionalAmount(new BigDecimal("1000000.00"))
                .tradeDate(LocalDate.of(2026, 10, 4))
                .maturityDate(LocalDate.of(2026, 12, 31))
                .spotRate(new BigDecimal("83.500000"))
                .forwardRate(new BigDecimal("84.100000"))
                .status(DerivativeStatus.ACTIVE)
                .build();
    }

    @Test
    @DisplayName("1. Should successfully create a new derivative")
    void createDerivative_Success() {
        when(derivativeRepository.existsByDerivativeReference("FWD-USD-0001")).thenReturn(false);
        when(exposureRepository.findById(1L)).thenReturn(Optional.of(sampleExposure));
        when(derivativeRepository.save(any(Derivative.class))).thenReturn(sampleDerivative);

        DerivativeResponseDto response = derivativeService.createDerivative(requestDto);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getDerivativeReference()).isEqualTo("FWD-USD-0001");
        assertThat(response.getDerivativeType()).isEqualTo(DerivativeType.FORWARD);
        assertThat(response.getExposureId()).isEqualTo(1L);
        assertThat(response.getExposureReference()).isEqualTo("EXP-USD-0001");
        assertThat(response.getNotionalAmount()).isEqualByComparingTo(new BigDecimal("1000000.00"));
        assertThat(response.getStatus()).isEqualTo(DerivativeStatus.ACTIVE);
        verify(derivativeRepository).save(any(Derivative.class));
    }

    @Test
    @DisplayName("2. Should default status to ACTIVE when status is omitted")
    void createDerivative_DefaultActiveStatus() {
        requestDto.setStatus(null);
        when(derivativeRepository.existsByDerivativeReference("FWD-USD-0001")).thenReturn(false);
        when(exposureRepository.findById(1L)).thenReturn(Optional.of(sampleExposure));
        when(derivativeRepository.save(any(Derivative.class))).thenAnswer(invocation -> {
            Derivative saved = invocation.getArgument(0);
            saved.setId(2L);
            return saved;
        });

        DerivativeResponseDto response = derivativeService.createDerivative(requestDto);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(DerivativeStatus.ACTIVE);
    }

    @Test
    @DisplayName("3. Should throw DuplicateResourceException on duplicate derivativeReference")
    void createDerivative_DuplicateReference() {
        when(derivativeRepository.existsByDerivativeReference("FWD-USD-0001")).thenReturn(true);

        assertThatThrownBy(() -> derivativeService.createDerivative(requestDto))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("Derivative reference already exists");

        verify(derivativeRepository, never()).save(any(Derivative.class));
    }

    @Test
    @DisplayName("4. Should throw ResourceNotFoundException when Exposure not found")
    void createDerivative_ExposureNotFound() {
        when(derivativeRepository.existsByDerivativeReference("FWD-USD-0001")).thenReturn(false);
        when(exposureRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> derivativeService.createDerivative(requestDto))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Exposure not found with id: 1");

        verify(derivativeRepository, never()).save(any(Derivative.class));
    }

    @Test
    @DisplayName("5. Should throw IllegalArgumentException when notionalAmount is zero or negative")
    void createDerivative_InvalidNotionalAmount() {
        requestDto.setNotionalAmount(new BigDecimal("0.00"));

        assertThatThrownBy(() -> derivativeService.createDerivative(requestDto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Notional amount must be greater than zero");

        verify(derivativeRepository, never()).save(any(Derivative.class));
    }

    @Test
    @DisplayName("6. Should throw IllegalArgumentException when maturityDate is before tradeDate")
    void createDerivative_InvalidMaturityDate() {
        requestDto.setTradeDate(LocalDate.of(2026, 12, 31));
        requestDto.setMaturityDate(LocalDate.of(2026, 10, 4));

        assertThatThrownBy(() -> derivativeService.createDerivative(requestDto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Maturity date cannot be before trade date");

        verify(derivativeRepository, never()).save(any(Derivative.class));
    }

    @Test
    @DisplayName("7. Should retrieve derivative by ID")
    void getDerivativeById_Success() {
        when(derivativeRepository.findById(1L)).thenReturn(Optional.of(sampleDerivative));

        DerivativeResponseDto response = derivativeService.getDerivativeById(1L);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getDerivativeReference()).isEqualTo("FWD-USD-0001");
        assertThat(response.getExposureReference()).isEqualTo("EXP-USD-0001");
    }

    @Test
    @DisplayName("8. Should throw ResourceNotFoundException when derivative ID not found")
    void getDerivativeById_NotFound() {
        when(derivativeRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> derivativeService.getDerivativeById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Derivative not found with id: 99");
    }

    @Test
    @DisplayName("9. Should retrieve all derivatives")
    @SuppressWarnings("unchecked")
    void getAllDerivatives_Success() {
        when(derivativeRepository.findAll(any(Specification.class))).thenReturn(List.of(sampleDerivative));

        List<DerivativeResponseDto> results = derivativeService.getAllDerivatives(null, null, null, null, null);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getDerivativeReference()).isEqualTo("FWD-USD-0001");
    }

    @Test
    @DisplayName("10. Should filter derivatives by type")
    @SuppressWarnings("unchecked")
    void getAllDerivatives_FilterByType() {
        when(derivativeRepository.findAll(any(Specification.class))).thenReturn(List.of(sampleDerivative));

        List<DerivativeResponseDto> results = derivativeService.getAllDerivatives(DerivativeType.FORWARD, null, null, null, null);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getDerivativeType()).isEqualTo(DerivativeType.FORWARD);
    }

    @Test
    @DisplayName("11. Should filter derivatives by status")
    @SuppressWarnings("unchecked")
    void getAllDerivatives_FilterByStatus() {
        when(derivativeRepository.findAll(any(Specification.class))).thenReturn(List.of(sampleDerivative));

        List<DerivativeResponseDto> results = derivativeService.getAllDerivatives(null, DerivativeStatus.ACTIVE, null, null, null);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getStatus()).isEqualTo(DerivativeStatus.ACTIVE);
    }

    @Test
    @DisplayName("12. Should filter derivatives by exposure ID")
    @SuppressWarnings("unchecked")
    void getAllDerivatives_FilterByExposureId() {
        when(derivativeRepository.findAll(any(Specification.class))).thenReturn(List.of(sampleDerivative));

        List<DerivativeResponseDto> results = derivativeService.getAllDerivatives(null, null, 1L, null, null);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getExposureId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("13. Should successfully update an existing derivative")
    void updateDerivative_Success() {
        when(derivativeRepository.findById(1L)).thenReturn(Optional.of(sampleDerivative));
        when(derivativeRepository.findByDerivativeReference("FWD-USD-0001")).thenReturn(Optional.of(sampleDerivative));
        when(exposureRepository.findById(1L)).thenReturn(Optional.of(sampleExposure));
        when(derivativeRepository.save(any(Derivative.class))).thenReturn(sampleDerivative);

        DerivativeRequestDto updateDto = DerivativeRequestDto.builder()
                .derivativeReference("FWD-USD-0001")
                .derivativeType(DerivativeType.FORWARD)
                .exposureId(1L)
                .baseCurrency("USD")
                .quoteCurrency("INR")
                .notionalAmount(new BigDecimal("1500000.00"))
                .tradeDate(LocalDate.of(2026, 10, 4))
                .maturityDate(LocalDate.of(2026, 12, 31))
                .status(DerivativeStatus.ACTIVE)
                .description("Updated notional amount")
                .build();

        DerivativeResponseDto response = derivativeService.updateDerivative(1L, updateDto);

        assertThat(response).isNotNull();
        verify(derivativeRepository).save(sampleDerivative);
    }

    @Test
    @DisplayName("14. Should throw DuplicateResourceException on update if reference belongs to another derivative")
    void updateDerivative_DuplicateReference() {
        Derivative anotherDerivative = Derivative.builder()
                .id(2L)
                .derivativeReference("FWD-USD-0002")
                .build();

        when(derivativeRepository.findById(1L)).thenReturn(Optional.of(sampleDerivative));
        when(derivativeRepository.findByDerivativeReference("FWD-USD-0002")).thenReturn(Optional.of(anotherDerivative));

        DerivativeRequestDto updateDto = DerivativeRequestDto.builder()
                .derivativeReference("FWD-USD-0002")
                .derivativeType(DerivativeType.FORWARD)
                .exposureId(1L)
                .baseCurrency("USD")
                .quoteCurrency("INR")
                .notionalAmount(new BigDecimal("1000000.00"))
                .tradeDate(LocalDate.of(2026, 10, 4))
                .maturityDate(LocalDate.of(2026, 12, 31))
                .build();

        assertThatThrownBy(() -> derivativeService.updateDerivative(1L, updateDto))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("Derivative reference already in use");

        verify(derivativeRepository, never()).save(sampleDerivative);
    }

    @Test
    @DisplayName("15. Should successfully delete derivative by ID")
    void deleteDerivative_Success() {
        when(derivativeRepository.existsById(1L)).thenReturn(true);
        doNothing().when(derivativeRepository).deleteById(1L);

        derivativeService.deleteDerivative(1L);

        verify(derivativeRepository).deleteById(1L);
        verify(exposureRepository, never()).deleteById(anyLong());
    }

    @Test
    @DisplayName("16. Should throw ResourceNotFoundException on delete if ID not found")
    void deleteDerivative_NotFound() {
        when(derivativeRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> derivativeService.deleteDerivative(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Derivative not found with id: 99");

        verify(derivativeRepository, never()).deleteById(anyLong());
    }
}

