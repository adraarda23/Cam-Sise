package ardaaydinkilinc.Cam_Sise.inventory.service;

import ardaaydinkilinc.Cam_Sise.inventory.domain.FillerStock;
import ardaaydinkilinc.Cam_Sise.inventory.domain.vo.AssetType;
import ardaaydinkilinc.Cam_Sise.inventory.domain.vo.LossRate;
import ardaaydinkilinc.Cam_Sise.inventory.repository.FillerStockRepository;
import ardaaydinkilinc.Cam_Sise.inventory.repository.LossRecordRepository;
import ardaaydinkilinc.Cam_Sise.core.repository.FillerRepository;
import org.mockito.ArgumentCaptor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("FillerStockService Tests")
class FillerStockServiceTest {

    @Mock private FillerStockRepository fillerStockRepository;
    @Mock private FillerRepository fillerRepository;
    @Mock private LossRecordRepository lossRecordRepository;

    @InjectMocks
    private FillerStockService service;

    private static final Long FILLER_ID = 1L;

    private FillerStock palletStock;

    @BeforeEach
    void setUp() {
        palletStock = FillerStock.initialize(FILLER_ID, AssetType.PALLET, 100, new LossRate(5.0));
        palletStock.recordInflow(300, "INF-001");
        palletStock.clearDomainEvents();
    }

    @Nested
    @DisplayName("getStock")
    class GetStock {

        @Test
        @DisplayName("returns stock when found")
        void returnsStock() {
            when(fillerStockRepository.findByFillerIdAndAssetType(FILLER_ID, AssetType.PALLET))
                    .thenReturn(Optional.of(palletStock));

            FillerStock result = service.getStock(FILLER_ID, AssetType.PALLET);

            assertThat(result).isEqualTo(palletStock);
            assertThat(result.getCurrentQuantity()).isEqualTo(300);
        }

        @Test
        @DisplayName("throws IllegalArgumentException when stock not found")
        void throwsWhenNotFound() {
            when(fillerStockRepository.findByFillerIdAndAssetType(FILLER_ID, AssetType.PALLET))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getStock(FILLER_ID, AssetType.PALLET))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("recordInflow")
    class RecordInflow {

        @Test
        @DisplayName("increases stock quantity and saves")
        void increasesQuantity() {
            when(fillerStockRepository.findByFillerIdAndAssetType(FILLER_ID, AssetType.PALLET))
                    .thenReturn(Optional.of(palletStock));
            when(fillerStockRepository.save(palletStock)).thenReturn(palletStock);

            FillerStock result = service.recordInflow(FILLER_ID, AssetType.PALLET, 100, "INF-002");

            assertThat(result.getCurrentQuantity()).isEqualTo(400);
            verify(fillerStockRepository).save(palletStock);
        }

        @Test
        @DisplayName("throws when stock does not exist")
        void throwsWhenStockNotFound() {
            when(fillerStockRepository.findByFillerIdAndAssetType(FILLER_ID, AssetType.PALLET))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.recordInflow(FILLER_ID, AssetType.PALLET, 100, "REF"))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("recordCollection")
    class RecordCollection {

        @Test
        @DisplayName("decreases stock quantity and saves")
        void decreasesQuantity() {
            when(fillerStockRepository.findByFillerIdAndAssetType(FILLER_ID, AssetType.PALLET))
                    .thenReturn(Optional.of(palletStock));
            when(fillerStockRepository.save(palletStock)).thenReturn(palletStock);

            FillerStock result = service.recordCollection(FILLER_ID, AssetType.PALLET, 50, "PLAN-001");

            assertThat(result.getCurrentQuantity()).isEqualTo(250);
            verify(fillerStockRepository).save(palletStock);
        }

        @Test
        @DisplayName("throws when collection amount exceeds current stock")
        void throwsWhenExceedsStock() {
            when(fillerStockRepository.findByFillerIdAndAssetType(FILLER_ID, AssetType.PALLET))
                    .thenReturn(Optional.of(palletStock));

            assertThatThrownBy(() -> service.recordCollection(FILLER_ID, AssetType.PALLET, 999, "PLAN-001"))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("updateThreshold")
    class UpdateThreshold {

        @Test
        @DisplayName("updates threshold and saves")
        void updatesThreshold() {
            when(fillerStockRepository.findByFillerIdAndAssetType(FILLER_ID, AssetType.PALLET))
                    .thenReturn(Optional.of(palletStock));
            when(fillerStockRepository.save(palletStock)).thenReturn(palletStock);

            FillerStock result = service.updateThreshold(FILLER_ID, AssetType.PALLET, 200);

            assertThat(result.getThresholdQuantity()).isEqualTo(200);
        }
    }

    @Nested
    @DisplayName("getStocksByFiller")
    class GetStocksByFiller {

        @Test
        @DisplayName("returns all stocks for filler")
        void returnsAllStocks() {
            FillerStock sepStock = FillerStock.initialize(FILLER_ID, AssetType.SEPARATOR, 50, new LossRate(3.0));
            when(fillerStockRepository.findByFillerId(FILLER_ID)).thenReturn(List.of(palletStock, sepStock));

            List<FillerStock> result = service.getStocksByFiller(FILLER_ID);

            assertThat(result).hasSize(2);
        }

        @Test
        @DisplayName("returns empty list when filler has no stocks")
        void returnsEmptyList() {
            when(fillerStockRepository.findByFillerId(FILLER_ID)).thenReturn(List.of());

            List<FillerStock> result = service.getStocksByFiller(FILLER_ID);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("initializeStockForFiller")
    class InitializeStockForFiller {

        @Test
        @DisplayName("PALLET ve SEPARATOR için iki kayıt oluşturmalı")
        void createsTwoStockRecords() {
            service.initializeStockForFiller(FILLER_ID);

            verify(fillerStockRepository, times(2)).save(any(FillerStock.class));
        }

        @Test
        @DisplayName("Biri PALLET, diğeri SEPARATOR olmalı")
        void createsPalletAndSeparatorStocks() {
            ArgumentCaptor<FillerStock> captor = ArgumentCaptor.forClass(FillerStock.class);

            service.initializeStockForFiller(FILLER_ID);

            verify(fillerStockRepository, times(2)).save(captor.capture());
            List<AssetType> assetTypes = captor.getAllValues().stream()
                    .map(FillerStock::getAssetType)
                    .toList();
            assertThat(assetTypes).containsExactlyInAnyOrder(AssetType.PALLET, AssetType.SEPARATOR);
        }
    }

    @Nested
    @DisplayName("updateLossRate")
    class UpdateLossRate {

        @Test
        @DisplayName("Loss rate güncellenmeli ve kaydedilmeli")
        void updatesLossRateAndSaves() {
            when(fillerStockRepository.findByFillerIdAndAssetType(FILLER_ID, AssetType.PALLET))
                    .thenReturn(Optional.of(palletStock));
            when(fillerStockRepository.save(palletStock)).thenReturn(palletStock);

            FillerStock result = service.updateLossRate(FILLER_ID, AssetType.PALLET, 8.0);

            assertThat(result.getEstimatedLossRate().percentage()).isEqualTo(8.0);
            verify(fillerStockRepository).save(palletStock);
        }

        @Test
        @DisplayName("Stock bulunamazsa exception fırlatmalı")
        void throwsWhenStockNotFound() {
            when(fillerStockRepository.findByFillerIdAndAssetType(FILLER_ID, AssetType.PALLET))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.updateLossRate(FILLER_ID, AssetType.PALLET, 8.0))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("getStocksByAssetType")
    class GetStocksByAssetType {

        @Test
        @DisplayName("AssetType'a göre stokları döndürmeli")
        void returnsStocksByAssetType() {
            when(fillerStockRepository.findByAssetType(AssetType.PALLET))
                    .thenReturn(List.of(palletStock));

            List<FillerStock> result = service.getStocksByAssetType(AssetType.PALLET);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getAssetType()).isEqualTo(AssetType.PALLET);
        }

        @Test
        @DisplayName("Kayıt yoksa boş liste döndürmeli")
        void returnsEmptyList() {
            when(fillerStockRepository.findByAssetType(AssetType.SEPARATOR))
                    .thenReturn(List.of());

            List<FillerStock> result = service.getStocksByAssetType(AssetType.SEPARATOR);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("getAllStocks")
    class GetAllStocks {

        @Test
        @DisplayName("Tüm stokları döndürmeli")
        void returnsAllStocks() {
            FillerStock sepStock = FillerStock.initialize(2L, AssetType.SEPARATOR, 50, new LossRate(3.0));
            when(fillerStockRepository.findAll()).thenReturn(List.of(palletStock, sepStock));

            List<FillerStock> result = service.getAllStocks();

            assertThat(result).hasSize(2);
        }
    }

    @Nested
    @DisplayName("getAllStocksByPoolOperatorId")
    class GetAllStocksByPoolOperatorId {

        @Test
        @DisplayName("PoolOperatorId'ye göre repository'e delege etmeli")
        void delegatesToRepository() {
            when(fillerStockRepository.findByPoolOperatorId(1L)).thenReturn(List.of(palletStock));

            List<FillerStock> result = service.getAllStocksByPoolOperatorId(1L);

            assertThat(result).hasSize(1);
            verify(fillerStockRepository).findByPoolOperatorId(1L);
        }
    }
}
