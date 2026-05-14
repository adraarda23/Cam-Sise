package ardaaydinkilinc.Cam_Sise.analytics.anomaly;

import ardaaydinkilinc.Cam_Sise.inventory.domain.FillerStock;
import ardaaydinkilinc.Cam_Sise.inventory.domain.vo.AssetType;
import ardaaydinkilinc.Cam_Sise.inventory.domain.vo.LossRate;
import ardaaydinkilinc.Cam_Sise.inventory.repository.FillerStockRepository;
import ardaaydinkilinc.Cam_Sise.shared.domain.event.DomainEvent;
import ardaaydinkilinc.Cam_Sise.shared.domain.event.DomainEventPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnomalySchedulerTest {

    @Mock private AnomalyDetectionService detectionService;
    @Mock private FillerStockRepository fillerStockRepository;
    @Mock private DomainEventPublisher eventPublisher;

    @InjectMocks
    private AnomalyScheduler scheduler;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(scheduler, "maxFillersPerRun", 500);
    }

    @Test
    @DisplayName("Publishes StockAnomalyDetected for each anomaly result")
    void publishesOnAnomaly() {
        List<FillerStock> stocks = List.of(
                FillerStock.initialize(1L, AssetType.PALLET, 100, new LossRate(5.0)),
                FillerStock.initialize(2L, AssetType.SEPARATOR, 50, new LossRate(3.0))
        );
        when(fillerStockRepository.findAll()).thenReturn(stocks);

        AnomalyResult anomaly = new AnomalyResult(
                1L, AssetType.PALLET, LocalDateTime.now(),
                200, 50, 10, 15.0, AnomalySeverity.CRITICAL, "spike");
        AnomalyResult normal = AnomalyResult.normal(2L, AssetType.SEPARATOR, "ok");

        when(detectionService.checkLatestDay(1L, AssetType.PALLET)).thenReturn(anomaly);
        when(detectionService.checkLatestDay(2L, AssetType.SEPARATOR)).thenReturn(normal);

        scheduler.runDailyScan();

        ArgumentCaptor<DomainEvent> captor = ArgumentCaptor.forClass(DomainEvent.class);
        verify(eventPublisher, times(1)).publish(captor.capture());
        assertThat(captor.getValue()).isInstanceOf(StockAnomalyDetected.class);
        StockAnomalyDetected event = (StockAnomalyDetected) captor.getValue();
        assertThat(event.fillerId()).isEqualTo(1L);
        assertThat(event.severity()).isEqualTo(AnomalySeverity.CRITICAL);
    }

    @Test
    @DisplayName("Does not publish when no anomalies")
    void noPublishWhenAllNormal() {
        FillerStock stock = FillerStock.initialize(1L, AssetType.PALLET, 100, new LossRate(5.0));
        when(fillerStockRepository.findAll()).thenReturn(List.of(stock));
        when(detectionService.checkLatestDay(any(), any()))
                .thenReturn(AnomalyResult.normal(1L, AssetType.PALLET, "ok"));

        scheduler.runDailyScan();

        verify(eventPublisher, never()).publish(any());
    }

    @Test
    @DisplayName("Honours max-fillers-per-run cap")
    void honoursCap() {
        ReflectionTestUtils.setField(scheduler, "maxFillersPerRun", 1);
        List<FillerStock> stocks = List.of(
                FillerStock.initialize(1L, AssetType.PALLET, 100, new LossRate(5.0)),
                FillerStock.initialize(2L, AssetType.PALLET, 100, new LossRate(5.0))
        );
        when(fillerStockRepository.findAll()).thenReturn(stocks);
        when(detectionService.checkLatestDay(any(), any()))
                .thenReturn(AnomalyResult.normal(1L, AssetType.PALLET, "ok"));

        scheduler.runDailyScan();

        verify(detectionService, times(1)).checkLatestDay(any(), any());
    }

    @Test
    @DisplayName("Detection exception per-stock is logged but does not abort scan")
    void exceptionDoesNotAbort() {
        FillerStock s1 = FillerStock.initialize(1L, AssetType.PALLET, 100, new LossRate(5.0));
        FillerStock s2 = FillerStock.initialize(2L, AssetType.SEPARATOR, 50, new LossRate(3.0));
        when(fillerStockRepository.findAll()).thenReturn(List.of(s1, s2));
        when(detectionService.checkLatestDay(1L, AssetType.PALLET))
                .thenThrow(new RuntimeException("boom"));
        when(detectionService.checkLatestDay(2L, AssetType.SEPARATOR))
                .thenReturn(AnomalyResult.normal(2L, AssetType.SEPARATOR, "ok"));

        scheduler.runDailyScan();

        verify(detectionService, times(2)).checkLatestDay(any(), any());
    }
}
