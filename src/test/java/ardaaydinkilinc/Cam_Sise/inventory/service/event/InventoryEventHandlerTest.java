package ardaaydinkilinc.Cam_Sise.inventory.service.event;

import ardaaydinkilinc.Cam_Sise.core.domain.event.FillerRegistered;
import ardaaydinkilinc.Cam_Sise.inventory.domain.event.AssetCollected;
import ardaaydinkilinc.Cam_Sise.inventory.domain.event.AssetInflowRecorded;
import ardaaydinkilinc.Cam_Sise.inventory.domain.event.StockThresholdExceeded;
import ardaaydinkilinc.Cam_Sise.inventory.domain.vo.AssetType;
import ardaaydinkilinc.Cam_Sise.inventory.service.FillerStockService;
import ardaaydinkilinc.Cam_Sise.logistics.service.CollectionRequestService;
import ardaaydinkilinc.Cam_Sise.shared.domain.vo.GeoCoordinates;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("InventoryEventHandler Tests")
class InventoryEventHandlerTest {

    @Mock private FillerStockService fillerStockService;
    @Mock private CollectionRequestService collectionRequestService;

    @InjectMocks
    private InventoryEventHandler handler;

    @Test
    @DisplayName("FillerRegistered eventinde stok başlatmalı")
    void handlesFillerRegisteredSuccessfully() {
        var event = new FillerRegistered(1L, "Test Dolumcu",
                new GeoCoordinates(41.0, 29.0), LocalDateTime.now());

        handler.handleFillerRegistered(event);

        verify(fillerStockService).initializeStockForFiller(1L);
    }

    @Test
    @DisplayName("FillerRegistered eventinde stok başlatma hatası loglanmalı")
    void handlesFillerRegisteredWithError() {
        var event = new FillerRegistered(1L, "Test Dolumcu",
                new GeoCoordinates(41.0, 29.0), LocalDateTime.now());
        doThrow(new RuntimeException("DB error")).when(fillerStockService).initializeStockForFiller(anyLong());

        assertThatCode(() -> handler.handleFillerRegistered(event)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("StockThresholdExceeded eventinde otomatik toplama talebi oluşturmalı")
    void handlesStockThresholdExceededSuccessfully() {
        var event = new StockThresholdExceeded(1L, 2L, AssetType.PALLET, 10, 50, LocalDateTime.now());

        handler.handleStockThresholdExceeded(event);

        verify(collectionRequestService).createAutomatic(2L, AssetType.PALLET, 10);
    }

    @Test
    @DisplayName("StockThresholdExceeded eventinde talep oluşturma hatası loglanmalı")
    void handlesStockThresholdExceededWithError() {
        var event = new StockThresholdExceeded(1L, 2L, AssetType.PALLET, 10, 50, LocalDateTime.now());
        doThrow(new RuntimeException("error")).when(collectionRequestService).createAutomatic(anyLong(), any(), anyInt());

        assertThatCode(() -> handler.handleStockThresholdExceeded(event)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("AssetInflowRecorded: fillerStockService çağrılmamalı (sadece loglama)")
    void handlesAssetInflowRecorded() {
        var event = new AssetInflowRecorded(1L, 2L, AssetType.PALLET, 100, 600, "REF-001", LocalDateTime.now());
        handler.handleAssetInflowRecorded(event);
        verifyNoMoreInteractions(fillerStockService, collectionRequestService);
    }

    @Test
    @DisplayName("AssetCollected: fillerStockService çağrılmamalı (sadece loglama)")
    void handlesAssetCollected() {
        var event = new AssetCollected(1L, 2L, AssetType.PALLET, 50, 550, "PLAN-001", LocalDateTime.now());
        handler.handleAssetCollected(event);
        verifyNoMoreInteractions(fillerStockService, collectionRequestService);
    }
}
