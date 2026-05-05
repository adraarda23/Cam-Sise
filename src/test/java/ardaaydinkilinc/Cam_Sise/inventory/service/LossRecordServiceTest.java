package ardaaydinkilinc.Cam_Sise.inventory.service;

import ardaaydinkilinc.Cam_Sise.inventory.domain.LossRecord;
import ardaaydinkilinc.Cam_Sise.inventory.domain.vo.AssetType;
import ardaaydinkilinc.Cam_Sise.inventory.domain.vo.LossRate;
import ardaaydinkilinc.Cam_Sise.inventory.domain.vo.Period;
import ardaaydinkilinc.Cam_Sise.inventory.repository.LossRecordRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("LossRecordService Tests")
class LossRecordServiceTest {

    @Mock
    private LossRecordRepository lossRecordRepository;

    @InjectMocks
    private LossRecordService service;

    private static final Long FILLER_ID = 1L;
    private static final AssetType ASSET_TYPE = AssetType.PALLET;
    private static final Period PERIOD = new Period(
            LocalDate.of(2026, 1, 1),
            LocalDate.of(2026, 3, 31)
    );

    private LossRecord existingRecord;

    @BeforeEach
    void setUp() {
        existingRecord = LossRecord.createWithEstimate(FILLER_ID, ASSET_TYPE, new LossRate(5.0), PERIOD);
        existingRecord.clearDomainEvents();
    }

    @Nested
    @DisplayName("createWithEstimate")
    class CreateWithEstimate {

        @Test
        @DisplayName("Kayıt yoksa yeni LossRecord oluşturulmalı ve kaydedilmeli")
        void shouldCreateWhenNoExistingRecord() {
            when(lossRecordRepository.findByFillerIdAndAssetType(FILLER_ID, ASSET_TYPE))
                    .thenReturn(Optional.empty());
            when(lossRecordRepository.save(any(LossRecord.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            LossRecord result = service.createWithEstimate(FILLER_ID, ASSET_TYPE, 5.0, PERIOD);

            assertThat(result).isNotNull();
            assertThat(result.getFillerId()).isEqualTo(FILLER_ID);
            assertThat(result.getAssetType()).isEqualTo(ASSET_TYPE);
            verify(lossRecordRepository).save(any(LossRecord.class));
        }

        @Test
        @DisplayName("Kayıt zaten varsa IllegalArgumentException fırlatmalı")
        void shouldThrowWhenRecordAlreadyExists() {
            when(lossRecordRepository.findByFillerIdAndAssetType(FILLER_ID, ASSET_TYPE))
                    .thenReturn(Optional.of(existingRecord));

            assertThatThrownBy(() -> service.createWithEstimate(FILLER_ID, ASSET_TYPE, 5.0, PERIOD))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("already exists");

            verify(lossRecordRepository, never()).save(any());
        }

        @Test
        @DisplayName("Kaydedilen kayıtta doğru estimated rate bulunmalı")
        void shouldSaveRecordWithCorrectEstimatedRate() {
            when(lossRecordRepository.findByFillerIdAndAssetType(FILLER_ID, ASSET_TYPE))
                    .thenReturn(Optional.empty());
            when(lossRecordRepository.save(any(LossRecord.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            LossRecord result = service.createWithEstimate(FILLER_ID, ASSET_TYPE, 7.5, PERIOD);

            assertThat(result.getEstimatedRate().percentage()).isEqualTo(7.5);
        }
    }

    @Nested
    @DisplayName("updateActualRate")
    class UpdateActualRate {

        @Test
        @DisplayName("Actual rate güncellenmeli ve kaydedilmeli")
        void shouldUpdateActualRateAndSave() {
            when(lossRecordRepository.findByFillerIdAndAssetType(FILLER_ID, ASSET_TYPE))
                    .thenReturn(Optional.of(existingRecord));
            when(lossRecordRepository.save(any(LossRecord.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            LossRecord result = service.updateActualRate(FILLER_ID, ASSET_TYPE, 8.0);

            assertThat(result.getActualRate().percentage()).isEqualTo(8.0);
            verify(lossRecordRepository).save(existingRecord);
        }

        @Test
        @DisplayName("Kayıt bulunamazsa IllegalArgumentException fırlatmalı")
        void shouldThrowWhenRecordNotFound() {
            when(lossRecordRepository.findByFillerIdAndAssetType(FILLER_ID, ASSET_TYPE))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.updateActualRate(FILLER_ID, ASSET_TYPE, 8.0))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("not found");
        }

        @Test
        @DisplayName("Dönen kayıtta doğru actual rate olmalı")
        void shouldReturnRecordWithUpdatedActualRate() {
            when(lossRecordRepository.findByFillerIdAndAssetType(FILLER_ID, ASSET_TYPE))
                    .thenReturn(Optional.of(existingRecord));
            when(lossRecordRepository.save(any(LossRecord.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            LossRecord result = service.updateActualRate(FILLER_ID, ASSET_TYPE, 3.5);

            assertThat(result.getActualRate()).isEqualTo(new LossRate(3.5));
        }
    }

    @Nested
    @DisplayName("recalculateEstimatedRate")
    class RecalculateEstimatedRate {

        @Test
        @DisplayName("Estimated rate yeniden hesaplanmalı ve kaydedilmeli")
        void shouldRecalculateAndSave() {
            Period newPeriod = new Period(LocalDate.of(2026, 4, 1), LocalDate.of(2026, 6, 30));
            when(lossRecordRepository.findByFillerIdAndAssetType(FILLER_ID, ASSET_TYPE))
                    .thenReturn(Optional.of(existingRecord));
            when(lossRecordRepository.save(any(LossRecord.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            LossRecord result = service.recalculateEstimatedRate(FILLER_ID, ASSET_TYPE, 6.0, newPeriod);

            assertThat(result.getEstimatedRate().percentage()).isEqualTo(6.0);
            assertThat(result.getCalculationPeriod()).isEqualTo(newPeriod);
            verify(lossRecordRepository).save(existingRecord);
        }

        @Test
        @DisplayName("Kayıt bulunamazsa IllegalArgumentException fırlatmalı")
        void shouldThrowWhenRecordNotFound() {
            when(lossRecordRepository.findByFillerIdAndAssetType(FILLER_ID, ASSET_TYPE))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.recalculateEstimatedRate(FILLER_ID, ASSET_TYPE, 6.0, PERIOD))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("not found");
        }

        @Test
        @DisplayName("Dönen kayıtta güncel estimated rate ve period olmalı")
        void shouldReturnRecordWithUpdatedRateAndPeriod() {
            Period newPeriod = new Period(LocalDate.of(2026, 4, 1), LocalDate.of(2026, 6, 30));
            when(lossRecordRepository.findByFillerIdAndAssetType(FILLER_ID, ASSET_TYPE))
                    .thenReturn(Optional.of(existingRecord));
            when(lossRecordRepository.save(any(LossRecord.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            LossRecord result = service.recalculateEstimatedRate(FILLER_ID, ASSET_TYPE, 9.0, newPeriod);

            assertThat(result.getEstimatedRate()).isEqualTo(new LossRate(9.0));
            assertThat(result.getCalculationPeriod()).isEqualTo(newPeriod);
        }
    }

    @Nested
    @DisplayName("getLossRecord")
    class GetLossRecord {

        @Test
        @DisplayName("Kayıt bulunduğunda döndürmeli")
        void shouldReturnRecordWhenFound() {
            when(lossRecordRepository.findByFillerIdAndAssetType(FILLER_ID, ASSET_TYPE))
                    .thenReturn(Optional.of(existingRecord));

            LossRecord result = service.getLossRecord(FILLER_ID, ASSET_TYPE);

            assertThat(result).isEqualTo(existingRecord);
        }

        @Test
        @DisplayName("Kayıt bulunamazsa exception fırlatmalı")
        void shouldThrowWhenNotFound() {
            when(lossRecordRepository.findByFillerIdAndAssetType(FILLER_ID, ASSET_TYPE))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getLossRecord(FILLER_ID, ASSET_TYPE))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("not found");
        }
    }

    @Nested
    @DisplayName("getLossRecordsByFiller")
    class GetLossRecordsByFiller {

        @Test
        @DisplayName("Filler'a ait tüm kayıtları döndürmeli")
        void shouldReturnRecordsForFiller() {
            LossRecord separatorRecord = LossRecord.createWithEstimate(
                    FILLER_ID, AssetType.SEPARATOR, new LossRate(2.0), PERIOD);
            when(lossRecordRepository.findByFillerId(FILLER_ID))
                    .thenReturn(List.of(existingRecord, separatorRecord));

            List<LossRecord> result = service.getLossRecordsByFiller(FILLER_ID);

            assertThat(result).hasSize(2);
        }

        @Test
        @DisplayName("Kayıt yoksa boş liste döndürmeli")
        void shouldReturnEmptyListWhenNoRecords() {
            when(lossRecordRepository.findByFillerId(FILLER_ID))
                    .thenReturn(List.of());

            List<LossRecord> result = service.getLossRecordsByFiller(FILLER_ID);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("getLossRecordsByAssetType")
    class GetLossRecordsByAssetType {

        @Test
        @DisplayName("AssetType'a göre kayıtları döndürmeli")
        void shouldReturnRecordsByAssetType() {
            when(lossRecordRepository.findByAssetType(ASSET_TYPE))
                    .thenReturn(List.of(existingRecord));

            List<LossRecord> result = service.getLossRecordsByAssetType(ASSET_TYPE);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getAssetType()).isEqualTo(ASSET_TYPE);
        }

        @Test
        @DisplayName("Kayıt yoksa boş liste döndürmeli")
        void shouldReturnEmptyListWhenNone() {
            when(lossRecordRepository.findByAssetType(ASSET_TYPE))
                    .thenReturn(List.of());

            List<LossRecord> result = service.getLossRecordsByAssetType(ASSET_TYPE);

            assertThat(result).isEmpty();
        }
    }
}
